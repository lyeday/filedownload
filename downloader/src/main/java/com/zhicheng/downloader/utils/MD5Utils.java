package com.zhicheng.downloader.utils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Project: DownloaderDemo
 * ClassName: MD5Utils
 * Date: 2020/3/31 14:00
 * Creator: wuzhicheng
 * Email: voisen@icloud.com
 * Version: 1.0
 * Description:  this is MD5Utils description !
 */
public class MD5Utils {

    public static String getMD5(String msg){
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(msg.getBytes(  "utf-8"));
            byte[] bytes = messageDigest.digest();
            StringBuffer stringBuffer = new StringBuffer();
            for (byte b : bytes) {
                stringBuffer.append(String.format("%02x",b));
            }
            return stringBuffer.toString();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

}
