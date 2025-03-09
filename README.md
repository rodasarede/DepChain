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