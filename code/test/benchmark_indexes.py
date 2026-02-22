"""
Author: Alessandro Diana
Description:
    Benchmark script to evaluate MongoDB index performance.

    For each tested index, the script measures:
    - Query execution time WITHOUT index
    - Query execution time WITH index
    - Insert execution time WITHOUT index
    - Insert execution time WITH index

    Each test is repeated multiple times in order to compute:
    - minimum execution time
    - maximum execution time
    - average execution time

    The goal is to quantify:
    - the performance gain provided by the index during read operations
    - the write-time penalty introduced by index maintenance
"""

import time
import argparse
import statistics
import numpy as np
from datetime import datetime, timedelta
from pymongo.errors import PyMongoError

from code.utils.mongoDB_conn import get_db
from code.db.create_indexes import create_indexes

BENCHMARK_OUTPUT_FILE = "mongodb_indexes_benchmark.txt" # name of the output file
OUTPUT_FILE_HANDLE = None                               # ref to output file
BENCHMARK_START_TIME = None
output_header_mex = "MongoDB Index Benchmark Report"    #
line_length = 120                                       # length for the header and footer separation lines
documents = []                                          # contains the documents that must be inserted with insertmany

# ------------------------------------ start: utilities methods ------------------------------------

"""
    Measures execution time of a function over multiple iterations.

    Parameters:
        func (callable): function to execute
        iterations (int): number of repetitions

    Returns:
        dict: min, max and average execution time (seconds)
    """
def measure_time(func, iterations):
    times = []                          # list to contain the executions times
    # run the function iterations times
    for _ in range(iterations):
        start = time.perf_counter()     # high-resolution monotonic timer
        func()                          # fucntion to do (test)
        end = time.perf_counter()
        times.append(end - start)       # calculate the time of the test
    # calculate and return the metrics
    arr = np.array(times)

    return {
        "min": arr.min(),
        "p50": np.percentile(arr, 50),
        "p95": np.percentile(arr, 95),
        "p99": np.percentile(arr, 99),
        "avg": arr.mean(),
        "max": arr.max(),
    }

# Print all available benchmark tests and their associated index names.
def print_available_tests():

    print("\nAvailable benchmark tests:\n")             # UI print
    for name, cfg in TESTS.items():
        print(f"- {name}")
        print(f"  Collection : {cfg['collection']}")    # UI print
        print(f"  Index name : {cfg['index_name']}")    # UI print
        print(f"  Index keys : {cfg['index']}\n")       # UI print

"""
    Format time metrics (min, max, avg) in the most readable unit.

    Chooses unit based on the minimum time:
    - if min >= 1 → seconds
    - if 1 > min >= 1e-3 → milliseconds
    - if 1e-3 > min → microseconds

    Returns a new dict with formatted values and the unit used.
    """
def format_time_metrics(metrics: dict) -> dict:
    
    min_val = metrics["min"]        # get the min value (used as the basis for choosing the unit of measurement for time)
    
    if min_val >= 1:                # seconds
        factor = 1
        unit = "s"
    elif min_val >= 1e-3:           # milliseconds
        factor = 1e3
        unit = "ms"
    else:                           # microseconds
        factor = 1e6
        unit = "µs"

    return {
        "min": metrics["min"] * factor,
        "max": metrics["max"] * factor,
        "avg": metrics["avg"] * factor,
        "p50": metrics["p50"] * factor,
        "p95": metrics["p95"] * factor,
        "p99": metrics["p99"] * factor,        
        "unit": unit
    }
  
"""
    Drops all indexes on a MongoDB collection except the default _id index.
    
    Parameters:
        collection (pymongo.collection.Collection): MongoDB collection
"""  
def drop_collection_indexes(collection):
    
    indexes = collection.index_information()        # get indexes informations

    for index_name in indexes:                      # scroll all indexes
        if index_name != "_id_":                   
            collection.drop_index(index_name)
            print(f"- Dropped index: {index_name}") # UI print

"""
    Block until MongoDB index state matches expected_indexes.

    Parameters:
        collection: pymongo collection
        expected_indexes: set of index names that must be present
        timeout: max seconds to wait (default= 10min)
        poll_interval: seconds between checks (default= 2s)
    """
def wait_for_indexes(collection, expected_indexes: set[str], timeout: float = 600.0, poll_interval: float = 0.2):
    
    if isinstance(expected_indexes, str):
        expected_indexes = {expected_indexes}
    elif not isinstance(expected_indexes, set):
        expected_indexes = set(expected_indexes)
    
    print("Waiting indexes...")
    start = time.time()     # take the current time

    while True:             # waiting loop
        try:
            info = collection.index_information()   # get information about the indexes avaiable
            current = set(info.keys())              # get the name of the indexes avaiable

            if expected_indexes.issubset(current):  # check
                print("Indexes ready...")
                return                              # return

        except PyMongoError as e:
            print(f"[wait_for_indexes] Transient error: {type(e).__name__}")

        if time.time() - start > timeout:           # timeout check
            raise TimeoutError(
                f"Timeout waiting for indexes on {collection.name}. "
                f"Expected: {expected_indexes}"
            )

        time.sleep(poll_interval)                   # sleep
        
# format in a clear way the elapsed time for the benchmarks
def format_elapsed_time(start: datetime, end: datetime) -> str:
    delta = end - start
    total_seconds = int(delta.total_seconds())

    days = total_seconds // 86400
    remainder = total_seconds % 86400

    hours = remainder // 3600
    remainder %= 3600

    minutes = remainder // 60
    seconds = remainder % 60

    return f"{days}d {hours}h {minutes}m {seconds}s"

    
# ------------------------------------ end: utilities methods ------------------------------------

# ------------------------------------ start: file handle methods ------------------------------------

# open the output file
def open_output_file():
    global OUTPUT_FILE_HANDLE
    OUTPUT_FILE_HANDLE = open(BENCHMARK_OUTPUT_FILE, "a")
    
# close the output file
def close_output_file():
    global OUTPUT_FILE_HANDLE
    if OUTPUT_FILE_HANDLE:
        OUTPUT_FILE_HANDLE.close()
        OUTPUT_FILE_HANDLE = None

# print the mex on the screen or into output file. Default print only on the screen.
def log(msg: str = "", file: bool = False):
    if file and OUTPUT_FILE_HANDLE:
        OUTPUT_FILE_HANDLE.write(msg + "\n")    # write in the output file
        OUTPUT_FILE_HANDLE.flush() 
    else:
        print(msg)                              # print on the screen
  
# print the standard header when want to write into output file  
def print_report_header():
    global BENCHMARK_START_TIME  
    now = datetime.utcnow().strftime("%Y-%m-%d %H:%M:%S")   # get curren date and time
    BENCHMARK_START_TIME = datetime.utcnow()
    
    log("=" * line_length, True)                            # start header line
    
    if len(output_header_mex) + 2 < line_length:            # put side *
        side_len_sx = int((line_length - len(output_header_mex) - 2)/2)
        side_len_dx = line_length - len(output_header_mex) - side_len_sx - 2
        header_line_0 = "*" * side_len_sx + " " + output_header_mex + " " + "*" * side_len_dx
        
        log(header_line_0, True)
        
        side_len_sx = int((line_length - len(f"Started at: {now}") - 2)/2)
        side_len_dx = line_length - len(f"Started at: {now}") - side_len_sx - 2
        header_line_1 = "*" * side_len_sx + " " + f"Started at: {now}" + " " + "*" * side_len_dx
        
        log(header_line_1, True)
    else:                                                   # don't put side *
        log(output_header_mex, True)
        log(f"Started at: {now}", True)

    log("-" * line_length, True)                            # end header line   
   
# print the standard footer when want to write into output file    
def print_report_footer():
    global BENCHMARK_START_TIME
    now = datetime.utcnow().strftime("%Y-%m-%d %H:%M:%S")   # get curren date and time
    base_line = f"Ended at: {now}"
    
    log("", True)                                           # equal to \n
    log("-" * line_length, True)                            # end header line 
    
    if len(base_line) + 2 < line_length:            # put side *
        
        side_len_sx = int((line_length - len(base_line) - 2)/2)
        side_len_dx = line_length - len(base_line) - side_len_sx - 2
        footer_line_0 = "*" * side_len_sx + " " + base_line + " " + "*" * side_len_dx
        
        log(footer_line_0, True)
        
        if BENCHMARK_START_TIME != None:
            end_time = datetime.utcnow()
            elapsed = format_elapsed_time(BENCHMARK_START_TIME, end_time)
            base_line_1 = f"Benchmarks done in: {elapsed}"
            
            side_len_sx = int((line_length - len(base_line_1) - 2)/2)
            side_len_dx = line_length - len(base_line_1) - side_len_sx - 2
            footer_line_1 = "*" * side_len_sx + " " + base_line_1 + " " + "*" * side_len_dx
            
            log(footer_line_1, True)
    else:       
        log(base_line, True)
        if BENCHMARK_START_TIME != None:
            end_time = datetime.utcnow()
            elapsed = format_elapsed_time(BENCHMARK_START_TIME, end_time)
            log(f"Benchmarks done in: {elapsed}", True)
    
    log("=" * line_length, True)                            # start header line
    log("", True)                                           # equal to write \n
    BENCHMARK_START_TIME = None

# ------------------------------------ end: file handle methods ------------------------------------

# ------------------------------------ start: test definitions ------------------------------------
# Each test returns a callable function that will be benchmarked

# -------- asset_prices (symbol, date) --------
# function to test the query (which should improve thanks to the index)
def test_asset_prices_query(db):
    def query():
        # obtain the prices of a share over a given period of time, a typical query for constructing and displaying historical price charts for an asset
        # example 1 with APPLE
        list(
            db.asset_prices.find(
                {
                    "symbol": "AAPL",
                    "date": {
                        "$gte": datetime(2023, 1, 1),
                        "$lte": datetime(2023, 1, 31)
                    }
                }
            )
        )
        # Queries used extensively during the creation and injection of transactions in MongoDB. Also useful for historical data. In this Test use Coca Cola.
        list(
            db.asset_prices.find(
                {"symbol": "KO"},
                {"date": 1}
            ).sort("date", 1).limit(1)
        )
        list(
            db.asset_prices.find(
                {"symbol": "KO"},
                {"date": 1}
            ).sort("date", -1).limit(1)
        )        
        
    return query

# function to test the write and the deletes
def test_asset_prices_insert(db,insert_batch_size):
    def insert():
        # test single insert and delete
        for i in range(insert_batch_size):
            db.asset_prices.insert_one({
                "symbol": "TEST",
                "date": datetime.utcnow(),
                "open": i,
                "close": i,
                "high": i,
                "low": i
            })
            db.asset_prices.delete_one({"symbol": "TEST"})      # clean up
        # now test multiple inset and delete
        db.asset_prices.insert_many(documents)
        db.asset_prices.delete_many({"symbol": "TEST"})         # Clean up after test
    return insert
    
# function to create the list of document for the multiple insert and delete 
def create_asset_prics_docs(insert_batch_size):
    global documents
    
    now = datetime.utcnow()
    # create insert_batch_size documents
    for i in range(insert_batch_size):
        documents.append({
                "symbol": "TEST",                   
                "date": now + timedelta(seconds=i),  # slightly different date
                "open": i,
                "close": i,
                "high": i,
                "low": i
            })

# function to explain the usage for a query to see the usage of index or not
def explain_asset_prices_query(db):
    explain = db.command(
        "explain",
        {
            "find": "asset_prices",
            "filter": {
                "symbol": "AAPL",
                "date": {
                    "$gte": datetime(2023, 1, 1),
                    "$lte": datetime(2023, 1, 31)
                }
            }
        },
        verbosity="executionStats"
    )

    stats = explain["executionStats"]

    return {
        "executionTimeMillis": stats["executionTimeMillis"],
        "totalDocsExamined": stats["totalDocsExamined"],
        "totalKeysExamined": stats.get("totalKeysExamined", 0),
        "stage": stats["executionStages"]["stage"]
    }

# -------- transactions (user_id, date) --------
# function to test the query (which should improve thanks to the index)
def test_transactions_user_query(db):
    def query():
        # example 1, all transactions that have taken place within a certain period of time from today's date (recent history)
        list(
            db.transactions.find(
                {
                    "user_id": 28,
                    "date": {"$gte": datetime.utcnow() - timedelta(days=730)}
                }
            )
        )
        # example 2, all transaction for a user (complete history)
        list(
            db.transactions.find(
                {"symbol": "KO"},
                {"date": 1}
            ).sort("date", -1)
        )
    return query
    
# -------- transactions (type, date) --------
# function to test the query (which should improve thanks to the index)
def test_transactions_type_query(db):
    def query():
        # example 1, all buy transactions that have taken place within a period of time from today's date
        list(
            db.transactions.find(
                {
                    "type": "buy",
                    "date": {"$gte": datetime.utcnow() - timedelta(days=365)}
                }
            )
        )
        # example 2, all deposit transactions that have taken place within in a time window
        list(
            db.transactions.find(
                {
                    "type": "deposit",
                    "date": {
                        "$gte": datetime(2023, 1, 1),
                        "$lte": datetime(2023, 1, 31)
                    }
                }
            )
        )
    return query


# -------- transactions (status, date) --------
# function to test the query (which should improve thanks to the index)
def test_transactions_status_query(db):
    def query():
        # all pending transactions in order (query performed at the start of each trading day)
        list(
            db.transactions.find(
                {"status": "pending"},
                {"date": 1}
            ).sort("date", 1)
        )
    return query

# function to test the write and the deletes
def test_transactions_insert(db,insert_batch_size):
    def insert():
        # test single insert and delete
        for i in range(insert_batch_size):
            db.transactions.insert_one({
                "transaction_id": -1,
                "user_id": 1,
                "type": "buy",
                "status": "EXECUTED",
                "symbol": "AAPL",
                "date": datetime.utcnow(),
                "totalPrice": 100
            })
            db.transactions.delete_one({"transaction_id": -1})
        # now test multiple inset and delete
        db.transactions.insert_many(documents)
        db.transactions.delete_many({"transaction_id": -1})         # Clean up after test
    return insert
    
# function to create the list of document for the multiple insert and delete 
def create_transactions_docs(insert_batch_size):
    global documents
    
    now = datetime.utcnow()
    # create insert_batch_size documents
    for i in range(insert_batch_size):
        documents.append({
                "transaction_id": -1,
                "user_id": 1,
                "symbol": "AAPL",
                "type": "sell",
                "date": now,
                "currency": "USD",
                "totalPrice": 100,
                "paymentMethod": "paypal",
                "status": "EXECUTED",
                "assetType": "share",
                "pricePerUnit": 100,
                "quantity": 1,
                "updated_at": now
            })

# function to explain the usage for a query to see the usage of index or not
def explain_transactions_user_query(db):    
    explain = db.command(
        "explain",
        {
            "find": "transactions",
            "filter": {
                "user_id": "29",
                "date": {
                    "$gte": datetime.utcnow() - timedelta(days=400)
                }
            }
        },
        verbosity="executionStats"
    )

    stats = explain["executionStats"]

    return {
        "executionTimeMillis": stats["executionTimeMillis"],
        "totalDocsExamined": stats["totalDocsExamined"],
        "totalKeysExamined": stats.get("totalKeysExamined", 0),
        "stage": stats["executionStages"]["stage"]
    }
    
# function to explain the usage for a query to see the usage of index or not
def explain_transactions_type_query(db):        
    explain = db.command(
        "explain",
        {
            "find": "transactions",
            "filter": {
                "type": "buy",
                "date": {
                    "$gte": datetime.utcnow() - timedelta(days=365)
                }
            }
        },
        verbosity="executionStats"
    )

    stats = explain["executionStats"]

    return {
        "executionTimeMillis": stats["executionTimeMillis"],
        "totalDocsExamined": stats["totalDocsExamined"],
        "totalKeysExamined": stats.get("totalKeysExamined", 0),
        "stage": stats["executionStages"]["stage"]
    }
    
# function to explain the usage for a query to see the usage of index or not
def explain_transactions_status_query(db):        
    explain = db.command(
        "explain",
        {
            "find": "transactions",
            "filter": {
                "status": "pending"
            },
            "sort": {
                "date": 1
            }
        },
        verbosity="executionStats"
    )

    stats = explain["executionStats"]

    return {
        "executionTimeMillis": stats["executionTimeMillis"],
        "totalDocsExamined": stats["totalDocsExamined"],
        "totalKeysExamined": stats.get("totalKeysExamined", 0),
        "stage": stats["executionStages"]["stage"]
    }
    

# -------- users (email) --------
# function to test the query (which should improve thanks to the index)
def test_users_email_query(db):
    def query():
        # search a user by mail (login scenario)
        list(
            db.users.find({"email": "test@example.com"})
        )
        
    return query

# function to test the write and the deletes
def test_users_insert(db, insert_batch_size):
    def insert():
        # test single insert and delete
        for i in range(insert_batch_size):
            db.users.insert_one({
                "user_id": -1,
                "first_name": "first_name",
                "last_name": "last_name",
                "email": "temp_test@example.com",
                "password_hash": "password_hash",
                "role": "user",
                "birth_date": datetime.utcnow(),
                "phone": "33333333333",
                "address": "address",
                "city": "city",
                "province": "province",
                "cap": "cap",
                "registration_date": datetime.utcnow(),
                    
                "cash": 0.0,
                "blockedCash": 0.0,
                "currency": "USD",

                "shareWallet": [],
                "etfWallet": [],
                "cryptoWallet": [],
                "recentTransactions": [],

                "created_at": datetime.utcnow(),
                "updated_at": datetime.utcnow()
            })
            db.users.delete_one({"email": "temp_test@example.com"})
        # now test multiple inset and delete
        db.users.insert_many(documents)
        db.users.delete_many({"email": {"$regex": "^temp_test_"}})          # Clean up after test

    return insert

# function to create the list of document for the multiple insert and delete 
def create_users_docs(insert_batch_size):
    global documents
    
    now = datetime.utcnow()
    # create insert_batch_size documents
    for i in range(insert_batch_size):
        documents.append({
                "user_id": -1,
                "first_name": "first_name",
                "last_name": "last_name",
                "email": f"temp_test_{i}@example.com",
                "password_hash": "password_hash",
                "role": "user",
                "birth_date": now,
                "phone": "33333333333",
                "address": "address",
                "city": "city",
                "province": "province",
                "cap": "cap",
                "registration_date": now,
                    
                "cash": 0.0,
                "blockedCash": 0.0,
                "currency": "USD",

                "shareWallet": [],
                "etfWallet": [],
                "cryptoWallet": [],
                "recentTransactions": [],

                "created_at": now,
                "updated_at": now
            })
            
# function to explain the usage for a query to see the usage of index or not
def explain_user_query(db):        
    explain = db.command(
        "explain",
        {
            "find": "users",
            "filter": {
                "email": "dossiantonio@example.org"
            }
        },
        verbosity="executionStats"
    )

    stats = explain["executionStats"]

    return {
        "executionTimeMillis": stats["executionTimeMillis"],
        "totalDocsExamined": stats["totalDocsExamined"],
        "totalKeysExamined": stats.get("totalKeysExamined", 0),
        "stage": stats["executionStages"]["stage"]
    }

# -------- news (date, category) --------
# function to test the query (which should improve thanks to the index)
def test_news_query(db):
    def query():
        # example 1, search 
        list(
            db.news.find(
                {
                    "category": "Technology",
                    "date": {"$gte": datetime.utcnow() - timedelta(days=90)}
                }
            ).sort("date", -1)
        )
        # example 2 
        list(
            db.news.find(
                {
                    "category": "unknown",
                    "date": 1
                }
            ).sort("date", -1)
        )
    return query

# function to test the write and the deletes
def test_news_insert(db, insert_batch_size):
    def insert():
        now = datetime.utcnow()
        # test single insert and delete
        for i in range(insert_batch_size):
            db.news.insert_one({
                "date": now,
                "title": "Test News",
                "summary": "summary",
                "text": "text_text_text",
                "sector": "sector",
                "index": "index",
                "company": "company",
                "ingested_at": now               
            })
            db.news.delete_one({"title": "Test News"})
        # now test multiple inset and delete
        db.news.insert_many(documents)
        db.news.delete_many({"title": "Test News"})         # Clean up after test
    return insert
    
# function to create the list of document for the multiple insert and delete 
def create_news_docs(insert_batch_size):
    global documents
    
    now = datetime.utcnow()
    # create insert_batch_size documents
    for i in range(insert_batch_size):
        documents.append({
                "date": now,
                "title": "Test News",
                "summary": "summary",
                "text": "text_text_text",
                "sector": "sector",
                "index": "index",
                "company": "company",
                "ingested_at": now               
            })
            
# function to explain the usage for a query to see the usage of index or not
def explain_news_query(db):        
    explain = db.command(
        "explain",
        {
            "find": "news",
            "filter": {
                "category": "Technology",
                "date": {"$gte": datetime.utcnow() - timedelta(days=90)}
            },
            "sort": {
                "date": -1
            }
        },
        verbosity="executionStats"
    )

    stats = explain["executionStats"]

    return {
        "executionTimeMillis": stats["executionTimeMillis"],
        "totalDocsExamined": stats["totalDocsExamined"],
        "totalKeysExamined": stats.get("totalKeysExamined", 0),
        "stage": stats["executionStages"]["stage"]
    }

# ------------------------------------ index registry ------------------------------------

TESTS = {
    "asset_prices": {
        "collection": "asset_prices",
        "index": [("symbol", 1), ("date", -1)],
        "index_name": "symbol_1_date_-1",
        "query": test_asset_prices_query,
        "insert": test_asset_prices_insert,
        "insert_documents": create_asset_prics_docs,
        "explain": explain_asset_prices_query
    },
    "transactions_user_date": {
        "collection": "transactions",
        "index": [("user_id", 1), ("date", -1)],
        "index_name": "user_id_1_date_-1",
        "query": test_transactions_user_query,
        "insert": test_transactions_insert,
        "insert_documents": create_transactions_docs,
        "explain": explain_transactions_user_query
    },
    "transactions_type_date": {
        "collection": "transactions",
        "index": [("type", 1), ("date", -1)],
        "index_name": "type_1_date_-1",
        "query": test_transactions_type_query,
        "insert": test_transactions_insert,
        "insert_documents": create_transactions_docs,
        "explain": explain_transactions_type_query
    },
    "transactions_status_date": {
        "collection": "transactions",
        "index": [("status", 1), ("date", 1)],
        "index_name": "status_1_date_1",
        "query": test_transactions_status_query,
        "insert": test_transactions_insert,
        "insert_documents": create_transactions_docs,
        "explain": explain_transactions_status_query
    },
    "users_email": {
        "collection": "users",
        "index": [("email", 1)],
        "index_name": "email_1",
        "query": test_users_email_query,
        "insert": test_users_insert,
        "insert_documents": create_users_docs,
        "explain": explain_user_query
    },
    "news_date_category": {
        "collection": "news",
        "index": [("date", -1), ("category", 1)],
        "index_name": "date_-1_category_1",
        "query": test_news_query,
        "insert": test_news_insert,
        "insert_documents": create_news_docs,
        "explain": explain_news_query
    }
}

# ------------------------------------ benchmark function ------------------------------------

"""
Handle benchmark execution configuration and orchestration based on user settings:
    - whether to run a single test or all available tests,
    - whether to drop existing indexes or keep them,
    - whether to write benchmark results to an output file.
It also handles opening and closing the output file and printing standard report headers and footers when file logging is enabled.

Parameters:
    - test_name (str): Name of the benchmark test to execute (used when run_all is False).
    - iterations (int): Number of iterations used to measure query and insert execution times.
    - insert_batch_size (int): Number of documents inserted per insert benchmark iteration.
    - drop_indexes (bool, default=True): If True, all indexes on the target collection are dropped before testing. If False, all indexes are kept except the one under test.
    - run_all (bool, default=False): If True, all benchmark tests are executed sequentially, each in both configurations (drop_indexes=True and drop_indexes=False).
    - write_file (bool, default=False): If True, benchmark results are written to an output file in addition to being printed on screen.
"""
def handle_benchmark(test_name: str, iterations: int, insert_batch_size: int, drop_indexes: bool = True, run_all: bool = False, write_file: bool = False):
    
    if write_file:
        open_output_file()          # open output file
        print_report_header()       # write header
        
    if run_all:         # run all the test (both configuration: drop_indexes=true and drop_indexes= false)
        print(f"FULL BENCHMARK - Results saved to {BENCHMARK_OUTPUT_FILE}")
        
        for test_name in TESTS.keys():
            benchmark(test_name, args.iterations, args.insert_batch_size, True, write_file)     # start benchmarks (drop indexes)
            benchmark(test_name, args.iterations, args.insert_batch_size, False, write_file)    # start benchmarks (keep indexes)
        
    else:
        benchmark(args.test, args.iterations, args.insert_batch_size, drop_indexes, write_file) # start benchmarks
        
    if write_file:
        print_report_footer()       # write footer
        close_output_file()         # close output file

"""
Manage index state for a collection before running a benchmark, depending on the selected configuration, this function either:
    - drops all indexes on the collection, or
    - ensures all application indexes are present and then drops only the index under test.

This allows fair performance comparisons between:
    - a collection with no indexes, and
    - a collection with all indexes except the one being evaluated.

Parameters:
    - drop_indexes (bool): If True, all indexes on the collection are dropped. If False, all standard indexes are created (if missing) and only the tested index is removed.
    - collection (pymongo.collection.Collection): MongoDB collection on which index operations are performed.
    - name_collection (str): Human-readable name of the collection (used for logging).
    - index_name (str): Name of the index under test.
"""
def handle_indexes(drop_indexes, collection, name_collection, index_name):
    
    if drop_indexes:                        # delete all indexes in this collection - SEE NOTE 0
        log(f"Delete all indexes present in the {name_collection} collection...",False) # UI print
        drop_collection_indexes(collection)
    else:                                   # drop only the examined index if it already exists
        try:
            log(f"Apply all indexes for {name_collection}...", False)                   # UI print
            create_indexes(False)                                                       # apply all indexes if they aren't already applied
            wait_for_indexes(collection, name_collection)                               # wait index
            collection.drop_index(index_name)                                           # drop examined index
            log(f"Index {index_name} alreay applied, dropped.",False)                   # UI print
        except Exception:
            pass
            
"""
Process, format, and display benchmark results. This function:
    - computes performance metrics (min, max, average, percentiles),
    - calculates query improvement and insert penalty percentages,
    - formats time values using the most appropriate time unit,
    - prints or writes the results and MongoDB explain statistics.

Parameters:
    - q_no_idx (dict): Timing statistics for queries executed without indexes.
    - i_no_idx (dict): Timing statistics for inserts executed without indexes.
    - e_no_idx (dict): MongoDB explain execution statistics for queries without indexes.
    - q_idx (dict): Timing statistics for queries executed with the tested index.
    - i_idx (dict): Timing statistics for inserts executed with the tested index.
    - e_idx (dict): MongoDB explain execution statistics for queries with the tested index.
    - write_file (bool, default=False): If True, results are written to the benchmark output file. Otherwise, results are printed to standard output.
"""
def handle_results(q_no_idx, i_no_idx, e_no_idx, q_idx, i_idx, e_idx, write_file: bool = False):
    
    # calculate results
    improvement = (q_no_idx["p50"] - q_idx["p50"]) / q_no_idx["p50"] * 100  # percentage improvement in queries from the version with index compared to the version without index
    penalty = (i_idx["p50"] - i_no_idx["p50"]) / i_no_idx["p50"] * 100      # percentage deterioration in insertion from the version without index compared to the version with index

    # format the results with the most appropriate unit of measurement for time
    q_no_idx_fmt = format_time_metrics(q_no_idx)
    q_idx_fmt    = format_time_metrics(q_idx)
    i_no_idx_fmt = format_time_metrics(i_no_idx)
    i_idx_fmt    = format_time_metrics(i_idx)

    log("\n---- RESULTS ----",write_file)
    log(f"Query no index  -> min: {q_no_idx_fmt['min']:.3f}, max: {q_no_idx_fmt['max']:.3f}, avg: {q_no_idx_fmt['avg']:.3f}, p50: {q_no_idx_fmt['p50']:.3f} , p95: {q_no_idx_fmt['p95']:.3f}, p99: {q_no_idx_fmt['p99']:.3f} {q_no_idx_fmt['unit']}",write_file)
    log(f"Query with idx  -> min: {q_idx_fmt['min']:.3f}, max: {q_idx_fmt['max']:.3f}, avg: {q_idx_fmt['avg']:.3f}, p50: {q_idx_fmt['p50']:.3f} , p95: {q_idx_fmt['p95']:.3f}, p99: {q_idx_fmt['p99']:.3f} {q_idx_fmt['unit']}",write_file)
    log(f"Insert no idx   -> min: {i_no_idx_fmt['min']:.3f}, max: {i_no_idx_fmt['max']:.3f}, avg: {i_no_idx_fmt['avg']:.3f}, p50: {i_no_idx_fmt['p50']:.3f} , p95: {i_no_idx_fmt['p95']:.3f}, p99: {i_no_idx_fmt['p99']:.3f} {i_no_idx_fmt['unit']}",write_file)
    log(f"Insert with idx -> min: {i_idx_fmt['min']:.3f}, max: {i_idx_fmt['max']:.3f}, avg: {i_idx_fmt['avg']:.3f}, p50: {i_idx_fmt['p50']:.3f} , p95: {i_idx_fmt['p95']:.3f}, p99: {i_idx_fmt['p99']:.3f} {i_idx_fmt['unit']}",write_file)

    log("\n---- COMPARISONS (50 percentile)----",write_file)
    log(f"Query improvement (if positive -> index better than no index): {improvement:.2f}%",write_file)
    log(f"Insert penalty (if positive -> no index better than index): {penalty:.2f}%",write_file)
    
    log("\n---- EXPLAIN (SEE MONGODB STATS) ----",write_file)
    log("-- WITHOUT INDEX --")
    log(f"- executionTimeMillis (internal query execution time): {e_no_idx['executionTimeMillis']}",write_file)
    log(f"- totalDocsExamined (total number of documents read by the engine): {e_no_idx['totalDocsExamined']}",write_file)
    log(f"- totalKeysExamined (number of entries in the index read): {e_no_idx['totalKeysExamined']}",write_file)
    log(f"- stage (main stage of the implementation plan): {e_no_idx['stage']}",write_file)
    log("\n-- WITH INDEX --",write_file)
    log(f"- executionTimeMillis (internal query execution time): {e_idx['executionTimeMillis']}",write_file)
    log(f"- totalDocsExamined (total number of documents read by the engine): {e_idx['totalDocsExamined']}",write_file)
    log(f"- totalKeysExamined (number of entries in the index read): {e_idx['totalKeysExamined']}",write_file)
    log(f"- stage (main stage of the implementation plan): {e_idx['stage']}",write_file)

"""
Execute a complete benchmark cycle for a single test.

This function performs the full benchmark workflow:
1. Prepares test data.
2. Configures collection indexes.
3. Measures query and insert performance without the index.
4. Collects MongoDB explain statistics without the index.
5. Creates the tested index and waits for completion.
6. Measures query and insert performance with the index.
7. Collects MongoDB explain statistics with the index.
8. Cleans up temporary data and reports results.

Parameters:
    test_name (str):
        Name of the benchmark test to execute (key of the TESTS dictionary).

    iterations (int):
        Number of iterations for each timing measurement.

    insert_batch_size (int):
        Number of documents inserted per iteration.

    drop_indexes (bool, default=True):
        Determines how indexes are handled before the test:
        - True: all indexes are removed.
        - False: all indexes are kept except the one under test.

    write_file (bool, default=False):
        If True, benchmark output is written to a file in addition
        to console output.

Output:
    None

Side effects:
    - Performs database read/write operations.
    - Creates and drops indexes.
    - Generates timing and explain statistics.
    - Writes benchmark results to screen and/or file.
"""
def benchmark(test_name: str, iterations: int, insert_batch_size: int, drop_indexes: bool = True, write_file: bool = False):
    db = get_db()
    if db is None:
        log("ERROR - DB connection not available",write_file)
        return

    test = TESTS[test_name]                 # get the name of the test chosen by the user
    coll = db[test["collection"]]           # get the collection 

    log(f"\n-------- Benchmark: {test_name} , iterations: {iterations} , insert_batch_size: {insert_batch_size}, drop_indexes: {drop_indexes} --------",write_file) # UI print
    log(f"---- Start tests: '{test_name}' using index: '{test['index_name']}' on the collection: '{test['collection']}' ----",False)    # UI print
    log(f"\nCreate {insert_batch_size} documents for the insert and delete tests...",False)                                             # UI print

    test["insert_documents"](insert_batch_size)                                 # create the docs and populate the global var documents
    handle_indexes(drop_indexes, coll, test['collection'], test["index_name"])  # handle the indexes
    
    # -- start benchmark --
    # WITHOUT INDEX
    log("Testing without index...",False) 
    log("- Testing query...",False)     
    q_no_idx = measure_time(test["query"](db), iterations)                      # query test without index
    log("- Testing insert...",False) 
    i_no_idx = measure_time(test["insert"](db,insert_batch_size), iterations)   # insert test without index
    log("- Testing explain...",False) 
    e_no_idx = test["explain"](db)                                              # run a test to get explain data from MongoDB

    coll.create_index(test["index"], name=test["index_name"])                   # Create index
    wait_for_indexes(coll, test["index_name"])                                  # wait index

    # WITH INDEX
    log("Testing with index...",False)
    log("- Testing query...",False) 
    q_idx = measure_time(test["query"](db), iterations)                         # query test with index
    log("- Testing insert...",False) 
    i_idx = measure_time(test["insert"](db,insert_batch_size), iterations)      # insert test with index
    log("- Testing explain...",False) 
    e_idx = test["explain"](db)                                                 # run a test to get explain data from MongoDB
    
    documents.clear()                                                           # clean up the documents list

    # calculate, put in better format and show/write the results
    handle_results(q_no_idx, i_no_idx, e_no_idx, q_idx, i_idx, e_idx, write_file)
    log(f"---- End tests: '{test_name}' using index: '{test['index_name']}' on the collection: '{test['collection']}' ----",False)  # UI print

# ------------------------------------ end: methods ------------------------------------

if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="MongoDB index benchmark utility"
    )

    parser.add_argument(
        "--test",
        choices=TESTS.keys(),
        help="Name of the index benchmark to run"
    )

    parser.add_argument(
        "--iterations",
        type=int,
        default=10,
        help="Number of iterations for each benchmark (default: 10)"
    )
    
    parser.add_argument(
        "--insert_batch_size",
        type=int,
        default=10,
        help="Number of documents that must be inserted in insert test (default: 1000)"
    )

    parser.add_argument(
        "--list-tests",
        action="store_true",
        help="List all available benchmark tests and exit"
    )
    
    parser.add_argument(
        "--keep-indexes",
        action="store_true",
        help="Keep existing indexes on the collection (realistic workload benchmark)"
    )
    
    parser.add_argument(
        "--run-all",
        action="store_true",
        help="Run all benchmarks in both isolated and realistic index modes and save results to file"
    )
    
    parser.add_argument(
        "--write-res",
        action="store_true",
        help="Write the results of the benchmarks into a log file."
    )

    args = parser.parse_args()

    # check argument
    if args.list_tests:                     # help case
        print_available_tests()
        exit(0)

    if not args.test and not args.run_all:  # control check for essential arguments (name test)
        print("\nERROR: --test is required unless --list-tests is used.")
        print_available_tests()
        exit(1)

    # handle and run the benchmark and their results
    handle_benchmark(args.test, args.iterations, args.insert_batch_size, not args.keep_indexes, args.run_all, args.write_res)

"""
Usage examples:
    python -m code.test.benchmark_indexes --list-tests
    
    python -m code.test.benchmark_indexes --test asset_prices --write-res 
    python -m code.test.benchmark_indexes --test asset_prices --iterations 20 --insert_batch_size 10000
    
    python -m code.test.benchmark_indexes --test transactions_user_date
    python -m code.test.benchmark_indexes --test transactions_type_date
    python -m code.test.benchmark_indexes --test transactions_status_date
    
    python -m code.test.benchmark_indexes --test users_email  
    
    python -m code.test.benchmark_indexes --test news_date_category
    
    python -m code.test.benchmark_indexes --run-all
    python -m code.test.benchmark_indexes --run-all --write-res 
        
NOTE 0:
    To evaluate index performance, the benchmark framework supports two distinct execution modes controlled by a configuration flag.
    - drop_indexes = True (isolation mode): all existing indexes on the target collection (except the mandatory _id index) are dropped before executing the benchmark. 
            This approach allows measuring the isolated impact of a single index on query execution time and write overhead, eliminating interference from other indexes. 
            (academic analysis and controlled performance evaluation).
    - drop_indexes = False : preserve all pre-existing indexes on the collection, enabling a realistic workload mode. In this configuration, the tested index operates 
            alongside other indexes that would typically be present in a production environment. 
            (real-world behavior, including cumulative index maintenance costs).
"""
