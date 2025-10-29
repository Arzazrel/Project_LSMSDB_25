"""
Author: Alessandro Diana
Description:
    Tracks real-time (pseudo-realtime) price data for a given asset using yfinance.
    - Prints current price every refresh interval.
    - Saves all collected data to CSV.
    - Optionally plots the price in real-time.
"""

import yfinance as yf
import time
import argparse
import csv
from datetime import datetime, timedelta
import matplotlib.pyplot as plt
import matplotlib.dates as mdates

import warnings
warnings.filterwarnings("ignore", message="Attempting to set identical low and high xlims")

# ------------------------------------ start: methods ------------------------------------

# method to track in real time the price of an asset and show it in command line, plot (optionally) and save in csv (optionally)
def live_tracker(symbol: str, duration_min: float = 5, refresh_sec: float = 1,
                 save_csv: bool = True, show_plot: bool = True):
    print(f"=== Live Tracker for {symbol} ===")                                                                 # UI print
    print(f"Duration: {duration_min} min | Refresh: {refresh_sec}s | CSV: {save_csv} | Plot: {show_plot}\n")    # UI print
          
    end_time = datetime.now() + timedelta(minutes=duration_min)     # calculate the end time for the tracking
    prices = []

    csv_filename = f"{symbol}_realtime_price.csv"           # name of the csv file

    # Initialize CSV
    if save_csv:
        with open(csv_filename, 'w', newline='', encoding='utf-8') as f:
            writer = csv.writer(f)
            writer.writerow(['timestamp', 'price'])         # column name
        print(f"Saving live data to {csv_filename}\n")      # UI print

    # Setup live plot (always initialize if requested)
    if show_plot:
        plt.ion()
        fig, ax = plt.subplots()
        ax.set_title(f"{symbol} - Live Price")
        ax.set_xlabel("Time")
        ax.set_ylabel("Price ($)")
        line, = ax.plot([], [], marker='o', linestyle='-', alpha=0.7)
        ax.xaxis.set_major_formatter(mdates.DateFormatter("%H:%M:%S"))
        plt.xticks(rotation=45, ha='right')
        plt.tight_layout()
        plt.show(block=False)

    # Start tracking loop
    while datetime.now() < end_time:
        try:
            price = yf.Ticker(symbol).fast_info.last_price  # take the last price for the asset
            now = datetime.now()                            # take the current time
            prices.append((now, price))                     # add price and current time to list
            print(f"{now.strftime('%H:%M:%S')}\t{price}")   # UI print
            
            # Save to CSV
            if save_csv:
                with open(csv_filename, 'a', newline='', encoding='utf-8') as f:
                    writer = csv.writer(f)
                    writer.writerow([now.isoformat(), price])

            # Update plot
            if show_plot:
                times, vals = zip(*prices)
                line.set_data(times, vals)

                # Update Y-scale dynamically with 0.5% margin
                vmin, vmax = min(vals), max(vals)
                margin = max(0.005 * vmax, 0.01)  # almeno un margine minimo
                ax.set_ylim(vmin - margin, vmax + margin)
                ax.set_xlim(times[0], times[-1])

                ax.relim()
                ax.autoscale_view()
                ax.figure.canvas.draw()
                ax.figure.canvas.flush_events()

        except Exception as e:
            print(f"Error retrieving data: {e}")

        time.sleep(refresh_sec)                     # sleep to the next tracking time

    print(f"\nFinished tracking {symbol} for {duration_min} minutes.")  # UI print
    if show_plot:
        plt.ioff()
        plt.show()
      
# method to check the symbol
def is_valid_symbol(symbol: str) -> bool:
    try:
        ticker = yf.Ticker(symbol)          # get ticket
        data = ticker.history(period="1d")  # get history informations
        if data.empty:                      # check if the history is empty
            print(f"The symbol '{symbol}' is not valid or has no data (possibly delisted).")
            return False
        return True
        
    except Exception as e:
        print(f"Error checking symbol '{symbol}': {e}")
        return False
        
# Method to search for ticker by company name
def search_symbol(keyword):
    print(f"Searching for matches related to '{keyword}' …")    # UI print
    url = f"https://query2.finance.yahoo.com/v1/finance/search?q={keyword}" # request URL
    try:
        response = requests.get(url, timeout=10)                # request
        if response.status_code == 429:                         # check for response status
            print("Yahoo Finance rate limit reached. Please wait a minute and try again.")      # UI print
            time.sleep(5)                                       # wait
            return
            
        response.raise_for_status()                             # check if the HTTP response is valid
        results = response.json()
        quotes = results.get("quotes", [])
        if not quotes:                              # check the response
            print("No matches found.")              # UI print
            return
            
        print("The best matches found:")            # UI print
        for q in quotes[:10]:
            symbol = q.get("symbol", "")
            name = q.get("shortname", q.get("longname", ""))
            exch = q.get("exchange", "")
            print(f"{symbol:<10} - {name} ({exch})")    # UI print
    except Exception as e:
        print(f"Error while searching: {e}")        # UI print

# ------------------------------------ end: methods ------------------------------------

if __name__ == "__main__":
    # get arguments
    parser = argparse.ArgumentParser(description="Track live price data for an asset using Yahoo Finance.")

    parser.add_argument("--minutes", type=float, default=5, help="Tracking duration in minutes (default: 5)")
    parser.add_argument("--refresh", type=float, default=1, help="Refresh interval in seconds (default: 1)")
    parser.add_argument("--save_csv", type=bool, default=True, help="Save prices to CSV (default: True)")
    parser.add_argument("--show_plot", type=bool, default=True, help="Show live plot (default: True)")
    args = parser.parse_args()
    
    # get the symbol of the asset entered by the user
    user_input = input("Enter 'yes' if you want to search for the ticker symbol for an asset (e.g. company name), otherwise you will proceed directly: ").strip()
    if user_input.lower() == "yes":
        asset_name = input("Insert the asset or company name (e.g. Apple, Tesla): ").strip()
        search_symbol(asset_name)

    symbol = input("Insert the ticker symbol (e.g. AAPL, TSLA): ").strip().upper()
    if is_valid_symbol(symbol):
        # call the method to track and visualize the price
        live_tracker(symbol, args.minutes, args.refresh, args.save_csv, args.show_plot)

"""
Note:
- Examples for the call of the program with different parameters
-- python live_price_tracker.py
-- python live_price_tracker.py --minutes 10 --refresh 2
-- python live_price_tracker.py --save_csv False --show_plot False
"""