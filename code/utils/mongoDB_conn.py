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
#MONGO_URI = "mongodb://localhost:27017"     # case of no replica set
# case of replica set, ensure tu connect to primary replica
MONGO_URI = (
    "mongodb://localhost:27017,localhost:27018,localhost:27019/"
    "?replicaSet=rs0"
)
DB_NAME = "myfuture_lsmsdb_2025"

# return the connection to myfuture_lsmsdb_2025 or None in case of error
def get_db(majority=False):
    try:
        if majority:
            client = MongoClient(
                MONGO_URI,
                serverSelectionTimeoutMS=3000,
                w=majority
            )
        else:
            client = MongoClient(
                MONGO_URI,
                serverSelectionTimeoutMS=4000,
                w=1,                                # must wait for ack only from the primary node
                retryWrites=False                   # avoid retry during step-down
            )
        client.admin.command("ping")        # check with ping
        return client[DB_NAME]              # return the DB object

    except ServerSelectionTimeoutError as e:
        print("[DB ERROR] Unable to connect to MongoDB ():", e)
        return None