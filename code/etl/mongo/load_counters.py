"""
Author: Alessandro Diana
Description:
    Initializes MongoDB counters used for application-level sequential IDs.

    This script sets up the counters collection with:
    - user_id
    - transaction_id

    Counters are used instead of MongoDB _id to ensure:
    - deterministic ID generation
    - database-independent logic
    - replica-safe atomic increments

Behavior:
    - If a counter already exists, it is NOT overwritten
    - If the DB is unavailable, the script aborts
"""

from code.utils.mongoDB_conn import get_db

# ---- configuration ----

INITIAL_USER_ID = 0             # the value to set the counter for user_id
INITIAL_TRANSACTION_ID = 0      # the value to set the counter for transaction_id

# ------------------------------------ start: methods ------------------------------------

# Initializes a counter with id and value passed as parameters 
def init_counter(db, counter_name: str, start_value: int):

    existing = db.counters.find_one({"_id": counter_name})      # check the existence

    if existing:
        print(f"Counter '{counter_name}' already exists (seq={existing['seq']})")
        return
    
    # set the value of the counter
    db.counters.insert_one({
        "_id": counter_name,
        "seq": start_value
    })

    print(f"Counter '{counter_name}' initialized with seq={start_value}")

# main method
def main():
    db = get_db()           # try to get DB connection
    if db is None:
        print("MongoDB unavailable. Aborting.")
        return

    print("Initializing counters...")                           # UI print
    init_counter(db, "user_id", INITIAL_USER_ID)                # set value for the user_id counter
    init_counter(db, "transaction_id", INITIAL_TRANSACTION_ID)  # set value for the transaction_id counter
    print("Counters initialization completed.")                 # UI print

# ------------------------------------ end: methods ------------------------------------

if __name__ == "__main__":
    main()

"""
Test in MongoDB (use mongosh)
use myfuture_lsmsdb_2025
db.counters.find().pretty()

Expected output:
{ "_id": "user_id", "seq": 0 }
{ "_id": "transaction_id", "seq": 0 }
"""