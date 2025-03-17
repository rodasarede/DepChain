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