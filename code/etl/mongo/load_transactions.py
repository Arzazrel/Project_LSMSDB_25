"""
Author: Alessandro Diana
Project: MyFuture – LSMSDB 2025

Description:
    ETL script responsible for ingesting and simulating trading transactions.

    This step:
    - Reads transaction-like records from trades.csv
    - Generates realistic purchase, deposit and sell transactions
    - Uses historical prices for financial realism
    - Updates user balances and wallets consistently
    - Ensures transactional coherence without MongoDB transactions

Design notes:
    - Transactions are documents of record
    - Users are updated as a consequence of transactions
    - No MongoDB _id is used for logical relations
"""

import csv
from datetime import datetime, timedelta
from code.utils.mongoDB_conn import get_db
from code.utils import counter_service
from code.utils.asset_price_service import get_random_asset_price

MAX_RECENT_TX = 10          # max number of transactions in "recentTransactions"
DEC_ROUND = 4               # number of decimal places of approximation

# ------------------------------------ start: load method ------------------------------------

# insert a transaction (passed as parameter) in the db (passed as paramter)
def insert_transaction(db, tx):
    db.transactions.insert_one(tx)      # insert into DB

# applies the changes to the user due to the application of the transaction
def apply_purchase(db, user, tx):
    # --- Cash update ---
    user["cash"] -= tx["totalPrice"]            # withdrawn money from the account

    # --- Wallet update ---
    wallet_key = f"{tx['assetType']}Wallet"     # take the reference to the right wallet
    wallet = user[wallet_key]                   # take the correct wallet
    symbol = tx["symbol"]                       # take the symbol
    qty = tx["quantity"]                        # take the quantity
    price = tx["pricePerUnit"]                  # take the price for units

    asset_found = False                         # set default value

    # scroll all the assets in the wallet
    for asset in wallet:
        if asset["symbol"] == symbol:           # the same asset of the transaction is already in the portfolio
            
            # recalculate BEP (weighted average)
            old_qty = asset["quantity"]         
            old_bep = asset["bep"]              

            new_qty = old_qty + qty                                     # the quantity of the asset after the transaction
            new_bep = ((old_qty * old_bep) + (qty * price)) / new_qty   # calculate the new BEp for the asset (weighted average)

            # update the value for the asset
            asset["quantity"] = new_qty
            asset["bep"] = round(new_bep, DEC_ROUND)

            asset_found = True                  # update the value, asset found among those already owned
            break                               # exit from the for (scroll of the assets)
            
    # If asset not present in wallet, insert new entry
    if not asset_found:
        wallet.append({
            "symbol": tx["symbol"],
            "quantity": tx["quantity"],
            "blockedQuantity": 0,
            "bep": tx["pricePerUnit"]
        })
    
    # --- recent transactions update ---
    # update this transaction as last
    user["recentTransactions"].append({
        "transaction_id": tx["transaction_id"],
        "type": tx["type"],
        "symbol": symbol,
        "quantity": qty,
        "totalPrice": tx["totalPrice"],
        "status": tx["status"],
        "timestamp": tx["timestamp"]
    })

    # enforce max size (FIFO)
    if len(user["recentTransactions"]) > MAX_RECENT_TX:         # if the queue of recent transactions is larger than it should be (it was already full before the new transaction was entered)
        user["recentTransactions"].pop(0)                       # removes the oldest transaction

    user["updated_at"] = datetime.utcnow()                      # update time
    db.users.replace_one({"user_id": user["user_id"]}, user)    # update the user into DB
    
# read the data from csv file and load them into MongoDB
def ingest_transactions(csv_path):
    db = get_db()
    if db is None:
        print("[FATAL] MongoDB unavailable")
        return

    with open(csv_path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)

        for row in reader:
            user_id = int(row["accountAgeDays"])
            quantity = int(row["numItems"])
            payment_method = row["paymentMethod"]

            user = db.users.find_one({"user_id": user_id})
            if not user:
                continue

            snapshot = get_random_asset_price()
            if snapshot is None:
                continue

            timestamp = datetime.utcnow()

            # --- DEPOSIT if store credit ---
            if payment_method == "storecredit":
                deposit_amount = snapshot["price"] * quantity
                deposit_tx_id = counter_service.get_next_sequence("transaction_id")

                deposit_tx = {
                    "transaction_id": deposit_tx_id,
                    "userID": user_id,
                    "type": "deposit",
                    "timestamp": timestamp - timedelta(minutes=5),
                    "currency": "USD",
                    "quantity": None,
                    "status": "EXECUTED",
                    "assetType": None,
                    "pricePerUnit": None,
                    "totalPrice": deposit_amount
                }

                insert_transaction(db, deposit_tx)
                user["cash"] += deposit_amount

            # --- PURCHASE ---
            tx_id = counter_service.get_next_sequence("transaction_id")

            purchase_tx = {
                "transaction_id": tx_id,
                "userID": user_id,
                "symbol": snapshot["symbol"],
                "type": "purchase",
                "timestamp": timestamp,
                "currency": "USD",
                "quantity": quantity,
                "status": "EXECUTED",
                "assetType": snapshot["assetType"],
                "pricePerUnit": snapshot["price"],
                "totalPrice": snapshot["price"] * quantity
            }

            insert_transaction(db, purchase_tx)
            apply_purchase(db, user, purchase_tx)

    print("[STEP 4] Transactions ingestion completed")
    
# ------------------------------------ end: load method ------------------------------------
