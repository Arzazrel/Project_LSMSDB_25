"""
Author: Alessandro Diana
Description:
    Utility script to check consistency between asset list files (anagraphics)
    and actual historical CSV files present in the assets folder.

    The script:
    1. Reads all symbols from crypto, etf, and share lists.
    2. Scans the 'dataset/assets' subdirectories for historical files.
    3. Compares the two sets to find missing or extra assets.
"""

import os
import csv
import glob

# Paths configuration
LISTS_PATH = "dataset/asset_lists/v2"
ASSETS_DATA_PATH = "dataset/assets"

# Files configuration
LIST_FILES = {
    "crypto": ("crypto_list.csv", "symbol"),
    "etf": ("etf_list.csv", "symbol"),
    "share": ("share_list.csv", "Symbol") 
}

# ------------------------------------ start: utils methods ------------------------------------

"""
Automatically detects the delimiter of a CSV file.
"""
def get_csv_delimiter(file_path: str, n_lines: int = 20) -> str:
    with open(file_path, "r", encoding="utf-8") as f:
        sample_lines = "".join([f.readline() for _ in range(n_lines)])
        sniffer = csv.Sniffer()
        dialect = sniffer.sniff(sample_lines)
        delimiter = dialect.delimiter
    return delimiter

# ------------------------------------ end: utils methods ------------------------------------

# ------------------------------------ start: load method ------------------------------------
def check_assets_consistency():
    
    assets_list_symbols = set()          # contains all symbols from the csv files of asset lists
    
    print("--- Asset Lists csv files from: ",LISTS_PATH," ---")             # UI print
    for asset_type, (filename, col_name) in LIST_FILES.items():             # reads all csv files of asset lists
        full_path = os.path.join(LISTS_PATH, filename)
        
        if not os.path.exists(full_path):
            print(f"Warning: List file {full_path} not found. Skipping.")   # UI warning print
            continue
            
        delimiter = get_csv_delimiter(full_path)                            # get delimiter
        
        with open(full_path, mode='r', encoding='utf-8') as f:
            reader = csv.DictReader(f, delimiter=delimiter)                 # read from csv file
            count = 0
            for row in reader:                                              # scan all rows
                symbol = row.get(col_name)                                  # get current symbol
                if symbol:
                    assets_list_symbols.add(symbol.strip().upper())         # add to the list        
                    count += 1
            print(f"Loaded {count} symbols from {filename} ({asset_type})") # UI print

    print(f"Total unique symbols in assets lists: {len(assets_list_symbols)}")  # UI print
    
    # scan all the historical data
    print("\n--- Scanning Historical Data Files ---")
    
    # initial copy of master symbols to track which ones we FIND and remove found items from this set
    found_in_files = set()                              # assets founded in historical data folder
    not_in_asset_list = []                              # assets founded in historical data folder but not in assets lists(assets_list_symbols)
    missing_from_disk = assets_list_symbols.copy()      # containing the list of symbol that aren't in the historical data but are in assets list
    
    # Use glob to find all csv files in all subdirectories of dataset/assets
    search_pattern = os.path.join(ASSETS_DATA_PATH, "**", "*.csv")
    files = glob.glob(search_pattern, recursive=True)
    
    processed_files_count = 0                                   # set counter for processed files
    for file_path in files:                                     # scan each file
        
        filename = os.path.basename(file_path)                  # extract filename (e.g., 'AAPL_historical_daily.csv')
        symbol_part = filename.split('_')[0].strip().upper()    # get the first part representing the symbol (split by '_')
        
        if symbol_part in assets_list_symbols:                  # symbol is valid (is in assets lists)
            if symbol_part in missing_from_disk:                # control check before remove
                missing_from_disk.remove(symbol_part)           # remove from the 'missing' set
                
            found_in_files.add(symbol_part)                     # add into 'found_in_files'
        else:                                                   # symbol found on historical folder but not in assets lists
            not_in_asset_list.append(symbol_part)               # add into 'not_in_asset_list'
            
        processed_files_count += 1                              # update processed files counter

    print(f"Total historical files processed: {processed_files_count}") # UI print
    print("\n" + "="*50)                                                # UI print
    print("RESULTS SUMMARY")                                            # UI print
    print("="*50)                                                       # UI print
    
    # symbols in master list but NO historical file found
    print(f"\n[!] Missing Data: {len(missing_from_disk)} symbols in lists have NO historical file:")    # UI print
    if missing_from_disk:
        print(sorted(list(missing_from_disk)))                                                          # UI print
    else:
        print("None! All listed assets have a corresponding file.")                                     # UI print
        
    # Case B: Files found on disk but NO entry in master list
    print(f"\n[?] Extra Files: {len(not_in_asset_list)} files found on disk are NOT in master lists:")  # UI print
    if not_in_asset_list:
        print(sorted(not_in_asset_list))                                                                # UI print
    else:
        print("None! All files on disk are correctly cataloged.")                                       # UI print

    print("Consistency check completed.")                                                             # UI print
    
# ------------------------------------ end: load method ------------------------------------

if __name__ == "__main__":
    check_assets_consistency()
    
"""
to execute: python -m code.utils.asset_consistency_checker
"""