package com.sec.depchain.server;

import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sec.depchain.common.util.Constants.MessagaTypes;
public class MessageFormatter {
    private static String formatBaseMessage(String type, long ets) {
        JSONObject message = new JSONObject();
        message.put("type", type);
        message.put("ets", ets);
        return message.toString();
    }

    public static String formatReadMessage(long ets, int position) {
        JSONObject message = new JSONObject(formatBaseMessage(MessagaTypes.READ, ets));
        message.put("position", position);
        return message.toString();
    }

    public static JSONObject formatStateMessage(long ets, TSvaluePair valtsVal, Set<TSvaluePair> writeSet) {
        JSONObject message = new JSONObject();
        message.put("ets", ets);

        // Value-Timestamp pair
        JSONObject valuePair = new JSONObject();
        valuePair.put("value", valtsVal.getVal() != null ? valtsVal.getVal() : JSONObject.NULL);
        valuePair.put("timestamp", valtsVal.getTimestamp());
        message.put("value_pair", valuePair);
        
        // WriteSet as array
        JSONArray writeSetArray = new JSONArray();
        for (TSvaluePair pair : writeSet) {
            JSONObject pairObj = new JSONObject();
            pairObj.put("value", pair.getVal() != null ? pair.getVal() : JSONObject.NULL);
            pairObj.put("timestamp", pair.getTimestamp());
            writeSetArray.put(pairObj);
        }
        message.put("write_set", writeSetArray);

        return message;
    }

    public static String formatWriteMessage(String value, long ets) {
        JSONObject message = new JSONObject(formatBaseMessage(MessagaTypes.WRITE, ets));
        message.put("value", value);
        return message.toString();
    }

    public static String formatAcceptMessage(String value, long ets) {
        JSONObject message = new JSONObject(formatBaseMessage(MessagaTypes.ACCEPT, ets));
        message.put("value", value);
        return message.toString();
    }
}
