"""
Author: Alessandro Diana
Description:
    A utility for connecting to MongoDB.
    Provides a centralized function for getting the database.

    Error Behavior:
    - If MongoDB is unreachable:
    - Prints an error message
    - Returns None

    Modules using get_db() MUST check the return value.
"""

from pymongo import MongoClient
from pymongo.errors import ServerSelectionTimeoutError

# -- DB config parameters --
MONGO_URI = "mongodb://localhost:27017"
DB_NAME = "myfuture_db"

# return the connection to myfuture_db or None in case of error
def get_db():
    try:
        client = MongoClient(
            MONGO_URI,
            serverSelectionTimeoutMS=3000
        )
        client.admin.command("ping")        # check with ping
        return client[DB_NAME]              # return the DB object

    except ServerSelectionTimeoutError as e:
        print("[DB ERROR] Unable to connect to MongoDB ():", e)
        return None