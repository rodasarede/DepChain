package com.sec.depchain.common.util;

public final class Constants {
    public static final String UNDEFINED = "UNDEFINED";
    public static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    public static final String SIGNATURE_ALGORITHM_SHA256_ECDSA = "SHA256withECDSA";
    public static final String EC = "EC";
    public static final String PROPERTIES_PATH = "../common/src/main/java/com/sec/depchain/resources/system_membership.properties";
    public static final String KEY_DIR = "../common/src/main/java/com/sec/depchain/resources/keys";
    public static final String UNKNOWN = "UNKNOWN";
    public static final int THRESHOLD = 2;
    public static final String GENESIS_BLOCK_FILE = "../common/src/main/java/com/sec/depchain/common/SmartContractsUtil/Genesis.json";
    public static final int TIMEOUT_MS = 7500; // ms
    public static final String CALLS_FILE = "../common/src/main/java/com/sec/depchain/common/SmartContractsUtil/hashedCalls.json";
    public static final String SHA_256 = "SHA-256";

    private Constants() {

    }

    public class MessageType {
        public static final String READ = "READ";
        public static final String STATE = "STATE";
        public static final String WRITE = "WRITE";
        public static final String ACCEPT = "ACCEPT";
        public static final String COLLECTED = "COLLECTED";
        public static final String TX_REQUEST = "tx-request";
        public static final String TX_RESPONSE = "tx-response";
        public static final String ACK = "ACK";
        public static final String NORMAL = "NORMAL";

 }

    public class ExecType{
        public static final String TRANSFER = "transfer";
        public static final String APPROVE = "approve";
        public static final String TRANSFERFROM = "transferfrom";
        public static final String BALANCEOF = "balanceof";
        public static final String ISBLACKEDLISTED = "isblacklisted";
        public static final String ADDTOBLACKLIST = "addtoblacklist";
        public static final String REMOVEFROMBLACKLIST = "removefromblacklist";

    }
}
