package com.intlime.mark.tools;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class CryptTool {
    public static final String KEY = "CjAXDnV3qSi3YR7fX8rPZZgp6m32Mark";
    public static final String IV = "TzqP6cPMlTxaMark";

    public static String encrypt(String text) {
        return encrypt(text, KEY, IV);
    }

    public static String encrypt(String text, String key, String iv) {
        String out = "";
        try {
            byte[] keySpace = new byte[32]; //256 bit key space
            byte[] ivSpace = new byte[16]; //128 bit IV

            byte[] keyByte = key.getBytes("UTF-8");
            int keyLen = keyByte.length; // length of the key	provided
            if (keyLen > keySpace.length) {
                keyLen = keySpace.length;
            }

            byte[] ivByte = iv.getBytes("UTF-8");
            int ivLen = ivByte.length;
            if (ivLen > ivSpace.length) {
                ivLen = ivSpace.length;
            }

            System.arraycopy(keyByte, 0, keySpace, 0, keyLen);
            System.arraycopy(ivByte, 0, ivSpace, 0, ivLen);

            SecretKeySpec keySpec = new SecretKeySpec(keySpace, "AES");

            IvParameterSpec ivSpec = new IvParameterSpec(ivSpace);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);// Initialize this cipher instance
            byte[] results = cipher.doFinal(text.getBytes("UTF-8")); // Finish
            out = Base64.encodeToString(results, Base64.DEFAULT); // ciphertext
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    public static String decrypt(String text) {
        return decrypt(text, KEY, IV);
    }


    public static String decrypt(String text, String key, String iv) {
        String out = "";
        try {
            byte[] keySpace = new byte[32]; //256 bit key space
            byte[] ivSpace = new byte[16]; //128 bit IV

            byte[] keyByte = key.getBytes("UTF-8");
            int keyLen = keyByte.length; // length of the key	provided
            if (keyLen > keySpace.length) {
                keyLen = keySpace.length;
            }

            byte[] ivByte = iv.getBytes("UTF-8");
            int ivLen = ivByte.length;
            if (ivLen > ivSpace.length) {
                ivLen = ivSpace.length;
            }

            System.arraycopy(keyByte, 0, keySpace, 0, keyLen);
            System.arraycopy(ivByte, 0, ivSpace, 0, ivLen);

            SecretKeySpec keySpec = new SecretKeySpec(keySpace, "AES");

            IvParameterSpec ivSpec = new IvParameterSpec(ivSpace);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);// Initialize this ipher instance
            byte[] decodedValue = Base64.decode(text.getBytes(), Base64.DEFAULT);
            byte[] decryptedVal = cipher.doFinal(decodedValue); // Finish
            out = new String(decryptedVal);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    public static String base64Encode(String str) {
        try {
            return Base64.encodeToString(str.getBytes("UTF-8"), Base64.DEFAULT).replaceAll("\n", "");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String base64Decode(String str) {
        try {
            return new String(Base64.decode(str.getBytes("UTF-8"), Base64.DEFAULT), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
