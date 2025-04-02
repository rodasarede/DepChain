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

    public static String formatStateMessage(long ets, TSvaluePair valtsVAl, Set<TSvaluePair> writeSet) {

        JSONObject message = new JSONObject(formatBaseMessage(MessagaTypes.READ, ets));

        // Value-Timestamp pair
        JSONObject valuePair = new JSONObject();
        valuePair.put("value", valtsVAl.getVal() != null ? valtsVAl.getVal() : JSONObject.NULL);
        valuePair.put("timestamp", valtsVAl.getTimestamp());
        message.put("value_pair", valuePair);
        
        // WriteSet as array
        JSONArray writeSetArray = new JSONArray();
        for (TSvaluePair pair : writeSet) {
            JSONObject pairObj = new JSONObject();
            valuePair.put("value", valtsVAl.getVal() != null ? valtsVAl.getVal() : JSONObject.NULL);
            pairObj.put("timestamp", pair.getTimestamp());
            writeSetArray.put(pairObj);
        }
        message.put("write_set", writeSetArray);

        return message.toString();
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
