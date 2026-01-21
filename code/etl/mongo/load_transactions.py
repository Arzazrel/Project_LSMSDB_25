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
import random
from datetime import datetime, timedelta
from code.utils.mongoDB_conn import get_db
from code.utils import counter_service
from code.utils import asset_price_service

# ---- input path ----
CSV_PATH = "dataset/user_transaction/trades.csv"

# ---- global var ----
MAX_RECENT_TX = 10          # max number of transactions in "recentTransactions"
DEC_ROUND = 4               # number of decimal places of approximation
SELL_PROBABILITY = 0.10     # 10%, probability to generate a sell transaction (after a buy transaction)
step_print_UI = 100

# ------------------------------------ start: insert method ------------------------------------

# insert a transaction (passed as parameter) in the db (passed as paramter)
def insert_transaction(db, tx):
    try:
        db.transactions.insert_one(tx)          # insert into DB
    except Exception as e:
        print("ERROR inserting transaction:", tx["transaction_id"], e)
    
# update the user (document) into MongoDB
def update_user(db, user):
    user["updated_at"] = datetime.utcnow()                      # update time
    db.users.replace_one({"user_id": user["user_id"]}, user)    # update the user into DB
    
# ------------------------------------ end: insert method ------------------------------------

# ------------------------------------ start: user utility method ------------------------------------
    
# check if the user has the asset in the correct quantity that allow to perform the sell
def check_asset_and_quantity_for_sell(user, tx):
    # --- Wallet update ---
    wallet_key = f"{tx['assetType']}Wallet"     # take the reference to the right wallet
    wallet = user[wallet_key]                   # take the correct wallet

    # scroll all the assets in the wallet
    for asset in wallet:
        if asset["symbol"] == tx["symbol"]:     # the same asset of the transaction is already in the portfolio
            if asset["quantity"] < tx["quantity"]:
                return False
            else:
                return True
          
    return False
    
"""
Maintains the recentTransactions list ordered by date (most recent first).
- recent[0] → most recent
- recent[-1] → most old

The transaction is inserted in the correct position based on its date.
If the list exceeds the maximum allowed size, the oldest entry is removed.
"""
def insert_recent_transaction_ordered(user, tx_summary):

    recent = user.setdefault("recentTransactions", [])  # get the most recent transactions      
    tx_time = tx_summary["date"]                        # get the data of the current transaction

    # empty list -> just insert
    if not recent:
        recent.append(tx_summary)                       # simply append
        return

    # the are transaction in 'recentTransactions' -> check the date to find the correct position for the current transaction
    not_inserted = True
    
    for i in range(len(recent) - 1, -1, -1):    # Traverse from the oldest transaction backwards
        if tx_time <= recent[i]["date"]:        # time check
            recent.insert(i + 1, tx_summary)    # Insert after this position
            not_inserted = False
            break
    
    if not_inserted:                            # current transaction is more recent than all others
        recent.insert(0, tx_summary)            # insert in head

    # enforce max size (remove oldest)
    if len(recent) > MAX_RECENT_TX:
        recent.pop(-1)                          # remove the last transaction

# ------------------------------------ end: user utility method ------------------------------------

# ------------------------------------ start: apply method ------------------------------------

# applies the changes to the user due to the application of the transaction
def apply_deposit(db, user, dp):
    
    user["cash"] += dp["totalPrice"]                            # update cash amount
    
    # create the summary of the deposit to insert in the recentTransactions
    deposit_summary = {
        "transaction_id": dp["transaction_id"],
        "type": dp["type"],
        "totalPrice": dp["totalPrice"],
        "status": dp["status"],
        "date": dp["date"]
    }
    
    insert_recent_transaction_ordered(user, deposit_summary)    # recent transactions update (if needed)    
    update_user(db, user)                                       # update user document in MongoDB   

# applies the changes to the user due to the application of the transaction
def apply_purchase(db, user, tx):
    # --- Cash update ---
    if tx["paymentMethod"] == "storecredit":    # withdrawn money from the account (only if payment_type is storecredit
        user["cash"] -= tx["totalPrice"]            

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
    if asset_found == False:
        wallet.append({
            "symbol": tx["symbol"],
            "quantity": tx["quantity"],
            "blockedQuantity": 0,
            "bep": tx["pricePerUnit"]
        })
    
    # --- recent transactions update ---
    # create the summary of the purchase to insert in the recentTransactions
    purchase_summary = {
        "transaction_id": tx["transaction_id"],
        "type": tx["type"],
        "symbol": symbol,
        "quantity": qty,
        "totalPrice": tx["totalPrice"],
        "status": tx["status"],
        "date": tx["date"]
    }
    
    insert_recent_transaction_ordered(user, purchase_summary)   # recent transactions update (if needed)  
    update_user(db, user)                                       # update user document in MongoDB           
    
"""
Applies the effects of a sell transaction:
- increases user cash
- decreases asset quantity in wallet
- updates recentTransactions
"""
def apply_sell(db, user, tx):
    
    # --- Cash update ---
    user["cash"] += tx["totalPrice"]            # increment cash

    # --- Wallet update ---
    wallet_key = f"{tx['assetType']}Wallet"     # take the reference to the right wallet
    wallet = user[wallet_key]                   # take the correct wallet

    # scroll all the assets in the wallet
    for asset in wallet:
        if asset["symbol"] == tx["symbol"]:     # the same asset of the transaction is already in the portfolio
            if asset["quantity"] < tx["quantity"]:
                asset["quantity"] = 0
                
            asset["quantity"] -= tx["quantity"] # decrease quantity of asset
            if asset["quantity"] <= 0:
                wallet.remove(asset)            # quantity equal to 0, remove the asset from the wallet
            break

    # --- recent transactions ---
    insert_recent_transaction_ordered(user, {
        "transaction_id": tx["transaction_id"],
        "type": tx["type"],
        "symbol": tx["symbol"],
        "quantity": tx["quantity"],
        "totalPrice": tx["totalPrice"],
        "status": tx["status"],
        "date": tx["date"]
    })

    update_user(db, user)
   
# ------------------------------------ end: apply method ------------------------------------
   
"""
Generates a sell transaction derived from a purchase transaction.
Returns None if no valid sell date can be generated.
"""   
def generate_sell_transaction(db, user, purchase_tx):
    
    symbol = purchase_tx["symbol"]                              # take the symbol of the asset (sell the same asset of the purchase)
    qty = purchase_tx["quantity"]                               # take the quantity (sell the same quantity of the purchase)
    purchase_time = purchase_tx["date"]                         # take the data of the purchase

    most_recent_date = asset_price_service.get_most_recent_price_date(symbol)   # get the most recent date for the asset
    if most_recent_date is None or most_recent_date <= purchase_time:           # control check whether the purchase took place on the last day available for the asset
        return None

    sell_date = asset_price_service.pick_random_price_date(symbol, purchase_time)   # get the date for the purchase transaction (with hour 00:00)
    if sell_date is None:
        return None                                             # data incorrect 
    
    # generate a realistic trade datetime (during stock exchange opening hours) on date
    sell_time = datetime.combine(
        sell_date.date(),
        asset_price_service.random_time_between_market_hours()
    )
    
    price_doc = db.asset_prices.find_one(                       # get the document relating to the prices for the chosen asset and date
        {"symbol": symbol, "date": sell_date}                   # it's important use the date with hour 00:00 for a correct matching
    )
    
    if not price_doc:                                           # control check
        return None

    price, source = asset_price_service.pick_price_from_candle(price_doc, sell_time)    # get the price per unit for the transaction

    sell_tx_id = counter_service.get_next_sequence("transaction_id")    # get tid for the transaction

    return {
        "transaction_id": sell_tx_id,
        "user_id": user["user_id"],
        "symbol": symbol,
        "type": "sell",
        "date": sell_time,
        "currency": "USD",
        "totalPrice": price * qty,
        "paymentMethod": purchase_tx["paymentMethod"],
        "status": "EXECUTED",
        "assetType": purchase_tx["assetType"],
        "pricePerUnit": price,
        "quantity": qty,
        "updated_at": sell_time
    }
   
   
# ------------------------------------ start: load method ------------------------------------
   
# read the data from csv file and load them into MongoDB
def ingest_transactions():
    
    print("Transactions ingestion started.\nDB connection...")  # UI print
    db = get_db()                                               # MongoDB connection
    if db is None:
        print("ERROR: MongoDB unavailable")
        return
        
    print("DB connected succesfully.\nReading csv file...")     # UI print
    transaction_count = 0                                       # set counter
    null_transaction_count = 0                                  # counter for the failed transaction
    sell_count = 0                                              # set sell counter
    user_id_sell_tx = set()                                     # holds the user_ids for users for whom a sale transaction will be generated                                      
    
    with open(CSV_PATH, newline="", encoding="utf-8") as f:     # open csv file
        reader = csv.DictReader(f)                              # read csv file and put in dictionary
        
        for row in reader:                                      # scroll all row (transactions) of the file
            
            if transaction_count % 100 == 0:                                
                print(f"Creating transaction number: {transaction_count}")  # UI print to see progression
            
            user_id = int(row["accountAgeDays"])                # the user_id of the logged-in user (who made) the transaction will be equal to the "accountAgeDays"
            quantity = int(row["numItems"])                     # get the quantity of the asset purchased in the transaction
            payment_method = row["paymentMethod"]               # can be : 'storecredit', 'paypal', 'creditcard'

            user = db.users.find_one({"user_id": user_id})      # get the related user
            if not user:                                        # control check
                print("ERROR in ingest_transactions [load_transactions] - User with user_id: ",user_id, " isn't in the DB.")
                continue

            snapshot = asset_price_service.get_random_asset_price() # get a random asset, a realistic date and time and the corresponding price - SEE NOTE 0
            if snapshot is None:                                # control check
                print("ERROR in ingest_transactions [load_transactions] - failed selection of symbol, assetType, date,price")
                null_transaction_count += 1                     # update failed transation counter
                continue

            # -- create a deposit if the payment method is "storecredit" -- 
            if payment_method == "storecredit":
                
                deposit_amount = snapshot["price"] * quantity                       # price per unit * quantity of assets
                deposit_tx_id = counter_service.get_next_sequence("transaction_id") # get transaction_id
                # random select a payment method: 'paypal' or 'creditcard'
                if random.randint(0, 1) == 0:
                    deposit_pay_method = "paypal"
                else:
                    deposit_pay_method = "creditcard"

                # create deposit transaction
                deposit_tx = {
                    "transaction_id": deposit_tx_id,
                    "user_id": user_id,
                    "type": "deposit",                                              # set the type of the transaction
                    "date": snapshot["date"] - timedelta(minutes=5),                # set the date and time for the deposit transaction
                    "currency": "USD",
                    "status": "EXECUTED",
                    "totalPrice": deposit_amount,
                    "paymentMethod": deposit_pay_method,
                    "updated_at": snapshot["date"] - timedelta(minutes=5)
                }

                insert_transaction(db, deposit_tx)              # insert the transaction into MongoDB
                apply_deposit(db, user, deposit_tx)             # update the fields for the cash and recent transaction of the user 
                
            # --- PURCHASE ---
            tx_id = counter_service.get_next_sequence("transaction_id")             # get transaction_id

            # create purchase transaction
            purchase_tx = {
                "transaction_id": tx_id,
                "user_id": user_id,
                "symbol": snapshot["symbol"],
                "type": "purchase",
                "date": snapshot["date"],
                "currency": "USD",
                "totalPrice": snapshot["price"] * quantity,
                "paymentMethod": payment_method,
                "status": "EXECUTED",
                "assetType": snapshot["assetType"],
                "pricePerUnit": snapshot["price"],
                "quantity": quantity,
                "updated_at": snapshot["date"]
            }

            insert_transaction(db, purchase_tx)                 # insert the transaction into MongoDB
            apply_purchase(db, user, purchase_tx)               # applies the changes to the user
            
            transaction_count += 1                              # update counter
            
            if random.random() < SELL_PROBABILITY:              # generation of sell transaction -- SEE NOTE 1
                if check_asset_and_quantity_for_sell(user, purchase_tx):        # check if the user has the asset with correct quantity (return true or false)
                    
                    sell_tx = generate_sell_transaction(db, user, purchase_tx)  # generate the sell transaction
                    
                    if sell_tx:                                     # sell transaction generated in correct way
                        insert_transaction(db, sell_tx)             # insert the sell transaction
                        apply_sell(db, user, sell_tx)               # apply the change 
                        sell_count += 1                             # update the sell count
                        user_id_sell_tx.add(user_id)                # add the id into set
                    else:
                        print("ERROR [load_transactions] - error in generate a sell transaction")
            
    print(f"Transactions ingestion completed.\nNumber of transaction(purchase) injested: {transaction_count}.") # UI print
    print(f"Number of failed transaction(purchase): {null_transaction_count}.")                                 # UI print
    print(f"Number of sell transaction(generated) injested: {sell_count}.")                                     # UI print
    print("The users (user_id) for whom a sales transaction has been generated are:",list(user_id_sell_tx))     # UI print
# ------------------------------------ end: load method ------------------------------------

if __name__ == "__main__":
    ingest_transactions()   

"""
NOTE 0:
    The called function:
    - selects a random asset (optionally filtered by type)
    - selects a valid historical date for that asset
    - generates a realistic transaction time within market hours
    - extracts a coherent price from historical OHLC data
    
    Input:
    - asset_type (optional): asset category to filter ("share", "ETF", "crypto")

    Output, a dict with:
    - symbol: asset identifier
    - assetType: type of asset
    - date: transaction datetime (date + realistic market time)
    - price: selected price per unit
NOTE 1:
    Sell transactions are generated as a probabilistic consequence of purchase operations to simulate realistic user trading behavior.

    After each purchase, a fixed probability is evaluated to determine whether the acquired asset will be sold. If selected, a sell 
    transaction is generated using the same asset and quantity of the originating purchase.

    The sell transaction date is constrained to be strictly after the purchase date and not later than the most recent date 
    for which historical price data is available for that asset. If no valid time window exists, the sell transaction is skipped.

    This approach guarantees temporal consistency, avoids forward-looking bias, and ensures that all generated prices correspond to 
    real historical market data.
"""