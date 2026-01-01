"""
Author: Alessandro Diana
Description:
    Code for validation and statistical analysis of the transaction dataset.

    This script:
    - Reads the raw transactions dataset (trades.csv)
    - Performs basic validation checks
    - Computes descriptive statistics
    - DOES NOT write anything to MongoDB

    Purpose:
    - Dataset sanity check
    - Produce statistics for project documentation
    
    For each row:
    - verify that accountAgeDays is a valid integer
    - verify that numItems > 0
    - verify that paymentMethod is one of: paypal, creditcard, storecredit

    Globally statistics:
    - total number of transactions
    - number of unique users
    - distribution of payment methods
    - distribution of quantities
    - range of accountAgeDays
    - min/max/avg values of numItems
"""

import csv
from collections import Counter
from statistics import mean

# ---- input path ----
CSV_PATH = "dataset/user_transaction/trades.csv"

VALID_PAYMENT_METHODS = {"paypal", "creditcard", "storecredit"}

# ---- statistics containers ----
total_rows = 0                  # number of rows of the csv file
invalid_rows = 0                # number of invalid rows in the csv file

user_ids = set()                # containing the user_id
payment_methods = Counter()     # counter of occurrency for each payment type
quantities = []                 # cointanining the quantity of each transaction
account_ages = []               # cointanining the account_ages of each transaction

# ---- READ CSV ----
with open(CSV_PATH, newline="", encoding="utf-8") as csvfile:
    reader = csv.DictReader(csvfile)

    for row in reader:
        total_rows += 1

        try:
            user_id = int(row["accountAgeDays"])
            quantity = int(row["numItems"])
            payment = row["paymentMethod"].strip().lower()

            if quantity <= 0:                               # control check
                raise ValueError("Quantity <= 0")       

            if payment not in VALID_PAYMENT_METHODS:        # control check
                raise ValueError("Invalid payment method")

            # ---- update statistics ----
            user_ids.add(user_id)
            payment_methods[payment] += 1
            quantities.append(quantity)
            account_ages.append(user_id)

        except Exception:
            invalid_rows += 1

# ---- PRINT STATISTICS ----
print("\n===== TRANSACTION DATASET VALIDATION =====\n")
print(f"Total rows processed: {total_rows}")
print(f"Valid rows:          {total_rows - invalid_rows}")
print(f"Invalid rows:        {invalid_rows}")

print("\n--- USERS ---")
print(f"Distinct users (accountAgeDays): {len(user_ids)}")
print(f"Min user_id: {min(account_ages)}")
print(f"Max user_id: {max(account_ages)}")

print("\n--- PAYMENT METHODS ---")
for method, count in payment_methods.items():
    print(f"{method}: {count}")

print("\n--- QUANTITIES ---")
print(f"Min quantity: {min(quantities)}")
print(f"Max quantity: {max(quantities)}")
print(f"Average quantity: {mean(quantities):.2f}")

"""

Example of expected output:

===== TRANSACTION DATASET VALIDATION =====
Total rows processed: 39221
Valid rows:          39221
Invalid rows:        0

--- USERS ---
Distinct users (accountAgeDays): 1999
Min user_id: 1
Max user_id: 2000

--- PAYMENT METHODS ---
creditcard: 28004
paypal: 9303
storecredit: 1914

--- QUANTITIES ---
Min quantity: 1
Max quantity: 29
Average quantity: 3.42
"""