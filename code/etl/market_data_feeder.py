"""
Author: Alessandro Diana
Description:
    Market Data Feeder that bridges MongoDB, Yahoo Finance, and Redis.
    - Reads active asset symbols from MongoDB.
    - Fetches real-time price data using yfinance.
    - Updates Redis with current prices and intraday history.
    - Monitors NYSE/NASDAQ market hours with an optional override for testing.
"""

import yfinance as yf
import time
import argparse
import pytz
from datetime import datetime
from redis import Redis

# import from my codes using absolute package import
from code.utils.mongoDB_conn import get_db

# ------------------------------------ start: utils methods ------------------------------------

"""
Parses command-line arguments for the feeder configuration.
- refresh: seconds between updates (default is 20) - SEE NOTE 0.
- force: boolean to bypass market hour checks (test purpose).
"""
def get_args():
    parser = argparse.ArgumentParser(description="Real-time asset price feeder.")
    parser.add_argument("--refresh", type=int, default=20, help="Refresh interval in seconds (default: 20)")
    parser.add_argument("--force", action="store_true", help="Continue even if the market is closed (testing mode)")
    return parser.parse_args()

"""
Checks if the New York Stock Exchange (NYSE) or NASDAQ (same open time) is currently open. 
Standard hours: 09:30 - 16:00 ET, Monday through Friday.
"""
def is_market_open() -> bool:
    tz_ny = pytz.timezone('America/New_York')       # get NY time zone to avoid error in time conversion
    now_ny = datetime.now(tz_ny)                    # get current hour in NY
    
    # check for weekends (Saturday=5, Sunday=6)
    if now_ny.weekday() >= 5:
        return False
    
    opening_time = now_ny.replace(hour=9, minute=30, second=0, microsecond=0)   # set opening time
    closing_time = now_ny.replace(hour=16, minute=0, second=0, microsecond=0)   # set closing time
    
    return opening_time <= now_ny <= closing_time   # check and return if the market is open or not

# ------------------------------------ end: utils methods ------------------------------------


# ------------------------------------ start: classes ------------------------------------

"""
Handles data flow between MongoDB (input), Yahoo Finance (source), and Redis (output).
"""
class DataBridge:
    
    """
    Initializes database connections.
    """
    def __init__(self, redis_host='localhost', redis_port=6379):
        # MongoDB Connection using central utility
        db = get_db()
        if db is None:
            print("[ERROR] Could not connect to MongoDB. DataBridge initialization failed.")
            exit(1)
            
        # use 'assets' collection to retrieve tickers of active assets
        self.assets_collection = db["assets"]
        
        # Redis Connection (Output for real-time data)
        self.redis_client = Redis(host=redis_host, port=redis_port, decode_responses=True)

    """
    Retrieves the list of active asset symbols currently stored in MongoDB.
    """
    def get_active_symbols(self) -> list:
        cursor = self.assets_collection.find({}, {"symbol": 1, "_id": 0})
        return [doc['symbol'] for doc in cursor if 'symbol' in doc]

    """
    Updates Redis with the latest price information.
    - Sets the current_price String key.
    - Adds a entry to the intraday_prices ZSet with timestamp as score.
    """
    def update_redis_price(self, symbol: str, price: float):
        # Update current price string
        price_key = f"asset:{symbol}:current_price"
        self.redis_client.set(price_key, price)                     # put on redis current price
        
        # Update intraday ZSet history (Score: Timestamp, Member: "Timestamp:Price")
        intraday_key = f"asset:{symbol}:intraday_prices"            # set the key for the asset
        timestamp = int(time.time())                                # get current timestamp used as score in ZSet
        member = f"{timestamp}:{price}"                             # SEE NOTE 1
        self.redis_client.zadd(intraday_key, {member: timestamp})   # put on redis new intraday price

# ------------------------------------ end: classes ------------------------------------


# ------------------------------------ start: main method ------------------------------------

"""
Main execution loop for the market data feeder.
"""
def run_feeder():
    
    args = get_args()                               # get parameters
    bridge = DataBridge()                           # connect to DBs
    
    print("---- MyFuture Market Data Feeder ----")  # UI print
    
    # Market status check
    market_status = is_market_open()
    print(f"NYSE/NASDAQ Market Status: {'OPEN' if market_status else 'CLOSED'}")    # UI print
    # control check
    if not market_status and not args.force:
        print("The market is closed. Closing program. Use --force to run anyway.")  # UI print
        return

    symbols = bridge.get_active_symbols()           # Initial symbol retrieval
    # control check
    if not symbols:
        print("No active assets found in MongoDB. Exiting.")                    # UI print
        return
    
    print(f"Active assets found: {', '.join(symbols)}")                         # UI print
    print(f"Refresh interval: {args.refresh}s | Force mode: {args.force}\n")    # UI print

    try:
        i = 0           # set counter
        # cycle of actions that must continue until the market closes
        while True:
            print(f"- start - the {i} cycle - at {datetime.now().strftime('%H:%M:%S')} -")  # UI print
            i += 1

            # Check market hours during execution unless forced
            if not args.force and not is_market_open():
                print(f"[{datetime.now().strftime('%H:%M:%S')}] Market just closed. Stopping feeder.")  # UI print
                break
                
            tickers_string = " ".join(symbols)      # combine all symbols to performe a single request
            # get data from yfanance, only 1 request for all symbols
            data = yf.download(tickers_string,  
                period="1d",            # load only data of the last minute
                interval="1m",          
                group_by='ticker', 
                threads=True,           # use or not threads internally to be even faster
                progress=False          # hide or show the loading bar
            )

            # scan all founded assets, get current price and put into Redis
            for symbol in symbols:
                
                series = data[symbol]['Close'].dropna() # removes all NaNs from the Close column
                if not series.empty:
                    current_price = series.iloc[-1]     # takes the last REAL existing price
                else:
                    print(f"There aren't correct price {symbol}")
                    continue
                
                bridge.update_redis_price(symbol, current_price)    # update on redis
                
            print(f"- end - the {i} cycle - at {datetime.now().strftime('%H:%M:%S')} -")    # UI print

            #time.sleep(args.refresh)        # wait for the next refresh cycle

    except KeyboardInterrupt:
        print("\nFeeder stopped by user.")              # UI print

# ------------------------------------ end: main method ------------------------------------

if __name__ == "__main__":
    run_feeder()            # run main method
    
"""
NOTE 0:
    I chose 20 seconds because it is the best compromise between a real-time system and the limitations of free tools that do not update data for several seconds 
    and the system load. A higher frequency would lead to many current prices being the same, making the graphs worse and loading the system without providing 
    any additional performance in terms of real time.
    
NOTE 1:
    In Redis Sorted Sets (ZSet), members must be unique. If we used only the price as a member, 
    identical prices at different times would not create new entries; Redis would simply update 
    the score (timestamp) of the existing member. 
    To ensure we store every price point even when the price remains constant, we store a 
    combination of 'timestamp:price' as the member. This guarantees uniqueness for every 
    data point while keeping them sorted by the ZSet score (timestamp).  

NOTE: OPERATIONAL GUIDE FOR MARKET DATA FEEDER
    To run this program, both MongoDB (Replica Set) and Redis (Replica Set) must be active.
    Follow these commands based on the project configuration:

    1. START MONGODB REPLICA SET (3 Terminals)
       Terminal 1 (Port 27017):
       $ sudo mongod --replSet rs0 --port 27017 --dbpath /var/lib/mongo-rs/rs0-1 --bind_ip localhost --logpath /var/log/mongodb/rs0-1.log

       Terminal 2 (Port 27019):
       $ sudo mongod --replSet rs0 --port 27019 --dbpath /var/lib/mongo-rs/rs0-2 --bind_ip localhost --logpath /var/log/mongodb/rs0-2.log

       Terminal 3 (Port 27018):
       $ sudo mongod --replSet rs0 --port 27018 --dbpath /var/lib/mongo-rs/rs0-3 --bind_ip localhost --logpath /var/log/mongodb/rs0-3.log
       
       Terminal 4:
       If you want you can connect to primary replica 
       $ mongosh "mongodb://localhost:27017,localhost:27018,localhost:27019/?replicaSet=rs0"
       
       to close node use:
        1. db.getSiblingDB("admin").shutdownServer()
        2.	exit
        3.	mongosh "mongodb://localhost:27018"
        4.	db.getSiblingDB("admin").shutdownServer()
        5.	exit
        6.	mongosh "mongodb://localhost:27019"
        7.	db.getSiblingDB("admin").shutdownServer()
        8.	exit

    2. START REDIS REPLICA SET (3 Terminals)
       Terminal 1 (Primary - 6379):
       $ sudo redis-server /etc/redis/redis-6379/redis.conf

       Terminal 2 (Replica 1 - 6380):
       $ sudo redis-server /etc/redis/redis-6380/redis.conf

       Terminal 3 (Replica 2 - 6381):
       $ sudo redis-server /etc/redis/redis-6381/redis.conf
       
       Terminal 4:
       If you want you can connect to primary replica 
       $ redis-cli -p 6379
       
       To close all terminal use: exit

    3. RUN THIS PROGRAM
       Open a terminal in the project root directory and execute:
       $ python -m code.etl.market_data_feeder

       Optional parameters:
       $ python -m code.etl.market_data_feeder --refresh 10 --force
       
    4. VERIFY AND CLEAN THE DATA FROM REDIS
       Verify last price for main assets:
        # Apple
        GET asset:AAPL:current_price

        # Nvidia
        GET asset:NVDA:current_price

        # Per Tesla
        GET asset:TSLA:current_price
    
       Verify last 10 price in history for main assets:
        # Apple
        ZRANGE asset:AAPL:intraday_prices -10 -1 WITHSCORES

        # Nvidia
        ZRANGE asset:NVDA:intraday_prices 0 -1 WITHSCORES

        # Tesla
        ZRANGE asset:TSLA:intraday_prices
        
       If you want verify the number of the element:
        ZCARD asset:TSLA:intraday_prices

       If you want delete the used DB:
        FLUSHDB
        
       If you want delete all the DB on Redis:
        FLUSHALL
"""