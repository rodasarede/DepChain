package com.sec.depchain.common.util;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class KeyDistribution {
    private static Map<String, PublicKey> _public_keys = new HashMap<>();

    public static void addPublicKey(String id, PublicKey publicKey) {
        _public_keys.put(id, publicKey);
    }

    public static PublicKey getPublicKey(String id) {
        return _public_keys.get(id);
    }
}
