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
Description: Picks a random asset from the assets collection. Optionally filtered by asset type.

Input:
    asset_type: indicate the type of asset to choose (optional) 
    
Output:
    - symbol: the identifier dor the assets 
    - assetType: the type of assets (share, ETF, crypto)
    - currency: (default USD)
"""
def pick_random_asset(asset_type=None):

    db = get_db()           # get DB connection
    if db is None:          # check DB connection
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
        "symbol": asset["Symbol"],
        "assetType": asset["Type"],
        "currency": asset.get("currency", "USD")
    }
    
# Picks a random available date for a given asset symbol.
def pick_random_price_date(symbol: str):

    db = get_db()           # get DB connection
    if db is None:          # check DB connection
        return None

    # take the list of asset prices order by date
    prices = list(
        db.asset_prices.find(
            {"Symbol": symbol},
            {"Date": 1}
        )
    )

    if not prices:          # control check
        return None

    return random.choice(prices)["Date"]    # take and return one random date

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
        "open": price_doc["Open"],
        "close": price_doc["Close"],
        "high": price_doc["High"],
        "low": price_doc["Low"]
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

    trade_t = trade_time.time()         # take the tiome from the data

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
        return None

    asset = pick_random_asset(asset_type)   # get the random asset for the transaction
    if asset is None:                       # security check
        return None

    date = pick_random_price_date(asset["symbol"])  # get the random date for the transaction
    if date is None:                                # security check
        return None
        
    # generate a realistic trade datetime (during stock exchange opening hours) on that date
    trade_time = datetime.combine(
        date.date(),
        random_time_between_market_hours()
    )

    price_doc = db.asset_prices.find_one(           # get the document relating to the prices for the chosen asset and date
        {"Symbol": asset["symbol"], "Date": date}
    )

    price, source = pick_price_from_candle(price_doc, date) # get the price per unit for the transaction
    
    # return the usefull information generated for the transaction
    return {
        "symbol": asset["symbol"],
        "assetType": asset["assetType"],
        "date": date,
        "price": price,
    }


# ------------------------------------ end: utils methods ------------------------------------

"""
NOTE 0:
    Time alone does not support sums, but datetime does. Therefore, to compare whether a time is within a certain window from another, 
    I have to attach a date (in this case, today's date).
"""