package com.zhicheng.downloader.mode;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wuzhicheng
 * @version 1.0
 * @date 2020/3/27
 */
public class ContentInfo implements Parcelable{
    private long mContentLength;
    private boolean mAcceptRanges;
    private Map<String, List<String>> mResponeHeaders = new HashMap<>();
    public ContentInfo(Map<String, List<String>> responeHeaders) {
        if (responeHeaders == null){
            return;
        }
        this.mResponeHeaders = responeHeaders;
        for (String s : responeHeaders.keySet()) {
            List<String> list = responeHeaders.get(s);
            if (list.size() > 0) {
                System.out.println(s + "=>" + list.get(0));
            }
        }
        if (responeHeaders.containsKey("Content-Length")){
            this.mContentLength = Long.parseLong(getHeaderStringValue("Content-Length","-1"));
        }else if (responeHeaders.containsKey("Content-Range")){
            String value = getHeaderStringValue("Content-Range", "0/-1");
            String[] split = value.split("/");
            if (split.length>1){
                this.mContentLength = Long.parseLong(split[1]);
            }
        }
        mAcceptRanges = responeHeaders.containsKey("Content-Range") || responeHeaders.containsKey("Accept-Ranges");
    }

    public static final Creator<ContentInfo> CREATOR = new Creator<ContentInfo>() {
        @Override
        public ContentInfo createFromParcel(Parcel in) {
            return new ContentInfo(in);
        }

        @Override
        public ContentInfo[] newArray(int size) {
            return new ContentInfo[size];
        }
    };

    private String getHeaderStringValue(String key, String defaultValue){
        if (!mResponeHeaders.containsKey(key)){
            return defaultValue;
        }
        List<String> list = mResponeHeaders.get(key);
        if (list.size() > 0){
            String s = list.get(0);
            if (s == null) {
                return defaultValue;
            }
            return s;
        }
        return defaultValue;
    }

    public long getContentLength() {
        return mContentLength;
    }

    public boolean isAcceptRanges() {
        return mAcceptRanges && mContentLength > 0;
    }

    public Map<String, List<String>> getResponeHeaders() {
        return mResponeHeaders;
    }


    protected ContentInfo(Parcel in) {
        mContentLength = in.readLong();
        mAcceptRanges = in.readByte() != 0;
        mResponeHeaders = in.readHashMap(HashMap.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mContentLength);
        dest.writeByte((byte) (mAcceptRanges ? 1 : 0));
        dest.writeMap(mResponeHeaders);
    }
}
