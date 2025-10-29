"""
Author: Alessandro Diana

Description: Python code read CSV with ';' separator and save with ','
"""

import pandas as pd
import sys
import os

# arguments check
if len(sys.argv) < 2:
    print("Use: python convert_csv_separator.py <file_input.csv>")
    sys.exit(1)

input_path = sys.argv[1]
output_path = os.path.splitext(input_path)[0] + "_converted.csv"

# read CSV with ';' separator and save with ','
df = pd.read_csv(input_path, sep=';')
df.to_csv(output_path, index=False)

print(f"Converted file saved as: {output_path}")
