"""
Author: Alessandro Diana
Description:
    A program that, given an array of symbols, generates a CSV file with the bare minimum needed to be read by other programs. 
    Used to search for asset history from a list of asset symbols.
"""

import pandas as pd

# array of symbol to put in a csv file
symbols = ['AAVE', 'ADA', 'ALGO', 'ASTER', 'AVAX', 'BF.B', 'BFUSD', 'BNSOL', 'BRK.B', 'BUIDL', 'CBBTC', 'CWEN.A', 'DAI', 'DANO.PA', 'DOGE', 'EADSY.DE', 'ENA', 'ETC', 'EZETH', 'FIGR_HELOC', 'FOUNDED', 'HASH', 'HBAR', 'HTX', 'HYPE', 'ICP', 'JITOSOL', 'JLP', 'JUP', 'KAS', 'KCS', 'KHYPE', 'LBTC', 'LSCC', 'LSETH', 'LVMH.PA', 'MNT', 'MOG.A', 'NESN.S', 'NOVN.S', 'OKB', 'ONDO', 'OSETH', 'PAXG', 'PENGU', 'PEPE', 'POL', 'PYUSD', 'RENDER', 'RETH', 'ROG.S', 'RSETH', 'SAPG.DE', 'SHIB', 'STETH', 'SUSDE', 'SUSDS', 'SYRUPUSDC', 'SYRUPUSDT', 'TAO', 'TON', 'TRUMP', 'TTEF.DE', 'UBSG.S', 'USD1', 'USDE', 'USDF', 'USDS', 'USDT', 'USDT0', 'USDTB', 'WBETH', 'WBNB', 'WBT', 'WBTC', 'WEETH', 'WLD', 'WLFI', 'WSTETH', 'XAUT', 'ZEC', 'ZURN.S']

# create and populate the csv file
df = pd.DataFrame(symbols, columns=['symbol'])  
df.to_csv('missing_symbols.csv', index=False)