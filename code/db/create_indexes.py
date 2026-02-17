"""
Author: Alessandro Diana
Description:
    Script to create MongoDB indexes required by the application.
    - Mandatory indexes are always created.
    - Optional indexes can be enabled via command-line flags.
"""

import argparse
from code.utils.mongoDB_conn import get_db

# ------------------------------------ start: methods ------------------------------------

# create the indexes for the project in MongoDB
def create_indexes(extra_index: bool = False):
    db = get_db()
    if db is None:
        print("ERROR - DB connection not available")
        return

    print("Creating MongoDB indexes...")                        # UI print

    # -- USERS --
    print("Creating indexes working on users collection...")    # UI print
    print("- Index: email , (unique)")                      # UI print
    users_indexes = db.users.index_information()
    if "email_1" not in users_indexes:
        db.users.create_index(
            [("email", 1)],
            unique=True,
            name="email_1"
        )
    else:
        print("  -> Index already exists, skipped.")
        
    if "user_id_1" not in users_indexes:
        db.users.create_index(
            [("user_id", 1)],
            unique=True,
            name="user_id_1"
        )
    else:
        print("  -> Index already exists, skipped.")

    # -- ASSET PRICES --
    print("Creating indexes working on asset_prices collection...") # UI print
    print("- Index: symbol (1) , date (-1)")                        # UI print
    asset_prices_indexes = db.asset_prices.index_information()
    if "symbol_1_date_-1" not in asset_prices_indexes:
        db.asset_prices.create_index(
            [("symbol", 1), ("date", -1)],
            name="symbol_1_date_-1"
        )
    else:
        print("  -> Index already exists, skipped.")

    # -- TRANSACTIONS --
    print("Creating indexes working on transaction collection...")  # UI print
    
    transactions_indexes = db.transactions.index_information()      # get index informations for the transaction collection
    
    if "user_id_1_date_-1" not in transactions_indexes:
        db.transactions.create_index(
            [("user_id", 1), ("date", -1)],
            name="user_id_1_date_-1"
        )
    else:
        print("  -> Index already exists, skipped.")

    print("- Index: type (1), date (-1)")                              # UI print
    if "type_1_date_-1" not in transactions_indexes:
        db.transactions.create_index(
            [("type", 1), ("date", -1)],
            name="type_1_date_-1"
        )
    else:
        print("  -> Index already exists, skipped.")

    print("- Index: status (1), date (1)")                             # UI print
    if "status_1_date_1" not in transactions_indexes:
        db.transactions.create_index(
            [("status", 1), ("date", 1)],
            name="status_1_date_1"
        )
    else:
        print("  -> Index already exists, skipped.")
    
    if "transaction_id_1" not in users_indexes:
        db.transactions.create_index(
            [("transaction_id", 1)],
            unique=True,
            name="transaction_id_1"
        )
    else:
        print("  -> Index already exists, skipped.")

    # -- NEWS --
    print("Creating indexes working on news collection...") # UI print
    print("- Index: date(-1) , category(-1)")               # UI print
    news_indexes = db.news.index_information()
    if "date_-1_category_1" not in news_indexes:
        db.news.create_index(
            [("date", -1), ("category", 1)],
            name="date_-1_category_1"
        )
    else:
        print("  -> Index already exists, skipped.")

    # -- OPTIONAL --
    if extra_index:
        print("Create optional indexes...")                                         # UI print
        print("On asset collection -> Index: assets.type(1) , assets.sector(1)")    # UI print
        
        assets_indexes = db.assets.index_information()
        if "type_1_sector_1" not in assets_indexes:
            db.assets.create_index(
                [("type", 1), ("sector", 1)],
                name="type_1_sector_1"
            )
        else:
            print("  -> Optional index already exists, skipped.")

    print("Indexes creation completed.")                # UI print

# ------------------------------------ end: methods ------------------------------------

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--extra_index",
        action="store_true",
        help="Create optional index on assets(type, sector)"
    )
    args = parser.parse_args()

    create_indexes(extra_index=args.extra_index)

"""
Usage:
    python -m code.db.create_indexes
    python -m code.db.create_indexes --extra_index
"""