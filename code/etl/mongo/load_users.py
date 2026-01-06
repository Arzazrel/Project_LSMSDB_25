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
"""

import csv
from datetime import datetime
# import from my codes
from code.utils.mongoDB_conn import get_db
from code.utils import counter_service

CSV_PATH = "dataset/user/generated_users.csv"
COUNTER_NAME = "user_id"

# ------------------------------------ start: utils methods ------------------------------------

"""
Automatically detects the delimiter of a CSV file.

Parameters:
- file_path: path to the CSV file
- n_lines: number of lines to read to detect the delimiter (default=20)

Returns:
- delimiter as a string (e.g., ',', ';', '\t', etc.)
"""
def get_csv_delimiter(file_path: str, n_lines: int = 20) -> str:
    with open(file_path, "r", encoding="utf-8") as f:
        sample_lines = "".join([f.readline() for _ in range(n_lines)])
        sniffer = csv.Sniffer()
        dialect = sniffer.sniff(sample_lines)
        delimiter = dialect.delimiter
    return delimiter

# convert data
def parse_date(value):
    if not value or value == "":
        return None
    return datetime.fromisoformat(value)

# ------------------------------------ end: utils methods ------------------------------------

# ------------------------------------ start: load method ------------------------------------

# read the data from csv file and load them into MongoDB
def ingest_users():
    
    db = get_db()       # get db connection
    
    if db is None:      # control check
        print("DB not available. Operation canceled.")
        return
    else:
        print("DB available and connected.")

    max_user_id = 0     # maximum user ID found
    inserted = 0        # indicate the number of document inserted into DB

    with open(CSV_PATH, newline="", encoding="utf-8") as csvfile:   # read the dataset
        reader = csv.DictReader(csvfile)                # read rows 

        for row in reader:                              # scroll all rows
            
            if int(row["id"]) > max_user_id:            # check for current max user_id
                max_user_id = int(row["id"])            # update current max user_id
            
            # create the document for the current row (user or admin)
            if row["role"] == "admin":  # admin case
                user_doc = {
                    "user_id": row["id"],
                    "first_name": row["first_name"],
                    "last_name": row["last_name"],
                    "email": row["email"],
                    "password_hash": row["password_hash"],
                    "role": row["role"],
                    "birth_date": parse_date(row["birth_date"]),
                    "phone": row["phone"],
                    "address": row["address"],
                    "city": row["city"],
                    "province": row["province"],
                    "cap": row["cap"],
                    "registration_date": parse_date(row["registration_date"]),
                    
                    "created_at": datetime.utcnow(),
                    "updated_at": datetime.utcnow()
                }
            else:                       # user case
                user_doc = {
                    "user_id": row["id"],
                    "first_name": row["first_name"],
                    "last_name": row["last_name"],
                    "email": row["email"],
                    "password_hash": row["password_hash"],
                    "role": row["role"],
                    "birth_date": parse_date(row["birth_date"]),
                    "phone": row["phone"],
                    "address": row["address"],
                    "city": row["city"],
                    "province": row["province"],
                    "cap": row["cap"],
                    "registration_date": parse_date(row["registration_date"]),
                    
                    "cash": float(row["balance"]),
                    "blockedCash": 0.0,
                    "currency": "USD",

                    "shareWallet": [],
                    "etfWallet": [],
                    "cryptoWallet": [],
                    "recentTransactions": [],

                    "created_at": datetime.utcnow(),
                    "updated_at": datetime.utcnow()
                }
            
            db.users.insert_one(user_doc)               # insert the document into DB
            inserted = inserted + 1                     # update counter
            
    # Finally, the counter value must be updated so that it is consistent with the userID values given in the taset, input csv file.
    # The counter value will be set equal to the highest user_id value found.
    counter_service.set_counter_value(COUNTER_NAME, max_user_id)

    print(f"Inserted users: {inserted}")                # UI print
    
# ------------------------------------ end: load method ------------------------------------

if __name__ == "__main__":
    ingest_users()
