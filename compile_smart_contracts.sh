#!/bin/bash

CONTRACTS_BYTECODE_DIR="common/src/main/java/com/sec/depchain/resources/contracts_bytecode"
LAB_DIR="common/src/main/java/com/sec/depchain/resources/lab2Bytecode"
NODE_MODULES_DIR="node_modules/"
mkdir -p $CONTRACTS_BYTECODE_DIR
mkdir -p $LAB_DIR


# Compile the smart contracts
# solc --bin --abi common/src/main/java/com/sec/depchain/common/SmartContractsUtil/BlacklistContract.sol -o $CONTRACTS_BYTECODE_DIR 
# solc --bin-runtime --abi common/src/main/java/com/sec/depchain/common/SmartContractsUtil/ISTcoinContract.sol  --include-path $NODE_MODULES_DIR --base-path . -o $CONTRACTS_BYTECODE_DIR
solc --bin --optimize common/src/main/java/com/sec/depchain/common/SmartContractsUtil/ISTCoin.sol common/src/main/java/com/sec/depchain/common/SmartContractsUtil/Blacklist.sol -o $CONTRACTS_BYTECODE_DIR
# solc --bin --optimize common/src/main/java/com/sec/depchain/common/SmartContractsUtil/helloWorld.sol -o $LAB_DIR