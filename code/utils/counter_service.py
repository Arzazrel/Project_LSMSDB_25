"""
Author: Alessandro Diana
Description:
    ETL script for inserting users into the 'users' collection
    from the CSV file 'generated_users.csv'.

    Uses:
    - mongoDB_conn.py to connect to MongoDB
    - counter_service.py to generate sequential user_ids

    Error behavior:
    - If the DB is unavailable -> abort the script
    - If the counter fails -> skip the current user
"""

from pymongo import ReturnDocument
# import from my codes
from code.utils.mongoDB_conn import get_db

# update the counter 
def get_next_sequence(counter_name: str):
    db = get_db()

    if db is None:
        print(f"[COUNTER ERROR] DB is unavailable ({counter_name})")
        return None

    counter = db.counters.find_one_and_update(
        {"_id": counter_name},
        {"$inc": {"seq": 1}},
        return_document=ReturnDocument.AFTER
    )

    if counter is None:
        print(f"[COUNTER ERROR] Counter '{counter_name}' not initialized")
        return None

    return counter["seq"]
   
# Sets the counter to a specific value. If the counter does not exist, it is created.
def set_counter_value(counter_name: str, value: int):

    db = get_db()

    if db is None:
        print(f"[COUNTER ERROR] DB is unavailable ({counter_name})")
        return False

    if not isinstance(value, int) or value < 0:
        print(f"[COUNTER ERROR] Invalid counter value: {value}")
        return False

    db.counters.update_one(
        {"_id": counter_name},
        {"$set": {"seq": value}},
        upsert=True
    )

    return True
