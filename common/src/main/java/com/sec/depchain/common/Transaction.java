package com.sec.depchain.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.EVM;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.json.JSONObject;
import org.web3j.utils.Numeric;

import com.sec.depchain.common.SmartContractsUtil.helpers;
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

    private boolean success;

    private int clientId = 0;
    // https://ethereum.org/en/developers/docs/transactions/ 

    public Transaction(Address from, Address to, BigInteger value, String data, BigInteger nonce, String signature) {
        this.to = to;
        this.from = from;
        this.value = value;
        this.data = data;
        this.nonce = nonce;
        this.signature = signature;
        this.success = false;
    }

    public Transaction(Address from, Address to, BigInteger value, String data, BigInteger nonce, String signature, int clientId) {
        this.to = to;
        this.from = from;
        this.value = value;
        this.data = data;
        this.nonce = nonce;
        this.signature = signature;
        this.clientId = clientId;
        this.success = false;
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
    public boolean isValid(Blockchain blockchain) {
        MutableAccount senderState = (MutableAccount) blockchain.getSimpleWorld().get(from);
        BigInteger expectedNonce = BigInteger.valueOf(senderState.getNonce());
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

        senderState.incrementNonce();
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
       

        if (senderState == null) {
            if (DEBUG_MODE == 1) {
                System.out.println("TRANSACTION - DEBUG: senderState = null");
            }
            return false;
        }
        /*if (senderState.getBalance().compareTo(UInt256.valueOf(getValue())) < 0) {
            if (DEBUG_MODE == 1) {
                System.out.println("TRANSACTION - DEBUG: Insufficient balance");
            }
            return false; // Insufficient balance
        }*/

        // Check if the receiver exists in the current state
        MutableAccount receiverState = (MutableAccount) blockchain.getSimpleWorld().get(to);
        if (receiverState == null) {
            if (DEBUG_MODE == 1) {
                System.out.println("TRANSACTION - DEBUG: Receiver does not exist");
            }
            return false; // Receiver does not exist
        }

    
        // Additional checks if needed(signature verification)

        return true; // Transaction is valid
    }

    public void execute(Blockchain blockchain) {

        // Check if the transaction is valid
        /*if (!isValid(blockchain)) {
            return;
        }*/
       

        // Update the state of the sender and receiver
        MutableAccount senderState = (MutableAccount) blockchain.getSimpleWorld().get(from);
        MutableAccount receiverState = (MutableAccount) blockchain.getSimpleWorld().get(to);
        if (senderState.getBalance().compareTo(UInt256.valueOf(getValue())) < 0) {
            if (DEBUG_MODE == 1) {
                System.out.println("TRANSACTION - DEBUG: Insufficient balance");
            }
            return; // Insufficient balance
        }
        // attention: receiverState.getCode() is not null for any account it has always
        // at least"0x"
        if (receiverState.getCode().bitLength() > 0 && getData() != null) {
            System.out.println("Executing smart contract");
            // System.out.println("from:" + from);
            // System.out.println("code:" + receiverState.getCode());
            System.out.println("data:" + getData());

            blockchain.getExecutor();
            ByteArrayOutputStream byteArrayOutputStream = blockchain.getbyteArrayOutputStream();

            // execute as a smart contract call
            // blockchain.getExecutor().code(Bytes.fromHexString(receiverState.getCode()));
            blockchain.getExecutor().callData(Bytes.fromHexString(getData()));
            blockchain.getExecutor().sender(from);
            blockchain.getExecutor().execute();
            Boolean result = helpers.extractBooleanFromReturnData(byteArrayOutputStream);
            System.out.println("Output of 'execution': " + result);

            // hardcoded to check if balance updated for example: transfer
            // 0x1234567891234567891234567891234567891234
            // 0x336f5f589a81811b47d582d4853af252bfb7c5e2 10
            blockchain.getExecutor().callData(Bytes.fromHexString(
                    "70a08231" + helpers.padHexStringTo256Bit("0x336f5f589a81811b47d582d4853af252bfb7c5e2")));
            blockchain.getExecutor().execute();
            Long balanceOfReceiver = helpers.extractLongFromReturnData(byteArrayOutputStream);
            System.out.println("Output of 'balanceOf(336f5f)': " + Long.toString(balanceOfReceiver));

            if (result != true) {
                return;
            }

        } else {
            // execute as a normal native transfer
            // Update sender's balance
            senderState.setBalance(senderState.getBalance().subtract(UInt256.valueOf(value)));

            // Update receiver's balance
            receiverState.setBalance(receiverState.getBalance().add(UInt256.valueOf(value)));
        }

        setSuccess(true);
        //senderState.incrementNonce();
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

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String computeTxHash() {
        try {
            // Combine all fields that define transaction uniqueness
            String txData = from.toString() +
                    to.toString() +
                    value.toString() +
                    data +
                    nonce.toString() +
                    signature + Boolean.toString(success);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(txData.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    public int getClientId() {
        return clientId;
    }
    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

}
