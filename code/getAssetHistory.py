"""
Author: Alessandro Diana

Description: 
    Python code to request information (historical one by default) about an asset using the REST API provided by alphavantage. 
    The data obtained is in JSON format. The data is also saved in CSV format. 
    This code can also search for the tag for a given asset name.
"""

import os
import requests
import json
import csv
from datetime import datetime

# ------------------------------------ start: methods ------------------------------------

# Method to Ddownload history for `symbol`, save JSON and CSV.
def fetch_and_save(symbol, api_key, function='TIME_SERIES_DAILY', outputsize='full', datatype='json'):

    base_url = 'https://www.alphavantage.co/query'
    params = {
        'function': function,
        'symbol': symbol,
        'outputsize': outputsize,
        'datatype': datatype,
        'apikey': api_key
    }
    print(f"Requesting data for {symbol} …")            # UI print
    response = requests.get(base_url, params=params)    # request
    response.raise_for_status()                         # check if the HTTP response is valid
    data = response.json()
    
    # -- manage JSON and save it --
    json_filename = f"{symbol}_{function}.json"         # json file name
    with open(json_filename, 'w', encoding='utf-8') as fjson:   # create file
        json.dump(data, fjson, indent=4)                        # save data in the file
    print(f"Saved JSON to {json_filename}")             # UI print
    
    # -- create and save .csv file --
    # Identify the part containing the time series. The key depends on the function. Eg. for TIME_SERIES_DAILY, it is "Time Series (Daily)"
    if function == 'TIME_SERIES_DAILY':
        ts_key = 'Time Series (Daily)'
    elif function == 'TIME_SERIES_WEEKLY':
        ts_key = 'Weekly Time Series'
    elif function == 'TIME_SERIES_MONTHLY':
        ts_key = 'Monthly Time Series'
    elif function == 'TIME_SERIES_INTRADAY':
        interval = params.get('interval', '5min')       # get the frequency (defaul 5 min)
        ts_key = f'Time Series ({interval})'            # example of the key for default case : "Time Series (15min)"
    else:
        ts_key = None               # wrong case
    
    if ts_key is None or ts_key not in data:
        print(f"Warning: expected key '{ts_key}' not found in JSON. Keys are: {list(data.keys())}") # UI print error
        return
    
    timeseries = data[ts_key]       # get the data
    
    csv_filename = f"{symbol}_{function}.csv"           # csv file name
    with open(csv_filename, 'w', newline='', encoding='utf-8') as fcsv:     
        writer = csv.writer(fcsv)
        writer.writerow(['date', 'symbol', 'open', 'high', 'low', 'close', 'volume'])   # write header (column name)
        
        # for each row save the data in the csv file 
        for date_str, daily_data in sorted(timeseries.items()):
            writer.writerow([
                date_str,
                symbol,
                daily_data['1. open'],
                daily_data['2. high'],
                daily_data['3. low'],
                daily_data['4. close'],
                daily_data['5. volume']
            ])
    print(f"Saved CSV to {csv_filename}")               # UI print 
    
# method to get a list of tags matching the searched asset (by name)
def search_symbol(keyword, api_key):
    url = "https://www.alphavantage.co/query"
    params = {"function": "SYMBOL_SEARCH", "keywords": keyword, "apikey": api_key}  # set parameters 
    r = requests.get(url, params=params)                                            # send request
    data = r.json()                             # get data (JSON)
    
    # print the list of tag
    print("The best matches found:")
    for match in data.get("bestMatches", []):
        print(f"{match['1. symbol']} - {match['2. name']}")

# ------------------------------------ end: methods ------------------------------------

# ------------------------------------ main ------------------------------------        
# main function
if __name__ == '__main__':
    YOUR_API_KEY =  # -- INSERT YOUR API KEY --
    if not YOUR_API_KEY:
        YOUR_API_KEY = input("Insert your API key from Alpha Vantage: ").strip()    # get API Key from user
        
    user_input = input("Enter 'yes' if you want to search for the tag for the asset you want to search for, otherwise you will proceed directly to entering the tag: ").strip()
    if user_input.lower() == "yes":
        asset_name = input("Insert the asset name or the company name (es: Apple, Tesla): ").strip().lower()    
        search_symbol(asset_name, YOUR_API_KEY)
    
    symbol = input("Insert the TAG (es: AAPL, TSLA): ").strip().upper()         # get the TAG
    
    fetch_and_save(symbol, YOUR_API_KEY, function='TIME_SERIES_DAILY', outputsize='full')   # get informations and save them
