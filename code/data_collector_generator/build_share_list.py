"""
Author: Alessandro Diana

Description:
- Reads multiple processed CSV datasets containing equity information
- Normalizes fields across different sources
- Merges all data into a single unified CSV file
- Handles missing or empty values safely
- Output is ready for MongoDB ingestion
"""
import csv
import pandas as pd
from pathlib import Path

# -- path --
OUTPUT_FILE = "share_list.csv"

# -- input file names --
sp_files = [                            # files containing American companies
    "SP_400.csv",
    "SP_500.csv",
    "SP_600.csv"
]                       
euro_file = "top_50_euro_company.csv"   # file containing european companies

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
    
    
def clean_value(value):
    """Return None for NaN or empty strings"""
    if pd.isna(value) or str(value).strip() == "":
        return None
    return value

# ------------------------------------ end: utils methods ------------------------------------

# ------------------------------------ start: process method ------------------------------------

def create_final_csv_file():
    
    records = []                # contains the records read from input csv files

    # Process SP datasets
    for file in sp_files:       # scroll al sp files
        print("Check csv delimiter...")         # UI print
        delimiter = get_csv_delimiter(file)     # get delimiter
        print("Reading ",file, " CSV file...")  # UI print
        df = pd.read_csv(file, sep = delimiter, encoding="utf-8")   # read csv file

        for _, row in df.iterrows():
            records.append({
                "Symbol": clean_value(row.get("symbol")),
                "Short Name": clean_value(row.get("Short Name")),
                "Long Name": clean_value(row.get("Long Name")),
                "Type": "share",
                "Sector": clean_value(row.get("GICS Sector")),
                "Industry": clean_value(row.get("GICS Sub-Industry")),
                "Country": clean_value(row.get("Headquarters Location")),
            })

    # Process European dataset
    print("Check csv delimiter...")                 # UI print
    delimiter = get_csv_delimiter(euro_file)        # get delimiter
    print("Reading ",euro_file, " CSV file...")     # UI print
    df_euro = pd.read_csv(euro_file)                # read csv file

    for _, row in df_euro.iterrows():
        records.append({
            "Symbol": clean_value(row.get("symbol")),
            "Short Name": clean_value(row.get("Short Name")),
            "Long Name": clean_value(row.get("Long Name")),
            "Type": "share",
            "Sector": clean_value(row.get("Sector")),
            "Industry": clean_value(row.get("Industry")),
            "Country": clean_value(row.get("Country")),
        })

    # Create final DataFrame
    final_df = pd.DataFrame(records)
    final_df = final_df.dropna(subset=["Symbol"])   # security check, drop rows without symbol

    final_df.to_csv(OUTPUT_FILE, index=False)       # Save CSV

    print(f"Share list created: {OUTPUT_FILE}")     # UI print
    print(f"Total shares: {len(final_df)}")         # UI print

# ------------------------------------ end: process method ------------------------------------

if __name__ == "__main__":
    create_final_csv_file()
