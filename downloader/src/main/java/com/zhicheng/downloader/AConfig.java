package com.zhicheng.downloader;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author wuzhicheng
 * @version 1.0
 * @date 2020/3/27
 */
public class AConfig implements Parcelable {
    private int maxThreads = 8; //最大线程数量
    private int minBlockSize = 102400; //最小分块大小
    private int mTimeout = 30000; //毫秒
    private Map<String,String> headers = new HashMap<>();

    public static final Creator<AConfig> CREATOR = new Creator<AConfig>() {
        @Override
        public AConfig createFromParcel(Parcel in) {
            return new AConfig(in);
        }

        @Override
        public AConfig[] newArray(int size) {
            return new AConfig[size];
        }
    };

    public static AConfig defaultConfig(){
        return new AConfig();
    }

    public AConfig() {
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public int getMinBlockSize() {
        return minBlockSize;
    }

    public void setMinBlockSize(int minBlockSize) {
        this.minBlockSize = minBlockSize;
    }

    public int getTimeout() {
        return mTimeout;
    }

    public void setTimeout(int mTimeout) {
        this.mTimeout = mTimeout;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        if (headers == null) return;
        this.headers = headers;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(maxThreads);
        dest.writeInt(minBlockSize);
        dest.writeInt(mTimeout);
        dest.writeMap(headers);
    }


    protected AConfig(Parcel in) {
        maxThreads = in.readInt();
        minBlockSize = in.readInt();
        mTimeout = in.readInt();
        headers = in.readHashMap(HashMap.class.getClassLoader());
    }
}
