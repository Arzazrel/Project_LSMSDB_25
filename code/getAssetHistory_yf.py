"""
Author: Alessandro Diana

Description: 
    Python code to request full historical data about an asset using Yahoo Finance (via yfinance).
    The data is saved directly to CSV format.
    Optionally, you can search for the correct ticker using a company name.
"""

import yfinance as yf
import requests
import pandas as pd
import time

# ------------------------------------ start: methods ------------------------------------

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

# Method to search for ticker by company name
def search_symbol(keyword):
    print(f"Searching for matches related to '{keyword}' …")    # UI print
    url = f"https://query2.finance.yahoo.com/v1/finance/search?q={keyword}" # request URL
    try:
        response = requests.get(url, timeout=10)                # request
        if response.status_code == 429:                         # check for response status
            print("Yahoo Finance rate limit reached. Please wait a minute and try again.")      # UI print
            time.sleep(5)                                       # wait
            return
            
        response.raise_for_status()                             # check if the HTTP response is valid
        results = response.json()
        quotes = results.get("quotes", [])
        if not quotes:                              # check the response
            print("No matches found.")              # UI print
            return
            
        print("The best matches found:")            # UI print
        for q in quotes[:10]:
            symbol = q.get("symbol", "")
            name = q.get("shortname", q.get("longname", ""))
            exch = q.get("exchange", "")
            print(f"{symbol:<10} - {name} ({exch})")    # UI print
    except Exception as e:
        print(f"Error while searching: {e}")        # UI print

# ------------------------------------ end: methods ------------------------------------

if __name__ == "__main__":
    print("=== Yahoo Finance Asset History Downloader (CSV only) ===")

    user_input = input("Enter 'yes' if you want to search for the ticker symbol for an asset (e.g. company name), otherwise you will proceed directly: ").strip()
    if user_input.lower() == "yes":
        asset_name = input("Insert the asset or company name (e.g. Apple, Tesla): ").strip()
        search_symbol(asset_name)

    symbol = input("Insert the ticker symbol (e.g. AAPL, TSLA): ").strip().upper()
    fetch_and_save(symbol)
