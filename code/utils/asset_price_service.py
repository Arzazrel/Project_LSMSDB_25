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
MARKET_OPEN_TIME = time(15, 30)     # 09:30 EST
MARKET_CLOSE_TIME = time(22, 0)     # 16:00 EST

OPEN_WINDOW_MINUTES = 60            # 1 hour after open
CLOSE_WINDOW_MINUTES = 60           # 1 hour before close

VERBOSE = False                     # if true -> show all error and warning print , if else -> don't show error and warning print

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
    db = get_db()               # get DB connection
    if db is None:              # check DB connection
        if VERBOSE:
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

    db = get_db()               # get DB connection
    if db is None:              # check DB connection
        if VERBOSE:
            print("ERROR [asset_price_service] - DB connection not avaiable")
        return None

    query = {}
    if asset_type:              # check if the asset_type is given
        query["type"] = asset_type

    asset = list(db.assets.aggregate([{"$match": query}, {"$sample": {"size": 1}}]))   # chek all the asset (optionally filtered by asset_type) 
    if not asset:                          # control check for the list of assets
        return None
        
    asset = asset[0]            # take the element from list of dictionary to dictionary

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
def pick_random_price_date(symbol: str, start_date=None):

    db = get_db()               # get DB connection
    if db is None:              # check DB connection
        if VERBOSE:
            print("ERROR [asset_price_service] - DB connection not available")
        return None

    # --- retrieve start_date (earliest available) if not provided ---
    if start_date is None:
        first_doc = db.asset_prices.find(
            {"symbol": symbol},
            {"date": 1}
        ).sort("date", 1).limit(1)

        first_doc = list(first_doc)         # list of the docs with the same date (in the project only one)
        if not first_doc:
            return None

        start_date = first_doc[0]["date"]   # get the start date

    # --- retrieve end_date (most recent available) ---
    last_doc = db.asset_prices.find(
        {"symbol": symbol},
        {"date": 1}
    ).sort("date", -1).limit(1)

    last_doc = list(last_doc)               # list of the docs with the same date (in the project only one)
    if not last_doc:
        if VERBOSE:
            print("ERROR in pick_random_price_date [asset_price_service] - failed retrieve last_doc for end_date")
        return None

    end_date = last_doc[0]["date"]          # get the end date

    pipeline = [
        {"$match": {"symbol": symbol, "date": {"$gte": start_date, "$lte": end_date}}},
        {"$sample": {"size": 1}}
    ]
    random_doc = list(db.asset_prices.aggregate(pipeline))
    if random_doc:
        return random_doc[0]["date"]

    return start_date                       # default return 

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
    
    db = get_db()                   # get DB connection
    if db is None:                  # check DB connection
        if VERBOSE:
            print("ERROR [asset_price_service] - DB connection not avaiable")
        return None

    asset = pick_random_asset(asset_type)           # get the random asset for the transaction
    if asset is None:                               # security check
        if VERBOSE:
            print("ERROR pick_random_asset in get_random_asset_price() in [asset_price_service] - failed selection of asset for transaction")
        return None

    date = pick_random_price_date(asset["symbol"])  # get the random date for the transaction (with hour 00:00)
    if date is None:                                # security check
        if VERBOSE:
            print("ERROR - pick_random_price_date in get_random_asset_price() in [asset_price_service] - failed selection of the date for the transaction for the symbol: ",asset["symbol"])
        return None
        
    price_doc = db.asset_prices.find_one(           # get the document relating to the prices for the chosen asset and date
        {"symbol": asset["symbol"], "date": date}
    )
    if price_doc is None:
        if VERBOSE:
            print("ERROR db.asset_prices.find_one in get_random_asset_price in [asset_price_service] - failed the recovery of the asset price for the transaction")
        return None

    # generate a realistic trade datetime (during stock exchange opening hours) on that date
    trade_time = datetime.combine(
        date.date(),
        random_time_between_market_hours()
    )

    price, source = pick_price_from_candle(price_doc, trade_time)   # get the price per unit for the transaction

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
NOTE 1:
    Performance note – Optimized random date selection for asset prices

    The asset_prices collection contains daily historical price data and can reach tens of millions of documents. 
    Selecting a random date by loading all available dates for a given asset into memory is highly inefficient and does not scale 
    when simulating thousands of transactions.

    This module relies on the following realistic assumption:
    - For a given asset, once historical price collection starts, all intermediate trading days between the first available date
    and the most recent available date are present in the dataset.

    Based on this assumption, the random date selection is optimized as follows:
    1. For each asset symbol, only the earliest and latest available dates are retrieved from the database.
    2. A random date is generated uniformly within this date range (with time set to 00:00, consistent with stored price documents).
    3. The generated date is validated with a query.
    4. If the date is not present (e.g., missing trading day), the process is repeated until a valid date is found.

    This approach:
    - avoids loading large result sets into application memory
    - drastically reduces database load
    - scales efficiently for large transaction simulations
    - remains realistic for financial market datasets

    The trade-off is a minimal number of additional indexed lookups, which is negligible compared to the performance gain obtained.
"""