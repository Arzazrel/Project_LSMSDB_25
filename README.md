# Project_LSMSDB_25
Project for the Large Scale and Multi-Structured Data Bases  exam of the AIDE master's degree at the University of Pisa, year 2024-2025.
This project simulates a financial trading platform (MyFuture) developed for the LSMSDB 2025 course.
MongoDB is used as the primary operational database, while Redis is used for caching and fast-access features.

# Project Structured
The repository is organized into distinct modules to separate data engineering, backend logic, and performance analysis.

- api_specs/: Contains the technical documentation for the REST APIs, including the Postman collection and Swagger/OpenAPI definitions.
  - Images_API/: Screen of the used API.
  - Postman_collection.json: Ready-to-use collection for testing all endpoints (Auth, Assets, Transactions, Analytics).

- code/: The core of the data engineering logic, containing Python scripts for data collection, ETL, and database management.
  - data_collector_generator/: Scripts to fetch real-time data from external APIs (Yahoo Finance, CoinGecko) and generators for synthetic user/trade data.
  - db/: Automation scripts to manage MongoDB indexes (create_indexes.py, drop_all_indexes.py).
  - etl/: The Extract, Transform, Load logic. Contains market_data_feeder.py to populate MongoDB from the datasets.
  - utils/: Shared helper functions for CSV normalization, database connectivity, and consistency checking.
  - test/: Specialized Python tests for benchmarking trading logic and index performance

- dataset/: A collection of raw and processed data (CSV/JSON) used to seed the databases.
  - asset_lists/: Versioned lists of symbols (v0, v1, v2) for different market sectors.
  - assets/: Historical price data and metadata organized by index (S&P 400, 500, 600) and type (Crypto, ETF).
  - news/: Raw news datasets and the cleaned/processed versions ready for ingestion.
  - user/ & user_transaction/: Generated CSV files used for mass population of user profiles and trading history.

- documentation/: Academic and technical reports, presentations, and visual assets explaining the project's architecture and results.
  - documentation_images/: Diagrams showing the database schema, system architecture, and UI mockups.
  - LSMSDB_Project_...pdf: The final technical report detailing the LSMSDB (Large Scale Storage and Management of Data) project implementation.
  - mongodb_indexes_benchmark.txt: Raw results of performance queries before and after indexing.

- spring-boot-backend/: The main Java application providing the backend services and API implementation.
  - myfuture-backend/: The Maven-based Spring Boot project.
  - src/main/java: Contains the Controller-Service-DAO layers, Redis integration logic, and security configurations.
  - pom.xml: Project dependencies including Spring Data MongoDB, Redis, and JWT Security.

- test/: Performance benchmarks and visual results of structural tests.
  - Structural_performance/: Charts and screenshots (PNG) showing the system's response times and throughput.
  - live_tracker/: Logs and visual results of the real-time price tracking system with different update frequencies (1s to 30s).

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
## MongoDB - standalone setup

Since this project uses MongoTemplate without a Replica Set requirement for local development, follow these steps to configure and run a standalone MongoDB instance on WSL2/Linux.

1. Create a Dedicated Data Directory
Create a new directory to isolate this instance's data and ensure the correct permissions are set:

```Bash
# Create the directory
sudo mkdir -p /var/lib/mongo-standalone

# Set ownership to the mongodb user
sudo chown -R mongodb:mongodb /var/lib/mongo-standalone
```
2. Clean Up Existing Locks (Optional)
If MongoDB crashed previously or didn't shut down correctly, you might need to remove the lock file to prevent startup errors:

```Bash
sudo rm -f /var/lib/mongo-standalone/mongod.lock
```
3. Run MongoDB in Standalone Mode
Terminal 1: Start the Daemon Open a WSL2 terminal and run the following command. Note that we bind to 127.0.0.1 for local security and use a custom log path:

```Bash
sudo mongod \
  --port 27017 \
  --dbpath /var/lib/mongo-standalone \
  --bind_ip 127.0.0.1 \
  --logpath /var/log/mongodb-standalone.log \
  --logappend \
  --fork
```
(Note: I added the --fork flag so it runs in the background, but if you prefer to see the logs in real-time, remove --fork and --logpath).

Terminal 2: Verify Connection Open a second terminal to check if the instance is reachable:
```Bash
mongosh --port 27017
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
mongosh "mongodb://localhost:27017,localhost:27018,localhost:27019/myfuture_lsmsdb_2025?replicaSet=rs0&w=majority&readPreference=primary&retryWrites=true"
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

Start redis server cli
```bash
redis-cli -p 6379
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
mongosh "mongodb://localhost:27017,localhost:27018,localhost:27019/myfuture_lsmsdb_2025?replicaSet=rs0&w=majority&readPreference=primary&retryWrites=true"
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
)
```

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

Delete all the news with wrong date.
```MongoDB shell
db.news.deleteMany({ date: null });
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

If you want you can add test asset_prices to test aggregation.
To test 'worst-stable-fell'
```MongoDB shell
var docs = [];
for (var i = 0; i < 7; i++) {
  var date = new Date();
  date.setDate(date.getDate() - i);
  docs.push({
    symbol: "TEST_NEG",
    open: 100,
    close: 95, // -5% costante
    date: date,
    updatedAt: date
  });
}
db.asset_prices.insertMany(docs);
```

To test 'top-stable-raisen'
```MongoDB shell
var docs = [];
for (var i = 0; i < 7; i++) {
  var date = new Date();
  date.setDate(date.getDate() - i);
  docs.push({
    symbol: "TEST_POS",
    open: 100,
    close: 105, // +5% costante
    date: date,
    updatedAt: date
  });
}
db.asset_prices.insertMany(docs);
```

to delete this test asset_prices
```MongoDB shell
db.asset_prices.deleteMany({ symbol: { $in: ["TEST_POS", "TEST_NEG"] } });
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
db.users.findOne({ user_id: user_id })
```

To create an admin user for test (optional)
```MongoDB shell
db.users.insertOne({
  "user_id": NumberLong(9999),            
  "firstName": "Admin",
  "lastName": "Test",
  "email": "admin",                      
  "passwordHash": "$2a$10$BYzsOtlTLfjTozSITofgiuKpcWxtORaidnBDYLbS4BHf3OxjTnViq", // "admin" hash
  "role": "admin",                        // admin role
  "birthDate": new Date(),
  "phone": 333333333,
  "address": "street test, 0",
  "city": "testCity",
  "province": "testville",
  "cap": 00000,
  "registration_date": new Date(),
  "createdAt": new Date(),
  "updatedAt": new Date(),
  "suspended": false,
  "deleted": false
});
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

```Bash
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

# - Enabling Sharding --
To activate the sharding configuration as designed, execute the following commands in mongosh through the mongos router.

## Enable Sharding for the Database
First, the database must be authorized to distribute its data:
```Mongosh
sh.enableSharding("myfuture_lsmsdb_2025");
```

## Configure Shard Keys
Apply the sharding logic to the target collections. Note: The corresponding indexes must exist before running these commands.
Sharding Asset Prices for Symbol and Date (Compound)
```Mongosh
sh.shardCollection("myfuture_lsmsdb_2025.asset_prices", { "symbol": 1, "date": 1 });
```
Sharding Transactions by User ID
```Mongosh
sh.shardCollection("myfuture_lsmsdb_2025.transactions", { "user_id": 1 });
```

## Verification
To monitor the distribution of data chunks and ensure the balancer is working correctly:

```Mongosh
sh.status();
db.printShardingStatus();
```

# Backend Setup and API Documentation
Follow these steps to compile and run the Spring Boot application on WSL2 and access the Swagger documentation from your Windows browser.

1. Build and Run the Application
Navigate to the backend project directory and use the Maven Wrapper to clean and start the service:

```Bash
# Navigate to the backend folder
cd Project_LSMSDB_25/spring-boot-backend/myfuture-backend

# Clean previous builds and compile
./mvnw clean compile

# Run the Spring Boot application
./mvnw spring-boot:run
```
Keep this terminal open. The backend is ready when you see: Started MyfutureBackendApplication in X.XXX seconds

(If you connect by windows through WSL2 Networking) Connect from Windows
Since the backend is running inside WSL2, you need the virtual machine's IP address to access it from a Windows browser.
Find your WSL2 IP: Open a new WSL2 terminal and run:

```Bash
hostname -I
```
Example Output: 172.24.234.210

2. Access Swagger UI
Once you have the IP, open your browser on Windows and navigate to the following URL (replacing <WSL2_IP> with the address found in the previous step):

URL Format: http://<WSL2_IP>:8080/swagger-ui/index.html
Example: http://172.24.234.210:8080/swagger-ui/index.html

Note: Ensure you use http and not https, as the local development server is not configured for SSL.

3. Health Check (Verification)
To verify the service is responding correctly without using a browser, you can run a curl command from a second terminal:

```Bash
# Check the API documentation JSON
curl -I http://localhost:8080/v3/api-docs
```
A successful connection will return a HTTP/1.1 200 OK response.

# API Testing & Quality Assurance (Postman)
This project includes a comprehensive Postman suite to verify functional requirements, security roles, and MongoDB aggregations.

## Setup and Installation
Download Postman Desktop: Required for local testing (WSL2/Localhost). Download here.
Backend Status: Ensure the Spring Boot application is running (http://localhost:8080).
Import Collection: Open Postman > Import > Link.
Paste: http://localhost:8080/v3/api-docs

This automatically generates folders for Users, Customers, and Admins.

Configure Environment: Create a new Environment in Postman with these variables:
baseUrl: http://localhost:8080
admin_user: admin, admin_psw: adminpassword
regular_user: testuser, regular_psw: testuser

## Authentication & User Management
Before testing protected routes, we must ensure users can join the platform.
Test 1: User Registration
Endpoint: POST /api/users/register
Action: Send a JSON body with a new username and password.
Validation: Check for 201 Created.

Test 2: User Login
Endpoint: POST /api/users/login
Action: Use Basic Auth with the credentials created in Step 1.
Validation: Check for 200 OK.

## Core Functionality (Assets & News)
Verify that the "Market" logic works for all users.
Asset Browsing: GET /api/assets (Filter by type or sector).
News Feed: GET /api/news (Filter by category).

Validation: Ensure no critical fields (like price or title) are null

# RUN APPLICATION
Start the MongoDB and Redis instances (as seen above).

Start the system, navigate to the backend project directory and use the Maven Wrapper to clean and start the service:
```Bash
# Navigate to the backend folder
cd Project_LSMSDB_25/spring-boot-backend/myfuture-backend

# Clean previous builds and compile
./mvnw clean compile

# Run the Spring Boot application
./mvnw spring-boot:run
```
If the system starts correctly, it will attempt to enter the data into Redis and you should see the tasks performed with the tag [SCHEDULER] in the terminal.
To check that the system is starting up correctly, you can check the latest news items that have been saved in Redis.
```Redis Bash
ZRANGE news:latest 0 -1
ZRANGE news:latest:sector:Agriculture 0 -1
HGETALL news:699579026bbde3a9b3427a50
```     

Start the live_price_tracker programme to begin collecting asset prices in real time.
```Bash
python -m code.etl.market_data_feeder
```

If you want execute with optional parameters:
```Bash
python -m code.etl.market_data_feeder --refresh 10 --force
```
       
If you want verufy and clean the data from Redis you can test with this commands.
Verify last price for main assets:
```Redis Bash
# Apple
GET asset:AAPL:current_price

# Nvidia
GET asset:NVDA:current_price

# Per Tesla
GET asset:TSLA:current_price
```
        
Verify last 10 price in history for main assets:
```Redis Bash
# Apple
ZRANGE asset:AAPL:intraday_prices -10 -1 WITHSCORES

# Nvidia
ZRANGE asset:NVDA:intraday_prices 0 -1 WITHSCORES

# Tesla
ZRANGE asset:TSLA:intraday_prices 0 -1
```
        
If you want verify the number of the element:
```Redis Bash
ZCARD asset:TSLA:intraday_prices
```

If you want delete the used DB:
```Redis Bash
FLUSHDB
```

If you want delete all the DB on Redis:
```Redis Bash
FLUSHALL
```     

Then start Swagger or Postman to interact with the system.