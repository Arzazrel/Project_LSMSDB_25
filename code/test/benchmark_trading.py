"""
Author: Alessandro Diana
Project: MyFuture – LSMSDB 2025
Description: Performance benchmark to validate MongoDB modeling choices.
Tests execution time and document size for:
1. Transaction history (Linking vs Separate Collection)
2. Asset Prices history (Linking vs Embedded vs Separate Collection)
"""

import time
import bson
import random
from datetime import datetime, timedelta
from code.utils.mongoDB_conn import get_db

# --- CONFIGURATION ---
TEST_USER_IDS = [8000, 8001, 8002]  # Passive, Swing, Day Trader
TEST_SYMBOL = "TEST_BENCH"
ASSETS_TO_TEST = ["KO", "MSFT"]     # Coca-Cola and Microsoft
# --- GLOBAL VAR FOR ASSETS BACKUP ---
ASSET_BACKUP = []
DEFAULT_ITERATIONS = 100

# transaction distribution over 5 years, use an average of the value reported on the documentation -> SEE NOTE 0
USER_PROFILES = {
    8000: {"name": "Passive Investor", "monthly_tx": 3},    # ~180 total
    8001: {"name": "Swing Trader", "monthly_tx": 20},       # ~1200 total
    8002: {"name": "Day Trader", "monthly_tx": 350}         # ~21000 total
}

START_TRANSACTION_ID = 100000       # start of the transaction_id used for the test
num_underscore_ui = 80

# ------------------------------------ start: utils methods ------------------------------------

# print the overall size of the database before you begin.
def print_collection_stats(db):
    print("-" * num_underscore_ui)
    print("--- DATABASE GLOBAL STATS ---")
    cols = ["users", "transactions", "assets", "asset_prices"]
    for col_name in cols:
        stats = db.command("collstats", col_name)
        count = stats.get('count', 0)
        size_mb = stats.get('size', 0) / (1024 * 1024)
        print(f"Collection: {col_name:<15} | Docs: {count:<10} | Size: {size_mb:>7.2f} MB")
    print("-" * num_underscore_ui)

def get_time_ranges():
    now = datetime.utcnow()                 # get current date
    ref_date = now - timedelta(days=365)    # adjusting "now" to 1 year ago as requested if recent data is sparse
    
    # calculate and return the time window(period) for the tests
    return [
        ("1 Week", ref_date - timedelta(weeks=1), ref_date),
        ("1 Month", ref_date - timedelta(days=30), ref_date),
        ("3 Months", ref_date - timedelta(days=90), ref_date),
        ("3 to 6 Months", ref_date - timedelta(days=180), ref_date - timedelta(days=90)),
        ("6 to 12 Months", ref_date - timedelta(days=365), ref_date - timedelta(days=180)),
        ("1 Year", ref_date - timedelta(days=365), ref_date),
        ("1 to 2 Years", ref_date - timedelta(days=730), ref_date - timedelta(days=365)),
        ("5 Years", ref_date - timedelta(days=1825), ref_date)
    ]
  
# Download and save the original state of the test assets to memory.
def backup_assets(db):
    print(f"Backing up assets: {ASSETS_TO_TEST}...")
    global ASSET_BACKUP
    ASSET_BACKUP = list(db.assets.find({"symbol": {"$in": ASSETS_TO_TEST}}))
    if not ASSET_BACKUP:
        print("WARNING: No assets found to backup!")

# method for cleaning the database of data created for testing purposes
def cleanup(db):
    print("Cleaning up test data...")                           # UI print
    db.users.delete_many({"user_id": {"$in": TEST_USER_IDS}})   # clean users
    db.transactions.delete_many({"symbol": TEST_SYMBOL})        # clean transactions
    
    # restoration of original asset data 
    if ASSET_BACKUP:
        for original_doc in ASSET_BACKUP:
            db.assets.replace_one({"_id": original_doc["_id"]}, original_doc)   # restore
        print(f"Assets {ASSETS_TO_TEST} restored to original state.")

    print("Cleanup complete.")                                  # UI print
    
# method to create the user with different pattern and visualize the size
def user_size_print(inserted_ids, recent_tx_list, refs_list, all_tx_docs):
    
    # calculate the dimension for the user's version
    user_doc_linking = {
        "user_id": "999999",
        "firstName": f"Test_999999",
        "lastName": "Benchmark",
        "email": "test@email.com",
        "password_hash": "password",
        "role": "user",
        "birthDate": "01/01/0001",
        "phone": "3333333333",
        "address": "first avenue",
        "city": "smalville",
        "province": "large ville",
        "cap": "11111",
        "cash": 10000.0,
        "blockedCash": 0.0,
        "currency": "USD",
        "shareWallet": [], "etfWallet": [], "cryptoWallet": [],
        
        # pattern 1: Document Linking (List of IDs)
        "transaction_ids": inserted_ids,
        
        "registrationDate": "01/01/0001",
        "createdAt": datetime.utcnow(),
        "updatedAt": datetime.utcnow(),
        "suspended": False,
        "deleted": False
    }
        
    user_doc_embedded = {
        "user_id": "999999",
        "firstName": f"Test_999999",
        "lastName": "Benchmark",
        "email": "test@email.com",
        "password_hash": "password",
        "role": "user",
        "birthDate": "01/01/0001",
        "phone": "3333333333",
        "address": "first avenue",
        "city": "smalville",
        "province": "large ville",
        "cap": "11111",
        "cash": 10000.0,
        "blockedCash": 0.0,
        "currency": "USD",
        "shareWallet": [], "etfWallet": [], "cryptoWallet": [],
        
        # pattern 2: large embedded Array (Full objects - in documentation talk in semplified embedde only the date)
        "transactions_embedded": all_tx_docs,
        
        "registrationDate": "01/01/0001",
        "createdAt": datetime.utcnow(),
        "updatedAt": datetime.utcnow(),
        "suspended": False,
        "deleted": False
    }
    
    user_partial_embedded = {
        "user_id": "999999",
        "firstName": f"Test_999999",
        "lastName": "Benchmark",
        "email": "test@email.com",
        "password_hash": "password",
        "role": "user",
        "birthDate": "01/01/0001",
        "phone": "3333333333",
        "address": "first avenue",
        "city": "smalville",
        "province": "large ville",
        "cap": "11111",
        "cash": 10000.0,
        "blockedCash": 0.0,
        "currency": "USD",
        "shareWallet": [], "etfWallet": [], "cryptoWallet": [],
        
        # pattern 3: partial embedded (RecentTransaction)
        "recent_transactions": recent_tx_list,
        
        "registrationDate": "01/01/0001",
        "createdAt": datetime.utcnow(),
        "updatedAt": datetime.utcnow(),
        "suspended": False,
        "deleted": False
    }     

    user_ref_list = {
        "user_id": "999999",
        "firstName": f"Test_999999",
        "lastName": "Benchmark",
        "email": "test@email.com",
        "password_hash": "password",
        "role": "user",
        "birthDate": "01/01/0001",
        "phone": "3333333333",
        "address": "first avenue",
        "city": "smalville",
        "province": "large ville",
        "cap": "11111",
        "cash": 10000.0,
        "blockedCash": 0.0,
        "currency": "USD",
        "shareWallet": [], "etfWallet": [], "cryptoWallet": [],
        
        # pattern 4: simplified transaction (ID + Date)
        "ref_list": refs_list,
        
        "registrationDate": "01/01/0001",
        "createdAt": datetime.utcnow(),
        "updatedAt": datetime.utcnow(),
        "suspended": False,
        "deleted": False
    }    
    
    # calculate sizes
    size_link_kb = len(bson.BSON.encode(user_doc_linking)) / 1024
    size_part_emb_kb = len(bson.BSON.encode(user_partial_embedded)) / 1024
    size_emb_kb = len(bson.BSON.encode(user_doc_embedded)) / 1024
    size_ref_list_kb = len(bson.BSON.encode(user_ref_list)) / 1024
    
    # print sizes
    print(f"Size Analysis for User:")
    print(f"  -> Linking (IDs only):      {size_link_kb:>8.2f} KB")
    print(f"  -> Simplified (ID + Date):  {size_ref_list_kb:>8.2f} KB")
    print(f"  -> Recent (Java Mapping):   {size_part_emb_kb:>8.2f} KB")
    print(f"  -> Full :                   {size_emb_kb:>8.2f} KB")
                    
# ------------------------------------ end: utils methods ------------------------------------

# ------------------------------------ start: data generation methods ------------------------------------

def setup_test_data(db):
    print("Starting Data Generation for Benchmark (user with five years history)...")          # UI print
    print("-" * num_underscore_ui)     # UI print
    
    # clean previous test data
    db.users.delete_many({"user_id": {"$in": TEST_USER_IDS}})
    db.transactions.delete_many({"symbol": TEST_SYMBOL})
    
    now = datetime.utcnow()                                     # get current time
    
    # scan all user profile
    for uid, profile in USER_PROFILES.items():
        global START_TRANSACTION_ID
        print(f"Generating data for {profile['name']} (ID: {uid})...")  # UI print

        all_tx_docs = []            # list of the created transaction (full document)
        recent_tx_list = []         # list of the created transaction (partial document)
        refs_list = []              # list of the simplified transation (ID + Date)
        
        # generate transactions for 5 years = 12 * 5 = 60 month
        for month in range(60):
            date_month = now - timedelta(days=month*30)
            #
            for _ in range(profile['monthly_tx']):
                tx_date = date_month - timedelta(days=random.randint(0, 28))    # get a random day for transaction
                START_TRANSACTION_ID += 1           # update counter for transaction_id
                
                # create the transaction
                full_doc = {
                    "user_id": uid,
                    "transaction_id": START_TRANSACTION_ID,
                    "symbol": TEST_SYMBOL,
                    "transactionType": "purchase",
                    "date": tx_date,
                    "totalPrice": round(random.uniform(10, 1000), 2),
                    "quantity": random.randint(1, 10),
                    "status": "EXECUTED",
                    "paymentMethod": "storecredit",
                    "assetType": "share",
                    "pricePerUnit": random.randint(1, 500),
                    "updatedAt": tx_date
                }
                all_tx_docs.append(full_doc)
                
                # partial document (class mapping to RecentTransaction)
                recent_tx_list.append({
                    "transactionId": full_doc["transaction_id"],
                    "type": full_doc["transactionType"],
                    "symbol": full_doc["symbol"],
                    "quantity": full_doc["quantity"],
                    "totalPrice": full_doc["totalPrice"],
                    "status": full_doc["status"],
                    "date": full_doc["date"]  # mapping Instant date
                })
        
        # insert transactions
        res = db.transactions.insert_many(all_tx_docs)      # insert transaction into MongoDB
        inserted_ids = res.inserted_ids                     # get the IDs of the transaction
        
        # create simplified transaction (ID + Date)
        for ids in inserted_ids:
            refs_list.append({
                "id": ids,
                "d": tx_date
            })
        
        # create the User document with the three patterns
        user_doc = {
            "user_id": uid,
            "email": f"mail_{uid}",
            "firstName": f"Test_{uid}",
            "lastName": "Benchmark",
            "cash": 10000.0,
            "shareWallet": [], "etfWallet": [], "cryptoWallet": [],
            # pattern 1: document linking (List of IDs)
            "transaction_ids": inserted_ids,
            # pattern 2: large embedded Array (Full objects - in documentation talk in semplified embedde only the date)
            "transactions_embedded": all_tx_docs, 
            # pattern 3: partial embedded (RecentTransaction)
            "recent_transactions": recent_tx_list,       
            # pattern 4: simplified transaction (ID + Date)
            "ref_list": refs_list,   
            "registrationDate": now - timedelta(days=1825)
        }
        
        db.users.insert_one(user_doc)               # insert users in MongoDB
        
        user_size_print(inserted_ids, recent_tx_list, refs_list, all_tx_docs)   # create and visualize size of the user with the different pattern
        print("-" * num_underscore_ui)     # UI print
        
# ------------------------------------ end: data generation methods ------------------------------------

# ------------------------------------ start: performance tests -----------------------------------

#
def run_user_transaction_performance_test(db, iterations=DEFAULT_ITERATIONS):
    print(f"--- PERFORMANCE TEST (Mean of {iterations} runs) ---")  # UI print
    print("The improvements indicates the time saved using the separation collection method compared to the other method as a percentage.") # UI print
    
    ranges = get_time_ranges()          # get all the time ranges for the tests
    
    # scan all test user
    for uid in TEST_USER_IDS:
        profile = USER_PROFILES[uid]    # get profile of the user
        
        print(f"\n---- Testing {profile['name']} (ID: {uid}) -> monthly transactions: {profile['monthly_tx']}, total transaction in 5 years: {profile['monthly_tx']*60} ----")
        print(f"{'Range':<18} | {'Linking (ms)':<12} | {'Semplified (ms)':<12} | {'Partial (ms)':<12} | {'Sep (ms)':<12} || {'Imp.link'} | {'Imp.semp'} | {'Imp.part'}")
        
        user = db.users.find_one({"user_id": uid})              # get the user
        doc_linking_ids = user.get("transaction_ids", [])       # get the linking transactions 
        recent_array = user.get("recentTransactions", [])       # get the partial embedded transactions
        refs_array = user.get("ref_list", [])                   # get semplified embedded transactions
        doc_embedded = user.get("transactions_embedded", [])    # get embedded transactions

        # scan all range for the testing
        for label, start, end in ranges:
            # SEE NOTE 1
            # -- test linking (find in list and fetch from transactions) --
            t0 = time.time()
            for _ in range(iterations):
                user = db.users.find_one({"user_id": uid}, {"transaction_ids": 1, "_id": 0})
                # Simulated Linking approach: fetch IDs then query collection
                list(db.transactions.find({"_id": {"$in": doc_linking_ids}, "date": {"$gte": start, "$lte": end}}).sort("date", -1))
            t_link = ((time.time() - t0) / iterations) * 1000   # convert in ms
            
            # -- semplified embedded (filter local array + selective fetch)
            t0 = time.time()
            for _ in range(iterations):
                user = db.users.find_one({"user_id": uid}, {"ref_list": 1, "_id": 0})
                # filter in internal array by data
                matched_ids = [tx["id"] for tx in refs_array if start <= tx["d"] <= end]
                # retrieve the selected transaction 
                if matched_ids:
                    list(db.transactions.find({"transaction_id": {"$in": matched_ids}}).sort("date", -1))
            t_simpl = ((time.time() - t0) / iterations) * 1000  # convert in ms
            
            # -- test partial embedded (filter local array + selective fetch)
            t0 = time.time()
            for _ in range(iterations):
                user = db.users.find_one({"user_id": uid}, {"recent_transactions": 1, "_id": 0})
                # filter in internal array by data
                matched_ids = [tx["transactionId"] for tx in recent_array if start <= tx["date"] <= end]
                #if matched_ids:
                #    list(db.transactions.find({"transaction_id": {"$in": matched_ids}}).sort("date", -1))
            t_part = ((time.time() - t0) / iterations) * 1000

            # -- test separate collection (my choice, optimized query) --
            t0 = time.time()
            for _ in range(iterations):
                user = db.users.find_one({"email": "montanarifranco@example.org"}, {"user_id": 1, "_id": 0})
                list(db.transactions.find({"user_id": uid, "date": {"$gte": start, "$lte": end}}).sort("date", -1).hint("user_id_1_date_-1"))
            t_sep = ((time.time() - t0) / iterations) * 1000    # convert in ms
            
            # calculate - (t_other - t_sep) / t_other * 100 -> indi
            imp_vs_link = ((t_link - t_sep) / t_link * 100) if t_link > 0 else 0
            imp_vs_semp = ((t_simpl - t_sep) / t_simpl * 100) if t_simpl > 0 else 0
            imp_vs_part = ((t_part - t_sep) / t_part * 100) if t_part > 0 else 0
            
            print(f"{label:<18} | {t_link:>10.3f}ms | {t_simpl:>13.3f}ms | {t_part:>10.3f}ms | {t_sep:>10.3f}ms || {imp_vs_link:>7.2f}% | {imp_vs_semp:>6.2f}% | {imp_vs_part:>6.2f}%")
            
        print("-" * num_underscore_ui * 2)     # UI print

#
def run_asset_test(db, iterations=DEFAULT_ITERATIONS):
    print(f"--- ASSET PRICES BENCHMARK (Mean of {iterations} runs) ---")           # UI print
    
    ranges = get_time_ranges()          # get all the time ranges for the tests
    
    # scan all slecetd assets for testing
    for symbol in ASSETS_TO_TEST:
        asset = db.assets.find_one({"symbol": symbol})  # get asset doc
        # control check
        if not asset: 
            print(f"Asset {symbol} not found. Skipping...")
            continue                          
        
        # fetch prices for size comparison
        all_prices = list(db.asset_prices.find({"symbol": symbol}).sort("date", -1))    # get all asset_prices of the asset
        
        # extract data to create the pattern to test
        price_ids = [p["_id"] for p in all_prices]                          # extract id of asset_prices
        price_refs = [{"id": p["_id"], "d": p["date"]} for p in all_prices] # simplified embedded
        
        # INJECTION
        db.assets.update_one(
            {"symbol": symbol},
            {"$set": {
                "price_ids": price_ids,
                "price_refs": price_refs
            }}
        )
        
        # -- calculate sizes --
        raw_size = len(bson.BSON.encode(asset)) / 1024                                  # size of raw document
        size_link = len(bson.BSON.encode({**asset, "price_ids": price_ids})) / 1024     # linking
        size_simpl = len(bson.BSON.encode({**asset, "price_refs": price_refs})) / 1024  # semplified embedded
        
        # UI prints      
        print(f"\nAsset: {symbol} ({len(all_prices)} total price records)")
        print(f"  -> Original Size:          {raw_size:>8.2f} KB")
        print(f"  -> Linking Size (IDs):     {size_link:>8.2f} KB")
        print(f"  -> Simplified (ID+Date):   {size_simpl:>8.2f} KB")
        print(f"\n{'Range':<20} | {'Linking (ms)':<12} | {'Simpl. (ms)':<12} | {'Sep (ms)':<12} || {'Imp.link'} | {'Imp.semp'}")
        
        # scan all range for the testing
        for label, start, end in ranges:
            # test linking (Query by ID)
            t0 = time.time()
            for _ in range(iterations):
                a = db.assets.find_one({"symbol": symbol}, {"price_ids": 1, "_id": 0})  # get asset
                local_ids = a.get("price_ids", []) if a else []                         # get ids list
                if local_ids:
                    list(db.asset_prices.find({"_id": {"$in": local_ids}, "date": {"$gte": start, "$lte": end}}))
            t_link = ((time.time() - t0) / iterations) * 1000

            # test smplified embedded (filter local array + selective fetch)
            t0 = time.time()
            for _ in range(iterations):
                a = db.assets.find_one({"symbol": symbol}, {"price_refs": 1, "_id": 0}) # get asset
                local_refs = a.get("price_refs", []) if a else []                       # get semplified embedded (id and date)
                # filter in internal array by data
                matched_ids = [p["id"] for p in local_refs if start <= p["d"] <= end]
                # retrieve the selected transaction 
                if matched_ids:
                    list(db.asset_prices.find({"_id": {"$in": matched_ids}}).sort("date", -1))
            t_simpl = ((time.time() - t0) / iterations) * 1000

            # test separate collection (my choice, optimized query)
            t0 = time.time()
            for _ in range(iterations):
                list(db.asset_prices.find({"symbol": symbol, "date": {"$gte": start, "$lte": end}}))
            t_sep = ((time.time() - t0) / iterations) * 1000

            # calculate - (t_other - t_sep) / t_other * 100 -> indi
            imp_vs_link = ((t_link - t_sep) / t_link * 100) if t_link > 0 else 0
            imp_vs_semp = ((t_simpl - t_sep) / t_simpl * 100) if t_simpl > 0 else 0
            
            print(f"{label:<20} | {t_link:<12.3f} | {t_simpl:<12.3f} | {t_sep:<12.3f} || {imp_vs_link:>7.2f}% | {imp_vs_semp:>6.2f}%")
            
        print("-" * num_underscore_ui)
            
# ------------------------------------ end: performance tests -----------------------------------

if __name__ == "__main__":
    db = get_db()               # get connection to MongoDB
    if db is not None:
        try:
            print_collection_stats(db)                  # show the db sizes
            backup_assets(db)                           # back up assets
            setup_test_data(db)                         # setup data for the testing
            run_user_transaction_performance_test(db)   # run the testing for user nd transactions
            run_asset_test(db)                          # run the testing for asset and asset_prices
        finally:
            cleanup(db)                 # clean MongoDB from test data
    else:
        print("Could not connect to DB.")

"""
Why we are doing these tests:
1. Document Size (Bloating): MongoDB has a 16MB limit. For a Day Trader or a stock with 
   decades of history, embedding or linking thousands of IDs in the main document will 
   lead to performance degradation and eventually failure.
2. Query Efficiency: Separate collections with compound indexes {user_id: 1, date: -1} 
   allow the engine to scan only the required range, whereas large arrays force the 
   engine to load the whole array into RAM before filtering.
3. Memory Management: Large documents put pressure on the WiredTiger cache. 

NOTE 0 -> USER ASSUMPTIONS
1. The ‘Passive/Casual’ Investor (Approximately 70% of users)  
    Users who use the app as a piggy bank/accumulation plan/pension fund.  
    Average transactions: 1-3 transactions per month. 
    Behaviour: Often sets up an automatic ‘SIP’ (Systematic Investment Plan) or buys shares in 1-2 well-known companies when they have saved up some money. 
               For them, the ‘Transactions’ screen is almost useless; they only look at the total balance. 
2. The ‘Active/Swing Trader’ Investor (Approximately 25% of users)  
    This user seeks to beat the market and follows trends. 
    Average transactions: 10 - 30 transactions per month. 
    Behaviour: Buys and sells stocks within a few days or weeks. This is the user who generates ‘noble traffic’ on the app, consulting charts and news daily.  
               Here, the history of recent transactions begins to be important for monitoring the latest actions taken, and even the more extensive history 
               is consulted more frequently (mostly transactions from the last month). 
3. The ‘Day Trader’ or Speculator (Approximately 5% of users)  
    A very active user  who generates the most traffic,  often attracted by  highly volatile stocks. 
    Average transactions: 50 - 500 operations per month. 
    Behaviour: Can perform as many as 10-30 micro-transactions in a single day. Although these are few in number, they can generate more than 50% of the total order volume. 
               Having periods of intense activity in a short time, they will need to load more transactions when consulting them; one page will not be enough for all
               their most recent movements. They will often consult graphs for various assets, both short and long term. 

NOTE 1 -> retrieve also the user
    The embedding approach introduces significant I/O and memory overhead. While filtering a local array appears fast in isolated benchmarks, the requirement to fetch the 
    entire 'User' document—which can exceed 5MB for active traders—saturates network bandwidth and the database cache. Furthermore, it forces the application to maintain 
    bloated data structures in RAM, leading to poor scalability under high concurrent user loads. In contrast, the 'Flat' (Separate) collection approach enables granular 
    access to only the required data, ensuring system resource preservation and predictable performance.
    
    The "Heavy Fetch" Problem
        The benchmark reveals that as the User document grows (reaching 5.5MB for the Day Trader), the overhead of fetching even a projected field becomes a bottleneck. 
        The 97%+ improvement seen in shorter time ranges for the Separate Collection method proves that avoiding the User document entirely is the only way to maintain 
        sub-millisecond latency.

    The 5-Year Outlier
        In the 5-year range, we notice the Separate Collection performance aligns closer to the Embedded methods. This is expected: when a query retrieves nearly the 
        entire history of a Day Trader, the volume of data transferred (I/O) becomes the primary factor, regardless of the storage strategy. However, the Separate Collection 
        remains superior as it prevents 'Cache Thrashing'—keeping the main User document small and the database responsive for other operations.

How to run:
Ensure you are in the project root folder and execute:
python -m code.test.benchmark_trading
"""