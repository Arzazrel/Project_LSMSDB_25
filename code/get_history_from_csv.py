"""
Author: Alessandro Diana

Description: 
    Python code to request full historical data about all asset in a .csv file using Yahoo Finance (via yfinance).
    The data is saved directly to CSV format.
    The csv file must have the first column called "Symbol" (contain the symbol related to asset).
"""

import csv
import argparse
import pandas as pd
import yfinance as yf

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
        
# method that, given a tag, downloads the entire history of daily values.
def fetch_and_save(symbol):
    print(f"Requesting full historical data for {symbol} …")                    # UI print
    data = yf.download(symbol, period="max", interval="1d", auto_adjust=False)  # Download all available data (daily)

    if data.empty:    
        print(f"No data found for {symbol}. Check if the symbol is valid.")     # UI print
        return

    # Control check and fix for the case of multi index column
    if isinstance(data.columns, pd.MultiIndex):
        data.columns = [col[0] for col in data.columns]

    data.reset_index(inplace=True)
    data.insert(1, "symbol", symbol)    # add the symbol of the asset as first column

    # set the column name
    rename_map = {
        "Date": "date",
        "Open": "open",
        "High": "high",
        "Low": "low",
        "Close": "close",
        "Volume": "volume"
    }
    data.rename(columns=rename_map, inplace=True)   # name columns

    csv_filename = f"{symbol}_historical_daily.csv" # save the CSV file
    try:
        cols = ["date", "symbol", "open", "high", "low", "close", "volume"]
        available_cols = [c for c in cols if c in data.columns]
        data.to_csv(csv_filename, index=False, columns=available_cols)
        print(f"Saved CSV to {csv_filename}")       # UI print
    except Exception as e:
        print(f"Error saving CSV: {e}")             # UI print
        
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


# ------------------------------------ end: methods ------------------------------------

if __name__ == "__main__":
    # get arguments
    parser = argparse.ArgumentParser(description="Given a csv file containing asset symbols, download all historical (daily) data using Yahoo Finance.")
    parser.add_argument("--name", type=str, default="SP_500.csv", help="CSV file name (default: SP_500.csv)")
    args = parser.parse_args()
    
    delimiter = get_csv_delimiter(args.name)    # get delimiter
    
    df = pd.read_csv(args.name, sep = delimiter)# read csv
    tickers = df["Symbol"].tolist()             # create the ticker list

    print(f"Loaded {len(tickers)} ticker.")     # UI print
    count = 0                                   # initialize counter
    # download the history for each ticket
    for symbol in tickers:
        print(f"-------- Symbol {count} of {len(tickers)} --------")            # UI print
        
        try:
            if is_valid_symbol(symbol):         # check if the symbol is valid
                ticker = yf.Ticker(symbol)                                      # get ticker
                info = ticker.info                                              # get info
                name = info.get("longName") or info.get("shortName", "Unknown") # get name related to the asset
                print(f"{symbol}: {name}")                                      # UI print

                fetch_and_save(symbol)          # get historical data and save csv file

        except Exception as e:
            print(f"Error with {symbol}: {e}")  # UI print
        
        count += 1                              # update counter
        print(f"--------  --------")            # UI print
