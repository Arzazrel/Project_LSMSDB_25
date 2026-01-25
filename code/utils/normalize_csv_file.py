"""
Author: Alessandro Diana
Description:
- Loads processed CSV asset lists (shares, ETFs, cryptocurrencies)
- Normalizes documents according to the MongoDB asset schema
"""

import csv
import io
import re
from pathlib import Path

# function to normalize an uncorrect formatted file
def normalize_csv_file(input_path, output_path):
    print(f"Starting perfect normalization for {input_path}...")
    
    normalized_rows = []
    
    with open(input_path, 'r', encoding='utf-8') as f:
        # read and clean the header
        header_line = f.readline().strip().rstrip(';')
        header = [h.strip() for h in header_line.split(',')]
        normalized_rows.append(header[:7])                      # check the number of columns (must be 7)
        
        for line in f:
            line = line.strip()
            if not line:
                continue
            
            content = re.sub(r',+$', '', line)          # remove the trailing commas outside the quotation block. Es: "data",,,,,, -> "data"
            
            # handle the specific case of the file: the entire record is enclosed in quotation marks “...” and end with ;"
            if content.startswith('"') and content.endswith(';"'):
                content = content[1:-2]                                     # removethe " and final ;" 
            elif content.startswith('"') and content.endswith('"'):
                content = content[1:-1]
                
            # Cleaning double-double aces (the problem of grouped aces).Transform “” into " until there are only single ones left
            content = content.replace('""', '"')
            
            # Parsing the cleaned line using the CSV module to handle internal commas
            reader = csv.reader(io.StringIO(content), quotechar='"', skipinitialspace=True)
            try:
                row_data = next(reader)
                clean_row = [cell.strip().replace('"', '') for cell in row_data]    # Final cleaning of each cell: we remove extra spaces or residual accents
                
                # Let's only take the necessary fields (7 columns)
                if len(clean_row) >= 7:
                    normalized_rows.append(clean_row[:7])
            except Exception as e:
                print(f"Skipping line due to error: {e}")

    # Final writing in standard CSV format
    with open(output_path, 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f, quoting=csv.QUOTE_MINIMAL)
        writer.writerows(normalized_rows)
        
    print(f"Done! Clean file saved at: {output_path}")

# execute
BASE_PATH = Path("dataset/asset_lists/v2")
normalize_csv_file(BASE_PATH / "share_list.csv", BASE_PATH / "share_list_normalized.csv")