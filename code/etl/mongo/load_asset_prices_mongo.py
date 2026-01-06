"""
Author: Alessandro Diana
Description:
- Reads historical daily price CSV files for assets (shares, ETFs, crypto)
- Each CSV contains the full daily price history of a single asset
- All records are loaded into the 'asset_prices' MongoDB collection
- Adds ingestion timestamp for traceability and auditing
"""

import csv
import time
import pandas as pd
from pymongo import MongoClient
from datetime import datetime
from pathlib import Path
# import from my codes
from code.utils.mongoDB_conn import get_db

BATCH_SIZE = 1000                   # max number of document for each insert.many()
SLEEP_TIME = 0.05                   # sleep time between the insert.many(), expressed in seconds

# -- DB config parameters --
COLLECTION_NAME = "asset_prices"

# -- path --
BASE_DIR = Path("dataset/assets")

# -- inpt files --
""" # SEE NOTE 0
INPUT_FOLDERS = [
    "crypto",
    "etf",
    "SP_400_DS",
    "SP_500_DS",
    "SP_600_DS",
    "top_50_euro_company"
]"""
"""
# step 1 ->  874381 documents
INPUT_FOLDERS = [
    "crypto",
    "etf",
    "SP_400_DS_0"
]"""
"""
# step 2 ->  2253064 documents
INPUT_FOLDERS = [
    "SP_400_DS_1",
    "SP_400_DS_2",
    "SP_400_DS_3"
]"""
"""
# step 3 -> 1362478 documents
INPUT_FOLDERS = [
    "SP_500_DS_0",
    "SP_500_DS_1"
]"""
"""
# step 4 -> 1294101 documents
INPUT_FOLDERS = [
    "SP_500_DS_2",
    "SP_500_DS_3"
]"""
"""
# step 5 -> 1669364 documents
INPUT_FOLDERS = [
    "SP_500_DS_4"
]]"""

# step 7 ->  770535 documents
INPUT_FOLDERS = [
    "SP_600_DS_0"
]
"""
# step 7 -> 2996730 documents
INPUT_FOLDERS = [
    "SP_600_DS_1",
    "SP_600_DS_2",
    "SP_600_DS_3",
    "SP_600_DS_4",
]"""
"""
# step 8 -> 300249 documents
INPUT_FOLDERS = [
    "top_50_euro_company"
]"""

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
def load_asset_prices():
    
    documents = []              # contains formatted documents to be inserted into MongoDB
    files_processed = 0         # indicate the number of input files (history of an assets) processed
    tot_files_proc = 0
    tot_documents = 0

    for folder in INPUT_FOLDERS:            # scroll all input folders
        folder_path = BASE_DIR / folder     # take the current path
        print("Reading in: ",folder_path, "...")    # UI print

        for csv_file in folder_path.glob("*.csv"):  # scroll all csv files in the current folder
            files_processed += 1                    # update processed file counter
            tot_files_proc += 1
                                             
            delimiter = get_csv_delimiter(csv_file)                         # get delimiter            
            df = pd.read_csv(csv_file, sep = delimiter, encoding="utf-8")   # read csv file

            for _, row in df.iterrows():                    # read all rows of the current file
                documents.append({
                    "date": pd.to_datetime(row["date"]),
                    "symbol": clean_value(row["symbol"]),
                    "open": clean_value(row["open"]),
                    "high": clean_value(row["high"]),
                    "low": clean_value(row["low"]),
                    "close": clean_value(row["close"]),
                    "volume": clean_value(row["volume"]),
                    "ingested_at": datetime.utcnow()
                })
    
        print(f"CSV files processed in this folder: {files_processed}")    # UI print
        print(f"Documents to insert: {len(documents)}")     # UI print
        
        files_processed = 0

        # Insert into MongoDB
        db = get_db(False)              # get db connection
        prices_col = db[COLLECTION_NAME]
        # prices_col.delete_many({})    #if you want clean collection before inserting (ONLY FOR TESTING)

        if documents:
            count = 0
            for i in range(0, len(documents), BATCH_SIZE):
                result = prices_col.insert_many(
                    documents[i:i + BATCH_SIZE],
                    ordered=False
                )
                count += len(result.inserted_ids)
                print(f"Inserted {len(result.inserted_ids)} asset price records. -- [{count}|{len(documents)}]")   # UI print
                time.sleep(SLEEP_TIME)                          # sleep time
        else:
            print("No documents to insert")                     # UI print
            
        tot_documents += len(documents)
        documents.clear() 
    
    print(f"In total:\n- CSV files processed: {tot_files_proc}\n- inserted {tot_documents} asset price records.")   # UI print
   
# ------------------------------------ end: load method ------------------------------------

if __name__ == "__main__":
    load_asset_prices()   
    
"""
Istruction to use and test this script.
Execute:
python load_assets_prices_mongo.py

Test in MongoDB:

start MongoDB
mongosh
use myfuture_lsmsdb_2025
       
// Total records
db.asset_prices.countDocuments()

// History of a specific Asset (AAPL = Apple)
db.asset_prices.find({ symbol: "AAPL" }).sort({ date: -1 }).limit(5)

// Ex - OHLC monthly aggregate
db.asset_prices.aggregate([
  { $match: { symbol: "AAPL" } },
  {
    $group: {
      _id: { year: { $year: "$date" }, month: { $month: "$date" } },
      open: { $first: "$open" },
      close: { $last: "$close" },
      high: { $max: "$high" },
      low: { $min: "$low" },
      volume: { $sum: "$volume" }
    }
  }
])

NOTE 0:
    Ingesting all asset_prices can be very heavy; a document will be created for each row of the CSV (price data for an asset for one day).

In total, for all CSV files, the documents to be created are ...

The best way to manage this load is to split the CSV files. For this reason, the input files can be commented and uncommented by following the various steps to divide the server's workload.
"""