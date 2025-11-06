"""
Author: Alessandro Diana
Description: read a csv file and print in output the len (without header)
"""

import pandas as pd
import sys
import os

# arguments check
if len(sys.argv) < 2:
    print("Use: python csv_len.py <file_input.csv>")
    sys.exit(1)

input_path = sys.argv[1]
df = pd.read_csv(input_path)
print(len(df))