# Project_LSMSDB_25
Project for the Large Scale and Multi-Structured Data Bases  exam of the AIDE master's degree at the University of Pisa, year 2024-2025.
This project simulates a financial trading platform (MyFuture) developed for the LSMSDB 2025 course.
MongoDB is used as the primary operational database, while Redis is used for caching and fast-access features.


# MongoDB and Redis WSL2 Installation



# Data Ingestion Pipeline
The scripts used in this section are in the folder: '\code\etl\mongo'

Before running any script:
- MongoDB must be running (standalone or replica set)
- These collections must exist: users, assets, asset_prices, transactions, news, counters
- Counters must be initialized: user_id, transaction_id

The ingestion order is designed to avoid logical inconsistencies and ensure correct counter initialization:
1. counters
2. news
3. assets
4. asset_prices
5. users (sets user_id counter correctly)
6. transactions

## Step 0 - Database Initialization

Start MongoDB shell
'''bash
mongosh'''

Create and select database
'''MongoDB shell
use myfuture_lsmsdb_2025'''

Create collections
'''MongoDB shell
db.createCollection("users")
db.createCollection("assets")
db.createCollection("asset_prices")
db.createCollection("transactions")
db.createCollection("news")
db.createCollection("counters")'''

Verify collections
'''MongoDB shell
show collections'''

## Step 1 - Counters Ingestion
Initialize custom counters used for logical IDs. Use the python code: 
'''bash
python load_counters.py'''

Expected result -> counters collection populated with: user_id, and transaction_id.

Verify:
'''MongoDB shell
db.counters.find().pretty()'''

## Step 2 – News Ingestion
Use the python code:
'''bash
python load_news_mongo.py'''

Test in MongoDB:
'''MongoDB shell
use myfuture_lsmsdb_2025
db.news.countDocuments()
db.news.findOne()'''

If you want to see the data entered in abse on the date of ingestion. Ex. of latest 5 ingestions: 
'''MongoDB shell
db.news.find().sort({ ingested_at: -1 }).limit(5)'''

## Step 3 – Assets Ingestion
Use the python code:
'''bash
python load_assets_mongo.py'''

Test in MongoDB:
'''MongoDB shell
db.assets.countDocuments()'''
Count by type:
'''MongoDB shell
db.assets.aggregate([
  { $group: { _id: "$type", count: { $sum: 1 } } }
])'''

See some document filtred by category:
'''MongoDB shell
db.assets.find({ type: "share" }).limit(3).pretty()
db.assets.find({ type: "ETF" }).limit(3).pretty()
db.assets.find({ type: "crypto" }).limit(3).pretty()'''

## Step 4 – Asset Prices Ingestion
Use the python code:
'''bash
python load_asset_prices_mongo.py'''

Test in MongoDB:
'''MongoDB shell
db.asset_prices.countDocuments()
db.asset_prices.findOne()
db.asset_prices.find({ Symbol: "AAPL" }).limit(5)'''

## Step 5 – Users Ingestion
Use the python code:
'''bash
python load_users.py'''

This step are done:
1. reads users from CSV. For user_ids, the IDs in the CSV file are used for consistency and to link to the transaction dataset used.
2. initializes wallets and balances

Test in MongoDB
'''MongoDB shell
db.users.countDocuments()
db.users.findOne()'''

Verify counter:
'''MongoDB shell
db.counters.findOne({ _id: "user_id" })'''

## Step 6 – Transactions Simulation & Ingestion
Use the python code:
'''bash
python validate_trades_dataset.py 		# if you want see statistics of the transaction
python load_transactions.py'''



