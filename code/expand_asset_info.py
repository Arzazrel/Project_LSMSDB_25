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
    
# metod to fetch available info from Yahoo Finance for a symbol
def fetch_yahoo_info(symbol: str) -> dict:
    try:
        t = yf.Ticker(symbol)
        info = t.get_info() if hasattr(t, "get_info") else t.info
        return info or {}
    except Exception:
        return {}

# method to read the old csv and create the new csv with more information
def enrich_asset_info(input_csv: str, output_csv: str = "enriched_assets.csv"):

    delimiter = get_csv_delimiter(input_csv)                    # get delimiter
    df = pd.read_csv(input_csv, sep = delimiter, dtype=str).fillna("")          # read csv file

    # Detect type of file (company list vs ETF list vs crypto list)
    cols = [c.lower() for c in df.columns]
    is_etf = "fund name" in cols                                # column only in etf
    is_company = "company name" in cols or "security" in cols   # column only in company 
    is_crypto = "rank" in cols                                  # column only in crypto
    
    if not (is_etf or is_company or is_crypto):                 # control check
        print("Unable to determine file type: expected company or ETF or crypto CSV.")
        print("Columns found:", df.columns.tolist())
        return
    
    if "Symbol" not in cols:                                    # check for the symbol column
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
    # Prepare output columns
    if is_company:
        print("Detected: company list file (e.g., top_50_euro_company.csv)")
        df["Sector"] = ""                   # default value
        df["Industry"] = ""                 # default value

        for i in iterator:
            row = df.iloc[i]                # get current row 
            symbol = str(row.get("symbol", "")).strip().upper()
            old_name = str(row.get("Company Name", row.get("Security", ""))).strip()

            if not symbol or not is_valid_symbol(symbol):   # check if is a valid symbol
                df.at[i, "Short Name"] = old_name
                df.at[i, "Long Name"] = "N/A"
                continue                    # go to next rows

            info = fetch_yahoo_info(symbol)     # get info
            df.at[i, "Short Name"] = info.get("shortName", old_name)
            df.at[i, "Long Name"] = info.get("longName", "")
            df.at[i, "Sector"] = info.get("sector", "")
            df.at[i, "Industry"] = info.get("industry", "")

        out_cols = ["symbol", "Short Name", "Long Name", "Sector", "Industry", "Country"]   # shuffle the column in the new order
        df[out_cols].to_csv(output_csv, index=False, encoding="utf-8")      # save csv
        print(f"\n File saved as: {output_csv}")                            # UI print

    elif is_etf:
        print("Detected: ETF list file")
        df["country"] = ""                      # default value
        df["fundFamily"] = ""                   # default value
        df["annualReportExpenseRatio"] = ""     # default value
        df["totalAssets"] = ""                  # default value

        for i in iterator:
            row = df.iloc[i]                    # get current row 
            symbol = str(row.get("symbol", "")).strip().upper()
            fund_name = str(row.get("Fund Name", "")).strip()

            if not symbol or not is_valid_symbol(symbol):   # check if is a valid symbol
                df.at[i, "shortName"] = fund_name
                continue                        # go to next rows

            info = fetch_yahoo_info(symbol)     # get info
            df.at[i, "shortName"] = info.get("shortName", fund_name)
            df.at[i, "longName"] = info.get("longName", "")
            df.at[i, "country"] = info.get("country", "")
            df.at[i, "fundFamily"] = info.get("fundFamily", "")
            df.at[i, "annualReportExpenseRatio"] = info.get("annualReportExpenseRatio", "")
            df.at[i, "totalAssets"] = info.get("totalAssets", "")

        out_cols = ["symbol", "shortName", "longName","country", "fundFamily", "annualReportExpenseRatio", "totalAssets"]   # shuffle the column in the new order
        df[out_cols].to_csv(output_csv, index=False, encoding="utf-8")      # save csv
        print(f"\n File saved as: {output_csv}")                            # UI print
        
    elif is_crypto:
        print("Detected: company list file (e.g., top_50_euro_company.csv)")
        df["currency"] = ""                     # default value
        df["circulatingSupply"] = ""            # default value
        df["maxSupply"] = ""                    # default value
        
        df = df.drop(columns=["Rank"])          # remove column
        
        for i in iterator:
            row = df.iloc[i]                    # get current row 
            symbol = str(row.get("symbol", "")).strip().upper()
            crypto_name = str(row.get("Name", "")).strip()

            if not symbol or not is_valid_symbol(symbol):   # check if is a valid symbol
                df.at[i, "shortName"] = crypto_name
                df.at[i, "longName"] = crypto_name
                continue                        # go to next rows

            info = fetch_yahoo_info(symbol)     # get info
            df.at[i, "shortName"] = info.get("shortName", crypto_name)
            df.at[i, "longName"] = info.get("longName", crypto_name)
            df.at[i, "currency"] = info.get("currency", "")
            df.at[i, "circulating"] = info.get("circulatingSupply", "")
            df.at[i, "maxsupply"] = info.get("maxSupply", "")

        out_cols = ["symbol", "shortName", "longName","currency", "circulatingSupply", "maxSupply"]   # shuffle the column in the new order
        df[out_cols].to_csv(output_csv, index=False, encoding="utf-8")      # save csv
        print(f"\n File saved as: {output_csv}")                            # UI print

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
