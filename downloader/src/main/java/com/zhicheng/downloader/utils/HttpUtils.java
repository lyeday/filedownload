package com.zhicheng.downloader.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author wuzhicheng
 * @version 1.0
 * @date 2020/3/30
 */
public class HttpUtils {

    private static ThreadPoolExecutor mThreadPoolExecutor = new ThreadPoolExecutor(0,8,5, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), Executors.defaultThreadFactory(),new ThreadPoolExecutor.CallerRunsPolicy());

    public interface HeaderResponeCallback{
        void onSuccess(int code,Map<String, List<String>> headers);
        void onError();
    }

    public static HttpURLConnection getHttpConnection(String method, String url, int timeout, Map<String,String> headers){
        try {
            URL httpUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) httpUrl.openConnection();
            connection.setConnectTimeout(timeout);
            connection.setRequestMethod(method);
            connection.setRequestProperty("User-Agent","downloader/1.0");
            if (headers != null){
                for (String key : headers.keySet()) {
                    String value = headers.get(key);
                    connection.setRequestProperty(key,value);
                }
            }
            connection.connect();
            return connection;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void getHttpHeader(final String url, final int timeout, final Map<String,String> headers, final HeaderResponeCallback callback){
        mThreadPoolExecutor.submit(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection httpConnection = getHttpConnection("HEAD", url, timeout, headers);
                if (callback != null){
                    if (httpConnection == null) {
                        callback.onError();
                    }
                    Map<String, List<String>> headerFields = httpConnection.getHeaderFields();
                    try {
                        callback.onSuccess(httpConnection.getResponseCode(),headerFields);
                    } catch (IOException e) {
                        e.printStackTrace();
                        callback.onError();
                    }
                }
                if (httpConnection != null) {
                    httpConnection.disconnect();
                }
            }
        });
    }
}
