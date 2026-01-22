"""
Author: Alessandro Diana
Description:
    Utility script to remove specific assets from master list CSVs.
    Given a list of symbols, it scans crypto_list.csv, etf_list.csv, and share_list.csv and deletes any row matching those symbols.
"""

import pandas as pd
import os

# -- path --
LISTS_PATH = "dataset/asset_lists/v2"
FILES_TO_CLEAN = [
    "crypto_list.csv",
    "etf_list.csv",
    "share_list.csv"
]

# list of symbols to remove
symbols_to_remove = [
    'AAVE', 'ADA', 'ALGO', 'ASTER', 'AVAX', 'BF.B', 'BFUSD', 'BNSOL', 'BRK.B', 'BUIDL', 
    'CBBTC', 'CWEN.A', 'DAI', 'DANO.PA', 'DOGE', 'EADSY.DE', 'ENA', 'ETC', 'EZETH', 
    'FIGR_HELOC', 'FOUNDED', 'HASH', 'HBAR', 'HTX', 'HYPE', 'ICP', 'JITOSOL', 'JLP', 
    'JUP', 'KAS', 'KCS', 'KHYPE', 'LBTC', 'LSCC', 'LSETH', 'LVMH.PA', 'MNT', 'MOG.A', 
    'NESN.S', 'NOVN.S', 'OKB', 'ONDO', 'OSETH', 'PAXG', 'PENGU', 'PEPE', 'POL', 
    'PYUSD', 'RENDER', 'RETH', 'ROG.S', 'RSETH', 'SAPG.DE', 'SHIB', 'STETH', 
    'SUSDE', 'SUSDS', 'SYRUPUSDC', 'SYRUPUSDT', 'TAO', 'TON', 'TRUMP', 'TTEF.DE', 
    'UBSG.S', 'USD1', 'USDE', 'USDF', 'USDS', 'USDT', 'USDT0', 'USDTB', 'WBETH', 
    'WBNB', 'WBT', 'WBTC', 'WEETH', 'WLD', 'WLFI', 'WSTETH', 'XAUT', 'ZEC', 'ZURN.S'
]

# ------------------------------------ start: utils methods ------------------------------------

def clean_csv_lists():
    # convert the list into a set for faster searches (O(1)). Convert everything to uppercase to avoid case sensitivity issues.
    removal_set = {s.strip().upper() for s in symbols_to_remove}

    print(f"Starting cleaning process for {len(removal_set)} symbols...")   # UI print

    for filename in FILES_TO_CLEAN:                             # scan each input csv files
        file_path = os.path.join(LISTS_PATH, filename)

        if not os.path.exists(file_path):                       # control check
            print(f"File not found: {file_path}. Skipping.")    # UI print
            continue

        try:
            df = pd.read_csv(file_path)                         # read input file

            # identify the correct column (‘symbol’ management vs ‘Symbol’)
            symbol_col = None
            for col in df.columns:
                if col.lower() == 'symbol':
                    symbol_col = col
                    break
            
            if not symbol_col:                                  # control check
                print(f"Could not find a symbol column in {filename}. Skipping.")
                continue

            
            initial_count = len(df)                             # filter: we only keep the lines where the symbol is NOT in the removal set.
            df_cleaned = df[~df[symbol_col].str.strip().str.upper().isin(removal_set)]  # apply the filter
            
            final_count = len(df_cleaned)                       # remaining symbol in the input file
            removed_count = initial_count - final_count         # calculate the number of symbol erased from input file

            df_cleaned.to_csv(file_path, index=False)           # update input file
            
            print(f"Finished {filename}: Removed {removed_count} rows. (Remaining: {final_count})") # UI print

        except Exception as e:
            print(f"Error processing {filename}: {e}")          # UI print

    print("\nCleaning completed successfully.")                 # UI print

# ------------------------------------ end: utils methods ------------------------------------

if __name__ == "__main__":
    clean_csv_lists()