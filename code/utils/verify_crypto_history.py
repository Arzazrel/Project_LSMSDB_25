"""
Author: Alessandro Diana
Description:
    program that takes the list of cryptocurrencies and sorts their symbols so that Yahoo Finance only searches among cryptocurrencies, 
    then checks if historical data is available and, for the available assets, puts them in a CSV file, creating a list of cryptocurrencies 
    for injection into MongoDB.
"""

import pandas as pd
import yfinance as yf
import time
from pathlib import Path

def verify_and_format_crypto():
    input_path = "dataset/asset_lists/v0/top_100_cryptocoin.csv"
    output_path = "dataset/asset_lists/v0/crypto_list_VERIFIED_FINAL.csv"
    
    print(f"-- Start verification of crypto csv file {input_path}...")      # UI print
    
    # read input file
    try:
        df_top = pd.read_csv(input_path)
    except FileNotFoundError:
        print("Error: top_100_cryptocoin.csv not found!")                   # UI print
        return

    verified_assets = []                    # list containing the verified assets

    # 
    for index, row in df_top.iterrows():
        raw_symbol = str(row['Symbol']).strip()
        yahoo_symbol = f"{raw_symbol}-USD"              # force the format for Yahoo Finance
        
        print(f"Verify {yahoo_symbol} ({row['Name']})...", end=" ", flush=True)
        
        try:
            # try downloading a small history file to validate the ticker.
            ticker = yf.Ticker(yahoo_symbol)
            hist = ticker.history(period="5d")
            
            # if there is the data the asset is valid
            if not hist.empty:
                # Creiamo il dizionario con le colonne che servono al tuo DB
                asset = {
                    "symbol": yahoo_symbol,
                    "shortName": row['Symbol'], # Teniamo il ticker originale come short
                    "longName": row['Name'],
                    "currency": "USD",
                    "circulatingSupply": None,  # Non presenti nel top_100 ma richiesti dal DB
                    "maxSupply": None
                }
                verified_assets.append(asset)
                print("is valid")               # UI print
            else:
                print("is not valid")           # UI print
                
        except Exception as e:
            print(f"Error: {e}")
        
        time.sleep(0.2)                         # small pause to avoid to overload yahoo finance 

    # save in the final format
    if verified_assets:
        df_final = pd.DataFrame(verified_assets)
        df_final.to_csv(output_path, index=False)
        print(f"Done. Create file with {len(df_final)} verified crypto in: {output_path}")
    else:
        print("\nThere aren't verified assets.")

if __name__ == "__main__":
    verify_and_format_crypto()