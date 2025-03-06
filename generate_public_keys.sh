#!/bin/bash

# Directory to store the keys
KEY_DIR="keys"
mkdir -p $KEY_DIR

# Generate 100 key pairs
for i in {1..100}
do
  # Generate private key
  openssl ecparam -genkey -name secp256r1 -out "$KEY_DIR/private_key_$i.pem"

  # Generate public key from private key
  openssl ec -in "$KEY_DIR/private_key_$i.pem" -pubout -out "$KEY_DIR/public_key_$i.pem"

  echo "Generated key pair $i"
done

echo "All key pairs have been generated in the '$KEY_DIR' directory."

cat $KEY_DIR/public_key_*.pem > $KEY_DIR/all_public_keys.pem
echo "Combined all public keys into '$KEY_DIR/all_public_keys.pem'."

echo "All key pairs have been generated in the '$KEY_DIR' directory."
