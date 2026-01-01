"""
Author: Alessandro Diana
Description:
- Reads historical daily price CSV files for assets (shares, ETFs, crypto)
- Each CSV contains the full daily price history of a single asset
- All records are loaded into the 'asset_prices' MongoDB collection
- Adds ingestion timestamp for traceability and auditing
"""

import pandas as pd
from pymongo import MongoClient
from datetime import datetime
from pathlib import Path
# import from my codes
from code.utils.mongoDB_conn import get_db

# -- DB config parameters --
COLLECTION_NAME = "asset_prices"

# -- path --
BASE_DIR = Path("dataset/assets")

# -- inpt files --
INPUT_FOLDERS = [
    "crypto",
    "etf",
    "SP_400_DS",
    "SP_500_DS",
    "SP_600_DS",
    "top_50_euro_company"
]

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

    for folder in INPUT_FOLDERS:            # scroll all input folders
        folder_path = BASE_DIR / folder     # take the current path
        print("Reading in: ",folder_path, "...")    # UI print

        for csv_file in folder_path.glob("*.csv"):  # scroll all csv files in the current folder
            files_processed += 1                    # update processed file counter
                                             
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
    
    print(f"CSV files processed: {files_processed}")    # UI print
    print(f"Documents to insert: {len(documents)}")     # UI print

    # Insert into MongoDB
    db = get_db()                   # get db connection
    prices_col = db[COLLECTION_NAME]
    # prices_col.delete_many({})    #if you want clean collection before inserting (ONLY FOR TESTING)

    if documents:
        result = prices_col.insert_many(documents, ordered=False)           # add insert command
        print(f"Inserted {len(result.inserted_ids)} asset price records")   # UI print
    else:
        print("No documents to insert")                                     # UI print
   
# ------------------------------------ end: load method ------------------------------------

if __name__ == "__main__":
    load_asset_prices()   
    
"""
istruction to use and test this script.
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
"""