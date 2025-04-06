# DepChain

## Run Instructions

1. **Run the key script:**
   ```sh
   ./generate_key_pair.sh
   ```

2. **Build the project:**
   ```sh
   mvn clean install
   ```

3. **Run a server node:**
   ```sh
   cd server
   mvn exec:java -Dexec.args="<server_id>"   
   ```
   
4. **Run a client node:**
   ```sh
   cd client
   mvn exec:java -Dexec.args="<client_id>"    
   ```

5. **Run the entire system**

   #### Option 1: Using VSCode

   1. Step 1 and 2 (generate keys and build the project)
   2. Use the tasks.json file: "ctrl+shift+p" -> "run task" -> "Start all"

   #### Option 2: Not using VSCode

   1. Step 1 and 2 (generate keys and build the project)
   2. Run 4 servers nodes using Step 3 command and start them with id from 1 to 4
   3. Run 2 clients using Step 4 and start them with id 10 and 11


6. **Testing**

   Testing currently can be done by using the Clients and sending appending strings requests.


## Smart Contract Instructions 

1. For linux
   ``` 
   sudo add-apt-repository ppa:ethereum/ethereum
   sudo apt-get update
   sudo apt-get install solc
   ```
2. 
   ```
   npm install @openzeppelin/contracts
   ```
3. 
   '''
   ./compile_smart_contracts.sh
   '''

4. Create/Change Genesis.json with the compiled Bytecode and the wallets addresses generated before.


## DEMO

This section showcases examples of expected behavior in both normal and Byzantine fault scenarios, including clients and servers acting maliciously.

---

### ✅ NORMAL BEHAVIOR

#### • Send a simple transaction
**How to run:**
- Run all four servers.
- Start client 10.
- Send a transaction to client 11 using:
   ```
   > transfer <to> <value> 
   ```

**Expected:**  
The transaction will be included in a block (after 7.5 seconds) and confirmed successfully.

---

#### • Send two transactions within 7.5 seconds
**How to run:**  
Send two transactions with less than 7.5 seconds between them.  
This must be done from two different clients, as each client is blocked while waiting for a response to its request.

**Expected:**  
Both transactions will be included in the same block, since they are close enough in time.

---

### ⚠️ BYZANTINE CLIENTS

#### • Client sends a transaction with insufficient balance
**How to run:**  
Start a client and attempt to send a transaction with an amount greater than the client's balance.

**Expected:**  
The client prints the message:  
`Insufficient balance`.

---

#### • Client tries to impersonate another user
**How to run:**
- Run all four servers.
- Set `SWITCH_FROM_WITH_TO = 1` in the client application.
- Send a normal transaction.

**Expected:**  
The client swaps the `from` and `to` fields when creating the transaction.  
When validating, the signature won't match the `from` address.  
The servers will reject the transaction.  
Expected error: `isValid(blockchain)` returns false.

---

### ⚠️ BYZANTINE SERVERS

#### • Only 2 servers are running (not enough for consensus)
**How to run:**
- Start servers 1 and 2.
- Send a transaction from the client.
- After a delay, start server 3.

**Expected:**  
Initially, nothing happens — the leader doesn't receive enough `SEND` messages (requires `2f + 1`).  
Once server 3 is online, consensus can be reached and the client will receive a response.

---

#### • Server sends multiple `WRITE`s
**How to run:**
- Set `SERVER_2_MULTIPLE_WRITE = 1`.
- Enable `DEBUG_MODE = 1` in the `BlockchainMember`.
- Start all four servers.
- Send a transaction from the client.

**Expected:**  
Server 2 sends five identical `WRITE` messages. Servers 3 and 4 do not send any writes.  
Servers map the writes to the sender's ID and ignore duplicates from the same server.  
As a result, no progress is made.  
You can observe this behavior in the logs when debug mode is enabled.

---

### ⚠️ BYZANTINE LEADER

#### • Leader is down (does not respond)
**How to run:**
- Start servers 2, 3, and 4 (leave server 1 — the leader — turned off).
- Send a transaction from client 10 to client 11.
- Then, turn on server 1.

**Expected:**  
While the leader is off, nothing happens — the client keeps retrying.  
With `DEBUG_MODE = 1` enabled in `PerfectLinks`, you can observe the retries.  
Once server 1 is turned on, consensus is achieved and the transaction completes.

---

#### • Leader tries to modify the transaction amount
**How to run:**
- Set `LEADER_VALUE_TEST = 1`.
- Run all four servers.
- Send a valid transaction from the client.

**Expected:**  
During the `COLLECT` phase, the leader alters the transaction’s amount.  
The transaction signature becomes invalid and fails verification. 
Expected message in server logs:  
BEP - ERROR: There is one transaction that is not real!



