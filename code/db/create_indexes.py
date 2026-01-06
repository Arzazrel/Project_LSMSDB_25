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
    db.users.create_index(
        [("email", 1)],
        unique=True,
        name="idx_users_email_unique"
    )

    # -- ASSET PRICES --
    print("Creating indexes working on asset_prices collection...") # UI print
    print("- Index: symbol (1) , date (-1)")                        # UI print
    db.asset_prices.create_index(
        [("symbol", 1), ("date", -1)],
        name="idx_asset_prices_symbol_date"
    )

    # -- TRANSACTIONS --
    print("Creating indexes working on transaction collection...")  # UI print
    print("- Index: user_id(1) , date(-1)")                         # UI print
    db.transactions.create_index(
        [("user_id", 1), ("date", -1)],
        name="idx_transactions_user_date"
    )

    print("- Index: type(1) , date(-1)")                            # UI print
    db.transactions.create_index(
        [("type", 1), ("date", -1)],
        name="idx_transactions_type_date"
    )

    print("- Index: status(1) , date(1)")                           # UI print
    db.transactions.create_index(
        [("status", 1), ("date", 1)],
        name="idx_transactions_status_date"
    )

    # -- NEWS --
    print("Creating indexes working on news collection...") # UI print
    print("- Index: date(-1) , category(-1)")               # UI print
    db.news.create_index(
        [("date", -1), ("category", 1)],
        name="idx_news_date_category"
    )

    # -- OPTIONAL --
    if with_asset_extra_index:
        print("Create optional indexes...")                                         # UI print
        print("On asset collection -> Index: assets.type(1) , assets.sector(1)")    # UI print
        db.assets.create_index(
            [("type", 1), ("sector", 1)],
            name="idx_assets_type_sector"
        )

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