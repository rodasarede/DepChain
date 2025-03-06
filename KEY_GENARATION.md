# Generate N key pairs
for i in {1..100}
do
  openssl ecparam -genkey -name secp256r1 -out private_key_$i.pem
  openssl ec -in private_key_$i.pem -pubout -out public_key_$i.pem
done

# Assign a unique key pair to each node.


# Distribute Public Keys
cat public_key_*.pem > all_public_keys.pem

Each node should have access to its own private key and all public keys 


