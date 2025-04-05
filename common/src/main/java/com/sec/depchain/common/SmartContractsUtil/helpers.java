package com.sec.depchain.common.SmartContractsUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONObject;
import org.web3j.utils.Numeric;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class helpers {

    public static String loadBytecode(String path) {
        try {
            //print base path
            // System.out.println(Paths.get(".").toAbsolutePath().normalize().toString());
            return Files.readString(Paths.get(path));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String extractRuntimeBytecode(ByteArrayOutputStream byteArrayOutputStream) {
        try {
            String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
            JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();
            
            
            // If return_data doesn't exist, try to extract from memory using stack values
            String memory = jsonObject.get("memory").getAsString();
            JsonArray stack = jsonObject.get("stack").getAsJsonArray();
            
            if (stack.size() >= 2) {
                int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
                int size = Integer.decode(stack.get(stack.size() - 2).getAsString());
                
                // Make sure to remove the '0x' prefix from memory if it exists
                if (memory.startsWith("0x")) {
                    memory = memory.substring(2);
                }
                
                // Extract the relevant portion of memory based on offset and size
                return memory.substring(offset * 2, offset * 2 + size * 2);
            }
            
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static int extractIntegerFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();

        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
        return Integer.decode(returnData);
    }

    public static Boolean extractBooleanFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();

        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
        if(Integer.decode(returnData) == 1){
            return true;
        }else{
            return false;
        }
    }
    public static String extractErrorMessage(ByteArrayOutputStream byteArrayOutputStream){
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();
        String errorData = jsonObject.get("error").getAsString();
        // Remove the first 4 bytes (8 hex characters) from the error data
        if (errorData.startsWith("0x")) {
            errorData = errorData.substring(10); // Skip "0x" + 8 characters
        } else {
            errorData = errorData.substring(8); // Skip 8 characters
        }
        errorData = new String(hexStringToByteArray(errorData), StandardCharsets.UTF_8);
        // Trim leading and trailing whitespace from the error message
        errorData = errorData.trim();
        return errorData;
    }


    
    public static BigInteger extractBigIntegerFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();

        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
        System.out.println("return data: " + returnData);
        return new BigInteger(returnData, 16); // Use BigInteger to parse very large numbers
    }

    public static long extractLongFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();
    
        String memory = jsonObject.get("memory").getAsString();
    
        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());
    
        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
        return Long.decode("0x" + returnData); // Use Long.decode to parse larger numbers
    }
    
    

    public static String extractStringFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length-1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();

        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size()-1).getAsString());
        int size = Integer.decode(stack.get(stack.size()-2).getAsString());

        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);

        int stringOffset = Integer.decode("0x"+returnData.substring(0, 32 * 2));
        int stringLength = Integer.decode("0x"+returnData.substring(stringOffset * 2, stringOffset * 2 + 32 * 2));
        String hexString = returnData.substring(stringOffset * 2 + 32 * 2, stringOffset * 2 + 32 * 2 + stringLength * 2);

        return new String(hexStringToByteArray(hexString), StandardCharsets.UTF_8);
    }

    public static String convertHexadecimalToAscii(String hexString) {
        if (hexString.startsWith("0x")) {
            hexString = hexString.substring(2);
        }
        return new String(hexStringToByteArray(hexString), StandardCharsets.UTF_8);
    }

    
    public static byte[] hexStringToByteArray(String hexString) {
        int length = hexString.length();
        byte[] byteArray = new byte[length / 2];

        for (int i = 0; i < length; i += 2) {
            int value = Integer.parseInt(hexString.substring(i, i + 2), 16);
            byteArray[i / 2] = (byte) value;
        }

        return byteArray;
    }

    public static String convertIntegerToHex256Bit(int number) {
        BigInteger bigInt = BigInteger.valueOf(number);

        return String.format("%064x", bigInt);
    }
    public static String converStringToHexString(String str) {
        return Numeric.toHexString(str.getBytes());
    }
    public static String convertBigIntegerToHex256Bit(BigInteger number) {
        return String.format("%064x", number);
    }

    public static String padHexStringTo256Bit(String hexString) {
        if (hexString.startsWith("0x")) {
            hexString = hexString.substring(2);
        }

        int length = hexString.length();
        int targetLength = 64;

        if (length >= targetLength) {
            return hexString.substring(0, targetLength);
        }

        return "0".repeat(targetLength - length) +
                hexString;
    }

    public static JSONObject loadJsonFromFile(String filePath) {
        try {
            // Read all bytes from the file and convert to String
            String content = new String(Files.readAllBytes(Paths.get(filePath)));

            // Create and return JSONObject
            return new JSONObject(content);

        } catch (Exception e) {
            e.printStackTrace();
            return null;  
        }
    }
    
}
