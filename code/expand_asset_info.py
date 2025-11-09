"""
Author: Alessandro Diana
Description:
    Reads a CSV with columns: symbol, Security, GICS Sector, GICS Sub-Industry, Headquarters Location
    For each 'symbol', queries Yahoo Finance to obtain the shortName and longName.
    Creates a new CSV where 'Security' is replaced by 'Short Name' and 'Long Name'.
"""

import argparse
import time
import sys
import requests
import csv

import pandas as pd
import yfinance as yf

# try to import tqdm; if not available, use a simple fallback
try:
    from tqdm import tqdm
    TQDM_AVAILABLE = True
except Exception:
    TQDM_AVAILABLE = False

# ------------------------------------ start: methods ------------------------------------

# method to check the symbol
def is_valid_symbol(symbol: str) -> bool:
    try:
        ticker = yf.Ticker(symbol)          # get ticket
        data = ticker.history(period="1d")  # get history informations
        if data.empty:                      # check if the history is empty
            print(f"The symbol '{symbol}' is not valid or has no data (possibly delisted).")
            return False
        return True
        
    except Exception as e:
        print(f"Error checking symbol '{symbol}': {e}")
        return False
 
# method to get the shortname and longname or None if there is an error.
def fetch_names(symbol: str, timeout: float = 10.0):

    try:
        t = yf.Ticker(symbol)
        info = t.get_info() if hasattr(t, "get_info") else t.info
        short = info.get("shortName") or info.get("name") or ""
        long = info.get("longName") or short or ""
        return short or "", long or ""
        
    except Exception as e:
        return "", ""
       
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

# method to read the old csv and create the new csv with more information
def enrich_asset_info(input_csv: str, output_csv: str = "enriched_assets.csv"):

    delimiter = get_csv_delimiter(input_csv)                    # get delimiter
    df = pd.read_csv(input_csv, sep = delimiter, dtype=str).fillna("")          # read csv file
    
    if "symbol" not in df.columns:                              # check for the symbol column
        cols_lower = {c.lower(): c for c in df.columns}
        if "symbol" in cols_lower:
            df.rename(columns={cols_lower["symbol"]: "symbol"}, inplace=True)
        else:
            print("ERROR: the input CSV does not contain the 'symbol' column.") # UI print
            print("Columns found:", df.columns.tolist())                        # UI print
            return

    # add new columns
    df["Short Name"] = ""
    df["Long Name"] = ""
    
    # iteration for each row 
    iterator = range(len(df))
    if TQDM_AVAILABLE:
        iterator = tqdm(iterator, desc="Fetching from Yahoo Finance", unit="sym")   # set progression bar
    for i in iterator:
        row = df.iloc[i]
        symbol = str(row["symbol"]).strip().upper()
        #old_security = str(row.get("Security", "")).strip()        # for SP_xxx.csv file
        old_security = str(row.get("Company Name", "")).strip()     # for top_50_euro_company.csv file
        if not symbol:
            df.at[i, "Short Name"] = old_security   # default value
            df.at[i, "Long Name"] = ""              # default value
            continue


        ok = is_valid_symbol(symbol)            # check if is a valid symbol
        if not ok:
            print(f"[WARN] {symbol}: no data / symbol invalid (marked as N/A)")
            df.at[i, "Short Name"] = old_security   # default value
            df.at[i, "Long Name"] = "N/A"           # default value
            continue                            # go to next rows

        # name fetching
        short, long = fetch_names(symbol)           # get the names
        if (not short) and (not long):
            print(f"[WARN] {symbol}: unable to get names, writing N/A")
            df.at[i, "Short Name"] = "N/A"
            df.at[i, "Long Name"] = "N/A"
        else:
            df.at[i, "Short Name"] = short
            df.at[i, "Long Name"] = long


    cols = list(df.columns)         # get current columns
    if "Security" in cols:          # check if there is Security
        cols.remove("Security")     # remove security column

    #desired = ["symbol", "Short Name", "Long Name", "GICS Sector", "GICS Sub-Industry", "Headquarters Location"]    # shuffle the column in the new order, for SP_xxx.csv file
    desired = ["symbol", "Short Name", "Long Name", "Country"]    # shuffle the column in the new order, for top_50_euro_company.csv file
    
    df[desired].to_csv(output_csv, index=False, encoding="utf-8")   # save csv
    print(f"\n File saved as: {output_csv}")                        # UI print

# ------------------------------------ end: methods ------------------------------------

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Enrich CSV with shortName and longName from Yahoo Finance")
    parser.add_argument("--input", "-i", required=True, help="Input CSV file path")
    parser.add_argument("--output", "-o", default="enriched_assets.csv", help="Output CSV file path")
    args = parser.parse_args()

    try:
        enrich_asset_info(args.input, args.output)
    except KeyboardInterrupt:
        print("\nInterrupt by user.")
        sys.exit(1)
