/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ddm.server.common.utils;

import com.ddm.server.common.CommLogD;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 *
 */
public class MessageDigestor {

    public static String md5(String msg) {
        try {
            return md5(msg.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            CommLogD.error(e.getMessage(), e);
            return null;
        }
    }

    public static String md5(byte[] msg) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] buf = md5.digest(msg);
            return ByteUtils.toHexAscii(buf);
        } catch (NoSuchAlgorithmException ex) {
            CommLogD.error("MessageDigestor.md5", ex);
            return null;
        }
    }

    public static String sha256(String msg) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            // byte[] bufPass = passcode.getBytes("UTF-8");
            byte[] bufOther = (msg).getBytes("UTF-8");
            byte[] bufRes = sha.digest(bufOther);
            return ByteUtils.toHexAscii(bufRes);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            CommLogD.error("MessageDigestor.sha256", ex);
            return null;
        }
    }
}
