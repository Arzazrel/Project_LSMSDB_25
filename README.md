# Project_LSMSDB_25
Project for the Large Scale and Multi-Structured Data Bases  exam of the AIDE master's degree at the University of Pisa, year 2024-2025.
This project simulates a financial trading platform (MyFuture) developed for the LSMSDB 2025 course.
MongoDB is used as the primary operational database, while Redis is used for caching and fast-access features.


# MongoDB WSL2 Installation
This guide shows you how to install MongoDB, start a standalone node or a local replica set, and manage nodes and replicas.
Open the terminal and first update Ubuntu.
```bash
sudo apt update && sudo apt upgrade -y
```
Import the MongoDB public key
```bash
wget -qO - https://www.mongodb.org/static/pgp/server-8.0.asc | sudo apt-key add -
```
Add the repository (MongoDB version 8):
```bash
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu $(lsb_release -cs)/mongodb-org/8.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-8.0.list
```
Update repository:
```bash
sudo apt update
```
Instal MongoDB
```bash
sudo apt install -y mongodb-org
```
Start and test MongoDB Standalone. Start MongoDB as a service:
```bash
sudo systemctl start mongod
```
Enable automatic start-up at boot
```bash
sudo systemctl enable mongod
```
Check the status:
```bash
sudo systemctl status mongod
```
Open the MongoDB shell
```bash
mongosh
```
Test the database with the following commands:
```mongosh
use test
db.myCollection.insertOne({ x: 1 })
show collections
db.myCollection.find()
```
Exit from mongosh
```mongosh
exit
```
Stop MongoDB Standalone:
```bash
sudo systemctl stop mongod
```
## MongoDB - Start Local Replica Set
Prepare folders and permissions.
Open a terminal and create directories for the three nodes, ensuring that mongodb has the correct permissions:
```bash
sudo mkdir -p /var/lib/mongo-rs/rs0-1 /var/lib/mongo-rs/rs0-2 /var/lib/mongo-rs/rs0-3
sudo mkdir -p /var/log/mongodb
sudo chown -R mongodb:mongodb /var/lib/mongo-rs
sudo chown -R mongodb:mongodb /var/log/mongodb
```
Starting the three MongoDB nodes (MongoDB must be shut down):
1.	Nodo 1 (PRIMARY)
Open the first terminal and run:
```bash
sudo mongod --replSet rs0 --port 27017 \
  --dbpath /var/lib/mongo-rs/rs0-1 \
  --bind_ip localhost \
  --logpath /var/log/mongodb/rs0-1.log
```
Stay in the foreground, leave this terminal open.
If you want, you can add --fork to run it in the background, but in WSL2 this sometimes causes problems.

2.	Nodo 2 (SECONDARY)
Open a second terminal and run:
```bash
sudo mongod --replSet rs0 --port 27019 \
  --dbpath /var/lib/mongo-rs/rs0-2 \
  --bind_ip localhost \
  --logpath /var/log/mongodb/rs0-2.log
```
Leave this terminal open as well.

3.	Nodo 3 (SECONDARY)
Open a third terminal and run:
```bash
sudo mongod --replSet rs0 --port 27018 \
  --dbpath /var/lib/mongo-rs/rs0-3 \
  --bind_ip localhost \
  --logpath /var/log/mongodb/rs0-3.log
```
Leave this terminal open as well.

## Initialising the Replica Set

Open a fourth terminal to connect to the first node (PRIMARY):
```bash
mongosh "mongodb://localhost:27017,localhost:27018,localhost:27019/?replicaSet=rs0"
```
Inside the MongoDB shell (initialize the replica set):
```mongosh
rs.initiate({
  _id: "rs0",
  members: [
    { _id: 0, host: "localhost:27017" }, // nodo primario
    { _id: 1, host: "localhost:27019" }, // nodo secondario
    { _id: 2, host: "localhost:27018" }  // nodo secondario
  ]
})
```
Check the status of members:
```mongosh
rs.status().members.map(m => ({ name: m.name, stateStr: m.stateStr }))
```
The status should be:
- PRIMARY → 27017
- SECONDARY → 27019 e 27018
Control, you can connect from the terminal to any SECONDARY node to read the data:
mongo --port 27019
mongo --port 27018
Only the PRIMARY node accepts writes. SECONDARY nodes automatically replicate the data.
Connect with the project DB:
```mongosh
use myfuture_lsmsdb_2025
```
Stopping nodes (Connect to each node via mongo shell and stop it).
1.	db.getSiblingDB("admin").shutdownServer()
2.	exit
3.	mongosh "mongodb://localhost:27018"
4.	db.getSiblingDB("admin").shutdownServer()
5.	exit
6.	mongosh "mongodb://localhost:27019"
7.	db.getSiblingDB("admin").shutdownServer()
8.	exit

Or simply close the terminals where the processes are in the foreground with Ctrl+C.

## Future start-up with replica set (without having to redo settings)
MongoDB must not be running. Whenever you want to start the replica set:
1.    Open three terminals and start the three mongod instances as above.
2.    The set will retain the _id: rs0 configuration already registered, so rs.initiate() is not needed again.
3.    Check with:
```mongosh
rs.status().members.map(m => ({ name: m.name, stateStr: m.stateStr }))
```
To close the nodes, use the same procedure as above.

# Redis WSL2 Installation
Update repository:
```bash
sudo apt update && sudo apt upgrade -y
```
Install Redis
```bash
sudo apt install -y redis-server
```
Check the installed version
```bash
redis-server --version
```

## Base configuration
To allow only local connections (default):
```bash
sudo nano /etc/redis/redis.conf
```
If you want to allow connections from any IP address (only for secure local testing):
Replace # bind 127.0.0.1 with # bind 0.0.0.0
Note: After making this change, you must restart Redis.

## Start and test
Start Redis as a service:
```bash
sudo systemctl start redis-server
```
Check service status:
```bash
sudo systemctl status redis-server
```
open Redis client
```bash
redis-cli
```
Base test:
```redis-cli
set testkey "ciao"
get testkey
```
If everything works, get testkey returns hello. Clear the newly created key and close the Redis client. 
```redis-cli
del testkey
exit
```
Stop the service:
```bash
sudo systemctl stop redis-server
```

## Initialising the Replica Set in local (WSL2)
Node		Role		Port
Redis-1		PRIMARY		6379
Redis-2		REPLICA		6380
Redis-3		REPLICA		6381

Writings → on 6379
Critical readings → 6379
Non-critical readings → 6380 / 6381

Create directories for nodes:
```bash
sudo mkdir -p /etc/redis/redis-6379
sudo mkdir -p /etc/redis/redis-6380
sudo mkdir -p /etc/redis/redis-6381
sudo mkdir -p /var/lib/redis-6379 /var/lib/redis-6380 /var/lib/redis-6381
sudo chown -R redis:redis /var/lib/redis-*
```

Configure PRIMARY (port 6379)
```bash
sudo cp /etc/redis/redis.conf /etc/redis/redis-6379/redis.conf
sudo nano /etc/redis/redis-6379/redis.conf
```
Modify/check:

port 6379
bind 127.0.0.1
dir /var/lib/redis-6379

Save and exit.

Configure REPLICA 1 (port 6380)
```bash
sudo cp /etc/redis/redis.conf /etc/redis/redis-6380/redis.conf
sudo nano /etc/redis/redis-6380/redis.conf
```
Modify/check:

port 6380
replicaof 127.0.0.1 6379
dir /var/lib/redis-6380

Save and exit.

Configure REPLICA 2 (port 6381)
```bash
sudo cp /etc/redis/redis.conf /etc/redis/redis-6381/redis.conf
sudo nano /etc/redis/redis-6381/redis.conf
```
Modify/check:

port 6381
replicaof 127.0.0.1 6379
dir /var/lib/redis-6381

Save and exit.

## Initial start-up of nodes (3 terminals)
- Terminal A – PRIMARY
```bash
sudo redis-server /etc/redis/redis-6379/redis.conf
```
Leave open.
- Terminal B – REPLICA 1
```bash
sudo redis-server /etc/redis/redis-6380/redis.conf
```
Leave open.

- Terminal C – REPLICA 2
```bash
sudo redis-server /etc/redis/redis-6381/redis.conf
```
Leave open.
- Terminal D - to check (optionally)
```bash
redis-cli -p 6379
```
```redis-cli
INFO replication
```
You must see:
- role:master
- connected_slaves:2

Quick test, write a key-value and then check in a replica set:
```redis-cli
SET hello world
exit
redis-cli -p 6380
GET hello
```

## future start of replicas
Open three terminals and start the nodes in the same order.
- Terminal 1
```bash
redis-server /etc/redis/redis-6379/redis.conf
```
- Terminal 2
```bash
redis-server /etc/redis/redis-6380/redis.conf
```
- Terminal 3
```bash
redis-server /etc/redis/redis-6381/redis.conf
```
Replicas automatically connect to the primary.
Stop the nodes:
```bash
redis-cli -p 6379 shutdown
redis-cli -p 6380 shutdown
redis-cli -p 6381 shutdown
```
(alternatively) Ctrl + C on each terminal

# - Data Ingestion Pipeline -
The scripts used in this section are in the folder: `\code\etl\mongo`

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

Start MongoDB shell (enter in standalone mode or replica mode, with the command above).
```bash
mongosh
```

Create and select database
```MongoDB shell
use myfuture_lsmsdb_2025
```

Create collections
```MongoDB shell
db.createCollection("users")
db.createCollection("assets")
db.createCollection("asset_prices")
db.createCollection("transactions")
db.createCollection("news")
db.createCollection("counters")
```

Verify collections
```MongoDB shell
show collections
```

If you are using a MongoDB configuration with multiple replicas, ensure that you connect to the primary replica:
```bash
mongosh "mongodb://localhost:27017,localhost:27018,localhost:27019/?replicaSet=rs0"
```
To run the scripts for data ingestion, open the command line in the main project folder and follow the commands below.
The commands for verifying data uploaded to MongoDB all refer to the myfuture_lsmsdb_2025 database.
Therefore, it is essential to be in that database using that command:
```MongoDB shell
use myfuture_lsmsdb_2025
```

## Step 1 - Counters Ingestion
Initialize custom counters used for logical IDs. Use the python code: 
```bash
python -m code.etl.mongo.load_counters
```

Expected result -> counters collection populated with: user_id, and transaction_id.

Verify:
```MongoDB shell
db.counters.find().pretty()
```

If you want reset or modify a counter (ex.):
```MongoDB shell
db.counters.updateOne(
  { _id: "transaction_id" },
  { $set: { seq: 0 } }
)```

## Step 2 – News Ingestion
Use the python code:
```bash
python -m code.etl.mongo.load_news_mongo
```

Test in MongoDB:
```MongoDB shell
db.news.countDocuments()
db.news.findOne()
```

If you want to see the data entered in abse on the date of ingestion. Ex. of latest 5 ingestions: 
```MongoDB shell
db.news.find().sort({ ingested_at: -1 }).limit(5)
```

## Step 3 – Assets Ingestion
Use the python code:
```bash
python -m code.etl.mongo.load_assets_mongo
```

Test in MongoDB:
```MongoDB shell
db.assets.countDocuments()
```
Count by type:
```MongoDB shell
db.assets.aggregate([
  { $group: { _id: "$type", count: { $sum: 1 } } }
])
```

See some document filtred by category:
```MongoDB shell
db.assets.find({ type: "share" }).limit(3).pretty()
db.assets.find({ type: "ETF" }).limit(3).pretty()
db.assets.find({ type: "crypto" }).limit(3).pretty()
```

## Step 4 – Asset Prices Ingestion
Use the python code:
```bash
python -m code.etl.mongo.load_asset_prices_mongo
```

Test in MongoDB:
```MongoDB shell
db.asset_prices.countDocuments()
db.asset_prices.findOne()
db.asset_prices.find({ Symbol: "AAPL" }).limit(5)
```

## Step 5 – Users Ingestion
Use the python code:
```bash
python -m code.etl.mongo.load_users
```

This step are done:
1. reads users from CSV. For user_ids, the IDs in the CSV file are used for consistency and to link to the transaction dataset used.
2. initializes wallets and balances

Test in MongoDB
```MongoDB shell
db.users.countDocuments()
db.users.findOne()
```

Verify counter:
```MongoDB shell
db.users.findOne({ user_id: "user_id" })
```

## Step 6 – Transactions Simulation & Ingestion
Before inserting transactions into the DB, it is recommended to apply the index on asset_prices. The algorithm for creating and inserting transactions inserts into the ‘transactions’ collection and modifies the “users” collection, but makes many queries on the ‘asset_prices’ collection.
The index allows for a significant reduction in time (from approximately 4 days and 12 hours without the index to just one hour with the index).
```MongoDB shell
db.asset_prices.createIndex({ symbol: 1, date: -1 })
```

To verify if the index is applied or not:
```MongoDB shell
db.asset_prices.getIndexes()
```

For start the injection of the transactions use the python code:
```bash
# if you want see statistics of the transaction
python -m code.etl.mongo.validate_trades_dataset	
python -m code.etl.mongo.load_transactions
```

The step:
- simulates realistic buy/sell/deposit transactions
- uses historical prices
- updates user wallets and balances consistently

Test in MongoDB
```MongoDB shell
db.transactions.countDocuments()
db.transactions.findOne()
```

Check user coherence (see some user's field with user_id = 1):
```MongoDB shell
db.users.findOne(
  { user_id: 1 },
  { cash: 1, shareWallet: 1, recentTransactions: 1 }
)
```

# - Create indexes --
Now you can proceed to create the indexes on MongoDB that are useful for the project application.
```bash
python -m code.db.create_indexes
```
If you want to use other indexes that have not been deemed essential or truly effective, or add other indexes for other reasons, simply add them to the code section for optional indexes and run the script  passing the following parameter.
Optional indexes are not necessary for the project and may actually slow down performance, so only use them during testing and with full awareness.
```bash
python -m code.db.create_indexes --extra_index
```

If you want delete all the indexes applied use this script:
```bash
python -m code.db.drop_all_indexes
```

## test indexes
If you want to test the effectiveness of indexes and obtain performance data relating to basic queries with and without indexes, you can use the Python script `code\db\benchmark_indexes`.
This script allows you to run tests and obtain results (including MongoDB explain) for an index, passed as a parameter.
Examples of commands for its operation are shown below:

```MongoDB shell
python -m code.db.benchmark_indexes --list-tests
    
python -m code.db.benchmark_indexes --test asset_prices --write-res 
python -m code.db.benchmark_indexes --test asset_prices --iterations 20 --insert_batch_size 10000
    
python -m code.db.benchmark_indexes --test transactions_user_date
python -m code.db.benchmark_indexes --test transactions_type_date
python -m code.db.benchmark_indexes --test transactions_status_date
    
python -m code.db.benchmark_indexes --test users_email  
    
python -m code.db.benchmark_indexes --test news_date_category
    
python -m code.db.benchmark_indexes --run-all
python -m code.db.benchmark_indexes --run-all --write-res 
```
	
If the tests on the indexes have been completed, I recommend running the programme again to enter all the indexes designed for the project.
```bash
python -m code.db.create_indexes
```