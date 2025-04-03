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
    public static final int TIMEOUT_MS = 10000; //ms 

    private Constants() {

    }

    public class MessageType {
        public static final String READ = "READ";
        public static final String STATE = "STATE";
        public static final String WRITE = "WRITE";
        public static final String ACCEPT = "ACCEPT";
        public static final String COLLECTED = "COLLECTED";
    }
}
