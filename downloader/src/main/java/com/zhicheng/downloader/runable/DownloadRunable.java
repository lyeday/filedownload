package com.zhicheng.downloader.runable;

import android.os.Parcel;
import android.os.Parcelable;

import com.zhicheng.downloader.utils.HttpUtils;

import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wuzhicheng
 * @version 1.0
 * @date 2020/3/30
 */
public class DownloadRunable implements Runnable, Parcelable {

    public final static int STATUS_IDLE = 0;
    public final static int STATUS_PROGRESS = 1;
    public final static int STATUS_PAUSE = 2;
    public final static int STATUS_RETRY = 3;
    public final static int STATUS_FAILED = -1;

    public interface DataCallback{
        void onData(long start, byte[] bytes);
        void onComplete(DownloadRunable runable);
        void onFaild(DownloadRunable runable, Throwable e);
    }


    private long offset = 0;
    private long start;
    private long end;
    private int status;
    private String url;
    private Map<String,String> headers;
    private int timeout;
    private boolean quit = false;
    private byte[] buffer = new byte[2048];
    private int retryTimes = 0;

    private DataCallback dataCallback;

    public static final Creator<DownloadRunable> CREATOR = new Creator<DownloadRunable>() {
        @Override
        public DownloadRunable createFromParcel(Parcel in) {
            return new DownloadRunable(in);
        }

        @Override
        public DownloadRunable[] newArray(int size) {
            return new DownloadRunable[size];
        }
    };

    public void setDataCallback(DataCallback dataCallback) {
        this.dataCallback = dataCallback;
    }

    public DownloadRunable(long start, long end, String url, int timeout, Map<String, String> headers, DataCallback dataCallback) {
        this.start = start;
        this.end = end;
        this.url = url;
        this.headers = headers;
        this.timeout = timeout;
        this.dataCallback = dataCallback;
    }

    @Override
    public void run() {
        if (quit) return;
        if (start + offset >= end && end > 0){
            dataCallback.onComplete(this);
            return;
        }
        HashMap<String, String> map = new HashMap<>();
        if (headers != null) {
            map.putAll(headers);
        }
        if (end > start) {
            map.put("Range", "bytes=" + (start+offset) + "-" + end);
        }else{
            map.put("Range", "bytes=" + (start+offset) + "-");
        }
        HttpURLConnection connection = HttpUtils.getHttpConnection("GET", url, timeout, map);
        try {
            status = STATUS_PROGRESS;
            int code = connection.getResponseCode();
            if (code == 200 || code == 206){
                InputStream inputStream = connection.getInputStream();
                int len;
                while ((len = inputStream.read(buffer)) != -1){
                    int finalLen = len;
                    if (end > 0) {
                        finalLen = (int) Math.min(len, getSurplus());
                    }
                    byte[] bytes = new byte[finalLen];
                    System.arraycopy(buffer,0,bytes,0,finalLen);
                    dataCallback.onData(start + offset,bytes);
                    offset += len;
                    if (offset+start >= end){
                        break;
                    }
                    if (quit){
                        break;
                    }
                }
                inputStream.close();
                connection.disconnect();
                if (quit){
                    status = STATUS_PAUSE;
                }else {
                    status = STATUS_IDLE;
                    dataCallback.onComplete(this);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (retryTimes < 3){
                retryTimes++;
                status = STATUS_RETRY;
                run();
                return;
            }
            status = STATUS_FAILED;
            dataCallback.onFaild(this,e);
        }
    }

    public long getSurplus(){
        if (end < 0){
            return -1;
        }
        return end - offset - start;
    }
    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getOffset() {
        return offset;
    }

    public int getStatus() {
        return status;
    }

    public void reset(long start, long end){
        this.start = start;
        this.end = end;
        this.offset = 0;
    }


    public void invalid(){
        this.quit = true;
    }

    public void prepare(){
        this.quit = false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(offset);
        dest.writeLong(start);
        dest.writeLong(end);
        dest.writeInt(status);
        dest.writeString(url);
        dest.writeInt(timeout);
        dest.writeByteArray(buffer);
    }



    private DownloadRunable(Parcel in) {
        offset = in.readLong();
        start = in.readLong();
        end = in.readLong();
        status = in.readInt();
        url = in.readString();
        timeout = in.readInt();
        quit = false;
        buffer = in.createByteArray();
        retryTimes = 0;
    }
}
