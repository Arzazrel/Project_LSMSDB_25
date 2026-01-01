"""
Author: Alessandro Diana
Description:
- Loads processed CSV asset lists (shares, ETFs, cryptocurrencies)
- Normalizes documents according to the MongoDB asset schema
- Inserts all assets into the 'assets' collection
- Adds ingestion timestamp for auditability
"""

import csv
import pandas as pd
from pymongo import MongoClient
from datetime import datetime
from pathlib import Path
# import from my codes
from code.utils.mongoDB_conn import get_db

# -- DB config parameters --
COLLECTION_NAME = "assets"

# -- path --
BASE_PATH = Path("dataset/asset_lists/v2")

# -- inpt files --
FILES = {
    "share": "share_list.csv",
    "ETF": "etf_list.csv",
    "crypto": "crypto_list.csv"
}

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


# Convert empty strings or NaN to None
def clean_value(value):
    if pd.isna(value) or str(value).strip() == "":
        return None
    return value

# ------------------------------------ end: utils methods ------------------------------------

# ------------------------------------ start: load method ------------------------------------

# read the data from csv file and load them into MongoDB
def load_assets():
    
    documents = []              # contains formatted documents to be inserted into MongoDB

    # ---- SHARES ----
    print("Check csv delimiter...")                                     # UI print
    delimiter = get_csv_delimiter(BASE_PATH / FILES["share"])           # get delimiter
    print("Reading in: ",BASE_PATH / FILES["share"], " CSV file...")    # UI print
    df_shares = pd.read_csv(BASE_PATH / FILES["share"], sep = delimiter, encoding="utf-8")  # read csv file

    for _, row in df_shares.iterrows():                                 # scroll all the rows in the csv file
        documents.append({
            "symbol": clean_value(row.get("Symbol")),
            "shortName": clean_value(row.get("Short Name")),
            "longName": clean_value(row.get("Long Name")),
            "type": "share",
            "country": clean_value(row.get("Country")),
            "sector": clean_value(row.get("Sector")),
            "industry": clean_value(row.get("Industry")),
            "ingested_at": datetime.utcnow()                            # add the injection date 
        })

    # ---- ETFs ----
    print("Check csv delimiter...")                                     # UI print
    delimiter = get_csv_delimiter(BASE_PATH / FILES["ETF"])             # get delimiter
    print("Reading in: ",BASE_PATH / FILES["ETF"], " CSV file...")      # UI print
    df_etf = pd.read_csv(BASE_PATH / FILES["ETF"] sep = delimiter, encoding="utf-8")    # read csv file
    
    for _, row in df_etf.iterrows():                                    # scroll all the rows in the csv file
        documents.append({
            "symbol": clean_value(row.get("symbol")),
            "shortName": clean_value(row.get("shortName")),
            "longName": clean_value(row.get("longName")),
            "type": "ETF",
            "country": clean_value(row.get("country")),
            "fundFamily": clean_value(row.get("fundFamily")),
            "annualReportExpenseRatio": clean_value(row.get("annualReportExpenseRatio")),
            "totalAssets": clean_value(row.get("totalAssets")),
            "ingested_at": datetime.utcnow()                            # add the injection date 
        })

    # ---- CRYPTO ----
    print("Check csv delimiter...")                                     # UI print
    delimiter = get_csv_delimiter(BASE_PATH / FILES["crypto"])          # get delimiter
    print("Reading in: ",BASE_PATH / FILES["crypto"], " CSV file...")   # UI print
    df_crypto = pd.read_csv(BASE_PATH / FILES["crypto"] sep = delimiter, encoding="utf-8")  # read csv file


    for _, row in df_crypto.iterrows():
        documents.append({
            "symbol": clean_value(row.get("symbol")),
            "shortName": clean_value(row.get("shortName")),
            "longName": clean_value(row.get("longName")),
            "type": "crypto",
            "currency": clean_value(row.get("currency")),
            "circulatingSupply": clean_value(row.get("circulatingSupply")),
            "maxSupply": clean_value(row.get("maxSupply")),
            "ingested_at": datetime.utcnow()
        })

    # Insert into MongoDB
    print("Connecting to MongoDB...")               # UI print
    db = get_db()                                   # get db connection
    assets_col = db[COLLECTION_NAME]
    print("Inserting documents...")                 # UI print
    # assets_col.delete_many({})    #if you want clean collection before inserting (ONLY FOR TESTING)
    result = assets_col.insert_many(documents)      # add insert command

    print(f"Inserted {len(result.inserted_ids)} assets into MongoDB")

# ------------------------------------ end: load method ------------------------------------

if __name__ == "__main__":
    load_assets()


"""
istruction to use and test this script.
Execute:
python load_assets_mongo.py

Test in MongoDB

start MongoDB
mongosh
use myfuture_lsmsdb_2025
// Numero totale asset
db.assets.countDocuments()              

// Conta per tipo
db.assets.aggregate([
  { $group: { _id: "$type", count: { $sum: 1 } } }
])

// Ex. with share
db.assets.find({ type: "share" }).limit(3).pretty()

// Ex. with  ETF
db.assets.find({ type: "ETF" }).limit(3).pretty()

// Ex. with  crypto
db.assets.find({ type: "crypto" }).limit(3).pretty()

// check fields
db.assets.findOne({ symbol: "AAPL" })
"""
