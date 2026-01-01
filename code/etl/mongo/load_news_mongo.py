"""
Author: Alessandro Diana
Description:
    Python code to inject news data processed from the datasets used for the project into MongoDB.
"""

import pandas as pd
from pymongo import MongoClient
from datetime import datetime
import os
import csv
# import from my codes
from code.utils.mongoDB_conn import get_db

# -- DB config parameters --
COLLECTION_NAME = "news"

# -- path --
CSV_PATH = "../../dataset/news/processed_ds/final_news_ds.csv"

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

    if pd.isna(value):
        return None
    if isinstance(value, str) and value.strip() == "":
        return None
    return value

# Convert date string to datetime. Adjust format if needed.
def parse_date(value):

    try:
        return pd.to_datetime(value)    # convert format
    except Exception:
        return None
        
# ------------------------------------ end: utils methods ------------------------------------

# ------------------------------------ start: load method ------------------------------------

# read the data from csv file and load them into MongoDB
def load_news():
    
    print("Check csv delimiter...")                 # UI print
    delimiter = get_csv_delimiter(CSV_PATH)         # get delimiter
    print("Reading in: ",CSV_PATH, " CSV file...")  # UI print
    df = pd.read_csv(CSV_PATH, sep = delimiter, encoding="utf-8")   # read csv file
    print("Rows found: {len(df)}")                  # UI print

    documents = []              # contains formatted documents to be inserted into MongoDB

    for _, row in df.iterrows():                            # scroll all the rows in the csv file
        doc = {
            "date": parse_date(row.get("Date")),
            "title": clean_value(row.get("Title")),
            "summary": clean_value(row.get("Summary")),
            "text": clean_value(row.get("Text")),
            "sector": clean_value(row.get("Sector")),
            "index": clean_value(row.get("Index")),
            "company": clean_value(row.get("Company")),
            "ingested_at": datetime.utcnow()                # add the injection date 
        }

        documents.append(doc)

    if not documents:                               # control check
        print("No documents to insert.")            # UI print
        return

    print("Connecting to MongoDB...")               # UI print
    db = get_db()                                   # get db connection
    collection = db[COLLECTION_NAME]

    print("Inserting documents...")                 # UI print
    result = collection.insert_many(documents)

    print(f"Inserted {len(result.inserted_ids)} news documents.")   # UI print

# ------------------------------------ end: load method ------------------------------------

if __name__ == "__main__":
    load_news()


"""
istruction to use and test this script.
Execute:
python load_news_mongo.py

Expected output:

Check csv delimiter...
Reading in: path CSV file...
Rows found: numRows
Connecting to MongoDB...
Inserting documents...
Inserted numRows news documents.

Test in MongoDB

start MongoDB
mongosh
use myfuture_lsmsdb_2025
db.news.countDocuments()
db.news.findOne()

Or to discriminate by ingestion date:
db.news.find().sort({ ingested_at: -1 }).limit(5)
"""