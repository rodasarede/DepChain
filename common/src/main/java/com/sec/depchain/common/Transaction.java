package com.sec.depchain.common;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.hyperledger.besu.datatypes.Address;
import org.json.JSONObject;
import org.web3j.utils.Numeric;

import com.sec.depchain.common.util.CryptoUtils;

public class Transaction {
    private Address from; // the address of the sender, that will be signing the transaction. This will be
                          // an externally-owned account as contract accounts cannot send transactions
    private Address to; // the receiving address (if an externally-owned account, the transaction will
                        // transfer value. If a contract account, the transaction will execute the
                        // contract code)
    private BigInteger value; // how much ether to transfer
    private String data; // payload (func name, args, etc...); – optional field to include arbitrary data
    private BigInteger nonce; // a sequentially incrementing counter which indicates the transaction number
                              // from the account
    private String signature; // the identifier of the sender. This is generated when the sender's private key
                              // signs the transaction and confirms the sender has authorized this transaction
    private int DEBUG_MODE = 1;

    // https://ethereum.org/en/developers/docs/transactions/

    public Transaction(Address from, Address to, BigInteger value, String data, BigInteger nonce, String signature) {
        this.to = to;
        this.from = from;
        this.value = value;
        this.data = data;
        this.nonce = nonce;
        this.signature = signature;
    }

    public String getData() {
        return data;
    }

    public BigInteger getNonce() {
        return nonce;
    }

    public Address getFrom() {
        return from;
    }

    public Address getTo() {
        return to;
    }

    public String getSignature() {
        return signature;
    }

    public BigInteger getValue() {
        return value;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setNonce(BigInteger nonce) {
        this.nonce = nonce;
    }

    public void setFrom(Address from) {
        this.from = from;
    }

    public void setTo(Address to) {
        this.to = to;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public void setValue(BigInteger value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "from=" + from +
                ", to=" + to +
                ", value=" + value +
                ", data='" + data + '\'' +
                ", nonce=" + nonce +
                ", signature='" + signature + '\'' +
                '}';
    }

    // Method to validate the transaction
    public boolean isValid(Map<Address, AccountState> currentState) {

        if (!CryptoUtils.verifySignature(this)) {
            if (DEBUG_MODE == 1) {
                System.out.println("TRANSACTION - DEBUG: Signature verification failed");
            }
            return false;
        } else {
            if (DEBUG_MODE == 1) {
                System.out.println("TRANSACTION - DEBUG: Signature verification successfull");
            }
        }

        // Check if the sender has enough balance
        AccountState senderState = currentState.get(from);
        if (senderState == null) {
            if (DEBUG_MODE == 1) {
                System.out.println("TRANSACTION - DEBUG: senderState = null");
            }
            return false;
        }
        if (senderState == null || senderState.getBalance().compareTo(getValue()) < 0) {
            if (DEBUG_MODE == 1) {
                System.out.println("TRANSACTION - DEBUG: Insufficient balance");
            }
            return false; // Insufficient balance
        }

        // Check if the receiver exists in the current state
        AccountState receiverState = currentState.get(to);
        if (receiverState == null) {
            if (DEBUG_MODE == 1) {
                System.out.println("TRANSACTION - DEBUG: Receiver does not exist");
            }
            return false; // Receiver does not exist
        }

        BigInteger expectedNonce = senderState.getNonce();
        if (expectedNonce != null) {
            if (getNonce().compareTo(expectedNonce) < 0) {
                if (DEBUG_MODE == 1) {
                    System.out.println("TRANSACTION - DEBUG: Nonce too low (possible replay attack)");
                }
                return false; // Reject replay attacks
            } else if (getNonce().compareTo(expectedNonce) > 0) {
                if (DEBUG_MODE == 1) {
                    System.out.println("TRANSACTION - DEBUG: Nonce too high, out-of-order transaction.");
                }
                // Optionally queue transaction instead of rejecting
                return false;
            }
        }

        // Additional checks if needed(signature verification)

        return true; // Transaction is valid
    }

    public boolean execute(Map<Address, AccountState> currentState,Blockchain blockchain) {
        // Check if the transaction is valid
        if (!isValid(currentState)) {
            return false;
        }

        // Update the state of the sender and receiver
        AccountState senderState = currentState.get(from);
        AccountState receiverState = currentState.get(to);

        // Update sender's balance
        senderState.setBalance(senderState.getBalance().subtract(value));

        // Update receiver's balance
        receiverState.setBalance(receiverState.getBalance().add(value));

        // Add the transaction to the list of transactions
        // transactions.add(this);

        // Block newBlock = new Block(blockchain.getLatestBlock().getBlockHash(), transactions, currentState,
        //         blockchain.getLatestBlock().getHeight() + 1);
        // newBlock.printBlockDetails();
        // blockchain.getChain().add(newBlock);

        return true;
    }

    public byte[] getRawDataForSigning() {
        // Convert all transaction fields to bytes
        byte[] fromBytes = Numeric.hexStringToByteArray(this.from.toHexString());
        byte[] toBytes = Numeric.hexStringToByteArray(this.to.toHexString());
        byte[] valueBytes = this.value.toByteArray();
        byte[] dataBytes = this.data.getBytes(StandardCharsets.UTF_8);
        byte[] nonceBytes = this.nonce.toByteArray();

        // Calculate total length
        int totalLength = fromBytes.length + toBytes.length + valueBytes.length
                + dataBytes.length + nonceBytes.length;

        // Create destination array
        byte[] result = new byte[totalLength];
        int offset = 0;

        // Manual merging
        System.arraycopy(fromBytes, 0, result, offset, fromBytes.length);
        offset += fromBytes.length;
        System.arraycopy(toBytes, 0, result, offset, toBytes.length);
        offset += toBytes.length;
        System.arraycopy(valueBytes, 0, result, offset, valueBytes.length);
        offset += valueBytes.length;
        System.arraycopy(dataBytes, 0, result, offset, dataBytes.length);
        offset += dataBytes.length;
        System.arraycopy(nonceBytes, 0, result, offset, nonceBytes.length);

        return result;
    }

    public JSONObject serializeTransactionToJson() {
        JSONObject jsonTx = new JSONObject();
        jsonTx.put("from", getFrom());
        jsonTx.put("to", getTo());
        jsonTx.put("amount", getValue());
        jsonTx.put("data", getData());
        jsonTx.put("signature", getSignature());
        jsonTx.put("nonce", getNonce());
        // add any other relevant fields
        return jsonTx;
    }

}
