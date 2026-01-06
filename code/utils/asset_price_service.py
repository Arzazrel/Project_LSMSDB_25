"""
Author: Alessandro Diana
Description:
    Utility module for realistic asset and price selection used during transaction simulation.

    This module provides reusable functions to:
    - select a random financial asset (share, ETF, crypto)
    - select a valid historical date for that asset
    - extract a realistic price from OHLC candle data (open, close, high, low)

    The goal of this step is to ensure temporal and financial consistency when generating simulated trading transactions.
"""

import random
from datetime import datetime
from datetime import timedelta, time
from code.utils.mongoDB_conn import get_db

# Market hours (Italian time, CET/CEST)
MARKET_OPEN_TIME = time(15, 30)   # 09:30 EST
MARKET_CLOSE_TIME = time(22, 0)   # 16:00 EST

OPEN_WINDOW_MINUTES = 60          # 1 hour after open
CLOSE_WINDOW_MINUTES = 60         # 1 hour before close

# ------------------------------------ start: utils methods ------------------------------------

# Returns a random time between market open and close.
def random_time_between_market_hours():

    start = datetime.combine(datetime.today(), MARKET_OPEN_TIME)
    end = datetime.combine(datetime.today(), MARKET_CLOSE_TIME)

    delta_minutes = int((end - start).total_seconds() / 60)
    random_offset = random.randint(0, delta_minutes)

    return (start + timedelta(minutes=random_offset)).time()

"""
    Returns the most recent datetime for which price data is available
    for the given asset symbol.

    Returns None if no data is available.
"""
def get_most_recent_price_date(symbol):
    db = get_db()           # get DB connection
    if db is None:          # check DB connection
        print("ERROR [asset_price_service] - DB connection not avaiable")
        return None
        
    # get the most recent asset_prices for the selected symbol
    prices = db.asset_prices.find(
        {"symbol": symbol},
        {"date": 1}
    ).sort("date", -1).limit(1)

    most_recent = list(prices)
    if not most_recent:                     # control check
        return None

    return most_recent[0]["date"]           # return date

"""
Description: Picks a random asset from the assets collection. Optionally filtered by asset type.

Input:
    - asset_type: indicate the type of asset to choose (optional) 
    
Output:
    - symbol: the identifier dor the assets 
    - assetType: the type of assets (share, ETF, crypto)
    - currency: (default USD)
"""
def pick_random_asset(asset_type=None):

    db = get_db()           # get DB connection
    if db is None:          # check DB connection
        print("ERROR [asset_price_service] - DB connection not avaiable")
        return None

    query = {}
    if asset_type:          # check if the asset_type is given
        query["Type"] = asset_type

    assets = list(db.assets.find(query))    # chek all the asset (optionally filtered by asset_type) 
    if not assets:                          # control check for the list of assets
        return None

    asset = random.choice(assets)           # take one random asset

    # return a dictionary with te parameters
    return {
        "symbol": asset["symbol"],
        "assetType": asset["type"],
        "currency": asset.get("currency", "USD")
    }
    
"""
Description: Picks a random available date for a given asset symbol.

Input:
    - symbol: indicate the asset to choose 
    - start_date: indicates the start date for selecting the date (optional) 
    
Output:
    - date
"""
def pick_random_price_date(symbol: str, start_date = None):

    db = get_db()           # get DB connection
    if db is None:          # check DB connection
        print("ERROR [asset_price_service] - DB connection not avaiable")
        return None

    # create the query (the first parameter)
    query = {"symbol": symbol}

    # if a start_date is provided, select only more recent dates
    if start_date is not None:
        query["date"] = {"$gt": start_date} # add second parameters to the query

    # take the list of asset prices order by date (and 
    prices = list(
        db.asset_prices.find(
            query,
            {"date": 1}
        )
    )

    if not prices:          # control check
        print("ERROR, pick_random_price_date [asset_price_service] - asset prices list for ",symbol, "is null")
        return None

    return random.choice(prices)["date"]    # take and return one random date (with hour 00:00)

"""
Description: Selects a realistic transaction price from an OHLC candle based on trade time.

Input:
    price_doc: MongoDB document containing OHLC fields (Open, High, Low, Close)
    trade_time (optional): specific the time of the transaction. 
                           If trade_time is not provided, a random market time is generated.
                           If trade_time is provided:
                            - Within 1 hour after market open  -> Open price
                            - Within 1 hour before market close -> Close price
                            - Otherwise                         -> random OHLC price

Output:
    price: selected price value
    source: string indicating which OHLC value was used
"""
def pick_price_from_candle(price_doc, trade_time=None):
    
    ohlc = {
        "open": price_doc["open"],
        "close": price_doc["close"],
        "high": price_doc["high"],
        "low": price_doc["low"]
    }

    # Generate random trade time if not provided
    if trade_time is None:
        trade_time = datetime.combine(
            datetime.utcnow().date(),
            random.choice([
                MARKET_OPEN_TIME,
                MARKET_CLOSE_TIME
            ])
        )

    trade_t = trade_time.time()         # take the time from the data

    # -- calculate the time windows -- see NOTE 0
    # calculate the end of the open window
    open_limit = (datetime.combine(datetime.today(), MARKET_OPEN_TIME)
                  + timedelta(minutes=OPEN_WINDOW_MINUTES)).time()

    # calculate the start of the close window
    close_limit = (datetime.combine(datetime.today(), MARKET_CLOSE_TIME)
                   - timedelta(minutes=CLOSE_WINDOW_MINUTES)).time()

    if MARKET_OPEN_TIME <= trade_t <= open_limit:           # transaction made in the open window
        return ohlc["open"], "open"                         # return the open price and 'open'

    if close_limit <= trade_t <= MARKET_CLOSE_TIME:         # transaction made in the close window
        return ohlc["close"], "close"                       # return the close price and 'close'

    mode = random.choice(list(ohlc.keys()))                 # random selection
    return ohlc[mode], mode                                 # return random selection

"""
Description:
    Returns a realistic snapshot of an asset and its transaction price for simulation purposes.

The function:
    - selects a random asset (optionally filtered by type)
    - selects a valid historical date for that asset
    - generates a realistic transaction time within market hours
    - extracts a coherent price from historical OHLC data

Input:
    asset_type (optional): asset category to filter ("share", "ETF", "crypto")

Output:
    dict with:
        - symbol: asset identifier
        - assetType: type of asset
        - date: transaction datetime (date + realistic market time)
        - price: selected price per unit
"""
def get_random_asset_price(asset_type=None):
    
    db = get_db()           # get DB connection
    if db is None:          # check DB connection
        print("ERROR [asset_price_service] - DB connection not avaiable")
        return None

    asset = pick_random_asset(asset_type)   # get the random asset for the transaction
    if asset is None:                       # security check
        print("ERROR [asset_price_service] - failed selection of asset for transaction")
        return None

    date = pick_random_price_date(asset["symbol"])  # get the random date for the transaction (with hour 00:00)
    if date is None:                                # security check
        print("ERROR [asset_price_service] - failed selection of the date for the transaction for the symbol: ",asset["symbol"])
        return None
        
    price_doc = db.asset_prices.find_one(           # get the document relating to the prices for the chosen asset and date
        {"symbol": asset["symbol"], "date": date}
    )
    if price_doc is None:
        print("ERROR [asset_price_service] - failed the recovery of the asset price for the transaction")
        return None

    # generate a realistic trade datetime (during stock exchange opening hours) on that date
    trade_time = datetime.combine(
        date.date(),
        random_time_between_market_hours()
    )

    price, source = pick_price_from_candle(price_doc, date) # get the price per unit for the transaction
    
    # return the usefull information generated for the transaction
    return {
        "symbol": asset["symbol"],
        "assetType": asset["assetType"],
        "date": trade_time,
        "price": price,
    }

# ------------------------------------ end: utils methods ------------------------------------

"""
NOTE 0:
    Time alone does not support sums, but datetime does. Therefore, to compare whether a time is within a certain window from another, 
    I have to attach a date (in this case, today's date).
"""