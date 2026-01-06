"""
Author: Alessandro Diana
Description:
    Utility script to drop all non-default MongoDB indexes
    created by the application.

    The script:
    - connects to the database
    - iterates over all collections
    - removes every index except the mandatory _id_ index

    This script is useful for:
    - resetting the database before benchmarks
    - evaluating performance without indexes
    - development and testing environments
"""

from code.utils.mongoDB_conn import get_db

# ------------------------------------ start: methods ------------------------------------

def drop_all_indexes():
    db = get_db()
    if db is None:
        print("ERROR - DB connection not available")
        return

    for collection_name in db.list_collection_names():
        collection = db[collection_name]

        indexes = collection.index_information()

        for index_name in indexes:
            # MongoDB requires the _id_ index, it cannot be dropped
            if index_name == "_id_":
                continue

            collection.drop_index(index_name)
            print(f"Dropped index '{index_name}' from collection '{collection_name}'")  # UI print

    print("All indexes removed successfully.")          # UI print

# ------------------------------------ end: methods ------------------------------------

if __name__ == "__main__":
    drop_all_indexes()

"""
Usage:
    python -m code.db.drop_all_indexes
"""