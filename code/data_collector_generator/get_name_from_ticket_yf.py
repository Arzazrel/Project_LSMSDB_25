"""
Author: Alessandro Diana
Description:
    Code to obtain the name of the company from the ticket.
"""

import yfinance as yf

# ------------------------------------ start: methods ------------------------------------

# method to obtain the company name by a valid symbol (ticket)
def get_name_by_symbol(symbol: str):
    info = yf.Ticker(symbol).info           # get info
    name = info.get('longName') or info.get('shortName') or 'Unknown'
    
    print(f"The symbol: {symbol} is related to '{name}'")    # UI print

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

# ------------------------------------ end: methods ------------------------------------

if __name__ == "__main__":    
    symbol = input("Insert the ticker symbol (e.g. AAPL, TSLA): ").strip().upper()
    if is_valid_symbol(symbol):
        get_name_by_symbol(symbol)

