"""
Author: Alessandro Diana
Description:
    Download top cryptocurrencies from CoinGecko and save to CSV.
Parameters:
    - limit (int): Number of cryptocurrencies to download (default 50)
"""

import requests
import pandas as pd
import sys

# ------------------------------------ start: methods ------------------------------------

# method to get and save the crypto coin
def get_top_crypto(limit: int = 50, save_csv: bool = True):
    
    # set the request
    url = "https://api.coingecko.com/api/v3/coins/markets"
    params = {
        "vs_currency": "usd",
        "order": "market_cap_desc",
        "per_page": limit,
        "page": 1,
        "sparkline": False,
    }

    print(f"Download the top {limit} cryptocoin from CoinGecko...") # UI print
    response = requests.get(url, params=params)                     # do request

    if response.status_code != 200:         # response status check
        print(f"Error {response.status_code}: {response.text}")     # UI print
        return

    data = response.json()                  # take response data
    # format the data
    crypto_list = [
        {
            "Rank": i + 1,
            "Symbol": c["symbol"].upper(),
            "Name": c["name"],
            "Price (USD)": round(c["current_price"], 2),
            "Market Cap (USD)": round(c["market_cap"], 2),
        }
        for i, c in enumerate(data)
    ]

    df = pd.DataFrame(crypto_list)          # convert in pandas
    print("The list of the obtained data for the cryptocoin is: \n ",df)    # show the data obtained

    csv_name = "top_" + str(limit) + "_cryptocoin.csv"  # set the csv file name
    df.to_csv(csv_name, index=False)                    # save in csv file
    print("File saved as: ",csv_name)                   # UI print

# ------------------------------------ end: methods ------------------------------------

if __name__ == "__main__":
    
    limit = int(sys.argv[1]) if len(sys.argv) > 1 else 50       # get arguments
    
    get_top_crypto(limit)       # get and save the top crypto coin in csv file
