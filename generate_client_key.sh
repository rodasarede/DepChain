#!/bin/bash

# Configuration
KEY_DIR="common/src/main/java/com/sec/depchain/resources/keys/eth_keys"
mkdir -p "$KEY_DIR"
NUM_KEYS=100

echo "Generating $NUM_KEYS Ethereum private keys..."
echo "-------------------------------------------"

for ((i=1; i<=NUM_KEYS; i++)); do
  # Generate secure Ethereum private key (hex format)
  PRIVATE_KEY="0x$(openssl rand -hex 32 | tr '[:upper:]' '[:lower:]')"
  
  # Save to individual files
  echo "$PRIVATE_KEY" > "$KEY_DIR/private_key_$i.hex"
  echo "Generated private key $i"
done