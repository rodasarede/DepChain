# DepChain

## Run Instructions

1. **Run the key script:**
    ```./generate_key_pair.sh
    ```

2. **Build the project:**
    ```sh
    mvn clean install
    ```

3. **Run the application:**
    ```sh
    mvn exec:java -Dexec.mainClass="com.sec.app.BlockchainMember" -Dexec.args="id"
    mvn exec:java -Dexec.mainClass="com.sec.app.ClientApplication" 
    ```

