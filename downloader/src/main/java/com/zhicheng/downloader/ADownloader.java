package com.zhicheng.downloader;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.zhicheng.downloader.mode.ContentInfo;
import com.zhicheng.downloader.runable.DownloadRunable;
import com.zhicheng.downloader.utils.HttpUtils;
import com.zhicheng.downloader.utils.MD5Utils;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author wuzhicheng
 * @version 1.0
 * @date 2020/3/27
 */
public class ADownloader implements DownloadRunable.DataCallback, Parcelable {

    private static final String TAG = "ADownloader";

    public final static int STATUS_FAILED = -1;
    public final static int STATUS_PARSE = 100;
    public final static int STATUS_START = 101;
    public final static int STATUS_PAUSE = 102;
    public final static int STATUS_COMPLETE = 103;

    private File mFile;
    private File mTempFile;
    private RandomAccessFile mRandomAccessFile;
    private String mUrl;
    private AConfig mConfig = AConfig.defaultConfig();
    private int mDownloadStatus;
    private ContentInfo mContentInfo;
    private long mTimestamp;
    private long mSpeed = 0;
    private long mBytesSecond = 0;
    private long mCompleteBytes = 0;
    private IADownloadListener mDownloadListener;

    private ThreadPoolExecutor mThreadPoolExecutor;

    private List<DownloadRunable> mProgressInfo = Collections.synchronizedList(new ArrayList<DownloadRunable>());

    RejectedExecutionHandler mHandler = new ThreadPoolExecutor.CallerRunsPolicy();

    public ADownloader(File file, String url) {
        this.mFile = file;
        this.mUrl = url;
        initDownloader();
    }

    public ADownloader(File file, String url, AConfig config) {
        this.mFile = file;
        this.mUrl = url;
        this.mConfig = config;
        initDownloader();
    }

    private void initDownloader() {
        mThreadPoolExecutor = new ThreadPoolExecutor(1,
                mConfig.getMaxThreads(),
                20,
                TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                Executors.defaultThreadFactory(),
                mHandler);
        mTempFile = new File(mFile.getParent(),mFile.getName()+".tmp");
        try {
            mRandomAccessFile = new RandomAccessFile(mTempFile,"rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始下载
     */
    public synchronized void start(){
        if (getDownloadStatus() == STATUS_PARSE ||
                getDownloadStatus() == STATUS_START) {
            Log.w(TAG, "start: 下载正在进行中...");
            return;
        }

        if (getDownloadStatus() == STATUS_COMPLETE){
            setDownloadStatus(STATUS_COMPLETE);
            return;
        }

        if (mFile == null) {
            setDownloadStatus(STATUS_FAILED);
            Log.e(TAG, "start: 下载目标文件不能为空.");
            return;
        }

        if (getDownloadStatus() == STATUS_PAUSE || (mContentInfo != null && getDownloadStatus() == STATUS_FAILED)){
            resumeTask();
            return;
        }

        setDownloadStatus(STATUS_PARSE);
        //1.分析文件
        Map<String,String> headers = mConfig.getHeaders();
        if (headers == null){
            headers = new HashMap<>(1);
        }
        headers.put("Range","bytes=0-");
        HttpUtils.getHttpHeader(this.mUrl, mConfig.getTimeout(), headers, new HttpUtils.HeaderResponeCallback() {
            @Override
            public void onSuccess(int code, Map<String, List<String>> headers) {
                if (code == 200 || code == 206){
                    mContentInfo = new ContentInfo(headers);
                    //创建任务
                    System.out.println("创建任务...:"+mContentInfo.getContentLength());
                    if (mContentInfo.getContentLength() > 0){
                        try {
                            mRandomAccessFile.setLength(mContentInfo.getContentLength());
                            mTimestamp = System.currentTimeMillis();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    createDownloadTask();
                }else{
                    setDownloadStatus(STATUS_FAILED);
                }
            }
            @Override
            public void onError() {
                setDownloadStatus(STATUS_FAILED);
            }
        });
    }

    /**
     * 创建任务
     */
    private void resumeTask() {
        setDownloadStatus(STATUS_START);
        for (DownloadRunable runable : mProgressInfo) {
            runable.prepare();
            mThreadPoolExecutor.submit(runable);
        }
    }

    /**
     * 解析任务
     */
    private void createDownloadTask() {
        Log.i(TAG, "createDownloadTask: 开始任务:"+mContentInfo.getContentLength());
        mProgressInfo.clear();
        setDownloadStatus(STATUS_START);
        if (!mContentInfo.isAcceptRanges()){
            DownloadRunable info = new DownloadRunable(0,-1,mUrl,mConfig.getTimeout(),mConfig.getHeaders(),this);
            mProgressInfo.add(info);
            mThreadPoolExecutor.submit(info);
            return;
        }
        //先根据文件块总大小进行平分
        long contentLength = mContentInfo.getContentLength();
        if (contentLength > mConfig.getMinBlockSize()){
            //计算分块
            //每个线程负责的大小
            long blockSize = (long) (contentLength / mConfig.getMaxThreads() + 0.5);
            if (blockSize < mConfig.getMinBlockSize()){
                blockSize = mConfig.getMinBlockSize();
            }
            //分块
            long start = 0;
            while (start < contentLength){
                long s,e;
                long end = start + blockSize;
                s = start;
                start = end;
                //剩余
                long last = contentLength - start;
                if (last < mConfig.getMinBlockSize() ||
                        end > contentLength){
                    e = contentLength;
                    start = contentLength;
                }else{
                    e = end;
                }
                DownloadRunable info = new DownloadRunable(s,e,mUrl,mConfig.getTimeout(),mConfig.getHeaders(),this);
                mProgressInfo.add(info);
                mThreadPoolExecutor.submit(info);
            }
        }
    }


    /**
     * 分割数据
     */
    private synchronized void divisionTask(){
        if (getDownloadStatus() == STATUS_PAUSE){
            return;
        }
        DownloadRunable max = null;
        DownloadRunable runable = null;
        for (DownloadRunable r : mProgressInfo) {
            long surplus = r.getSurplus();
            switch (r.getStatus()){
                case DownloadRunable.STATUS_IDLE:
                    if (runable == null){
                        runable = r;
                    }
                    break;
                case DownloadRunable.STATUS_PROGRESS:
                    if (surplus > mConfig.getMinBlockSize() * 2){
                        if (max == null){
                            max = r;
                        }else{
                            if (surplus > max.getSurplus()){
                                max = r;
                            }
                        }
                    }
                    break;
                case DownloadRunable.STATUS_FAILED:
                    break;
            }

        }
        if (max == null || runable == null) return;
        long l = max.getSurplus() / 2;
        long maxEnd = max.getEnd();
        long newStart = maxEnd - l;
        max.setEnd(newStart);
        runable.reset(newStart,maxEnd);
        System.out.println("分割数据:"+(maxEnd - newStart));
        //重新提交任务
        mThreadPoolExecutor.submit(runable);
    }

    /**
     * 暂停下载
     */
    public void pause(){
        setDownloadStatus(STATUS_PAUSE);
        for (DownloadRunable runable : mProgressInfo) {
            runable.invalid();
            mThreadPoolExecutor.remove(runable);
        }
    }

    /**
     * 立即停止所有任务
     */
    public void stop(){
        mThreadPoolExecutor.shutdownNow();
    }

    @Override
    public void onData(long start, byte[] bytes) {
        synchronized (this){
            long timeMillis = System.currentTimeMillis();
            long l = timeMillis - mTimestamp;
            if (l > 1000){
                if (l > 2000){
                    mSpeed = bytes.length;
                }else{
                    mSpeed = mBytesSecond;
                }
                mTimestamp = timeMillis;
                mBytesSecond = 0;
            }
            try {
                mRandomAccessFile.seek(start);
                mRandomAccessFile.write(bytes);
                mCompleteBytes += bytes.length;
                mBytesSecond += bytes.length;
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (mDownloadListener != null){
                mDownloadListener.onDownloadProgress(this,mCompleteBytes,mContentInfo.getContentLength(),mSpeed);
            }
            Log.i(TAG, "onData: "+mCompleteBytes+"/"+mContentInfo.getContentLength());
        }
    }

    @Override
    public void onComplete(DownloadRunable runable) {
        if (mContentInfo.isAcceptRanges() && mCompleteBytes < mContentInfo.getContentLength()) {
            divisionTask();
            return;
        }

        if (mContentInfo.getContentLength() > 0){
            if (mContentInfo.getContentLength() == mCompleteBytes){
                if (getDownloadStatus() != STATUS_COMPLETE) {
                    setDownloadStatus(STATUS_COMPLETE);
                }
            }
        }else{
            for (DownloadRunable downloadRunable : mProgressInfo) {
                if (downloadRunable.getSurplus() < 0){
                    if (downloadRunable.getStatus() != DownloadRunable.STATUS_IDLE){
                        return;
                    }
                }else if (downloadRunable.getSurplus() != 0){
                    return;
                }
            }
            if (getDownloadStatus() != STATUS_COMPLETE) {
                setDownloadStatus(STATUS_COMPLETE);
            }
        }
    }

    @Override
    public void onFaild(DownloadRunable runable, Throwable e) {
        for (DownloadRunable downloadRunable : mProgressInfo) {
            downloadRunable.invalid();
            mThreadPoolExecutor.remove(downloadRunable);
        }
        if (getDownloadStatus() != STATUS_FAILED) {
            setDownloadStatus(STATUS_FAILED);
        }
    }

    public IADownloadListener getDownloadListener() {
        return mDownloadListener;
    }

    public void setDownloadListener(IADownloadListener downloadListener) {
        this.mDownloadListener = downloadListener;
    }

    public int getDownloadStatus() {
        return mDownloadStatus;
    }

    public void setDownloadStatus(int downloadStatus) {
        this.mDownloadStatus = downloadStatus;
        if (downloadStatus == STATUS_COMPLETE && mTempFile.exists()){
            try {
                mRandomAccessFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mTempFile.renameTo(mFile);
        }
        if (mDownloadListener != null){
            mDownloadListener.onDownloadStatusUpdate(this,downloadStatus);
        }
    }

    public boolean saveToDisk(Context context, String key){
        File file = getFileByKey(context, key);
        if (file.exists()){
            file.delete();
            Log.w(TAG, "saveToDisk: 覆盖已存在文件:"+key);
        }
        try {
            file.createNewFile();
            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
            Parcel parcel = Parcel.obtain();
            parcel.writeParcelable(this,0);
            byte[] marshall = parcel.marshall();
            outputStream.write(marshall);
            outputStream.flush();
            outputStream.close();
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 重启任务
     * @param context
     * @param key
     * @param listener
     * @return
     */
    public static ADownloader resumeWithKey(Context context, String key, IADownloadListener listener){
        File file = getFileByKey(context, key);
        if (!file.exists()){
            return null;
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] bytes = new byte[fileInputStream.available()];
            fileInputStream.read(bytes);
            fileInputStream.close();
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0);
            ADownloader downloader = parcel.readParcelable(ADownloader.class.getClassLoader());
            downloader.setDownloadListener(listener);
            return downloader;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取本地序列化的文件
     * @param context 上下文
     * @param key key
     * @return 文件
     */
    private static File getFileByKey(Context context, String key){
        File file = new File(context.getExternalFilesDir(null), ".adownload");
        if (!file.exists() || !file.isDirectory()){
            file.mkdirs();
        }
        return new File(file, MD5Utils.getMD5(key));
    }

    public File getFile() {
        return mFile;
    }

    public String getUrl() {
        return mUrl;
    }

    public AConfig getConfig() {
        return mConfig;
    }

    public long getCompleteBytes() {
        return mCompleteBytes;
    }


    public float getTotalBytes() {
        return mContentInfo.getContentLength();
    }


    public int getThreads(){
        return mThreadPoolExecutor.getMaximumPoolSize();
    }

    public int getActiveThreads(){
        return mThreadPoolExecutor.getActiveCount();
    }

    @Override
    public int describeContents() {
        return 0;
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mFile.getAbsolutePath());
        dest.writeString(mTempFile.getAbsolutePath());
        dest.writeString(mUrl);
        dest.writeParcelable(mConfig, flags);
        dest.writeParcelable(mContentInfo, flags);
        dest.writeLong(mCompleteBytes);
        dest.writeTypedList(mProgressInfo);
    }

    protected ADownloader(Parcel in) {
        mFile = new File(Objects.requireNonNull(in.readString()));
        mTempFile = new File(Objects.requireNonNull(in.readString()));
        mUrl = in.readString();
        mConfig = in.readParcelable(AConfig.class.getClassLoader());
        mContentInfo = in.readParcelable(ContentInfo.class.getClassLoader());
        mCompleteBytes = in.readLong();
        mProgressInfo = in.createTypedArrayList(DownloadRunable.CREATOR);
        mDownloadStatus = STATUS_PAUSE;
        if (mProgressInfo!=null){
            for (DownloadRunable downloadRunable : mProgressInfo) {
                downloadRunable.setDataCallback(this);
            }
        }
        initDownloader();
    }

    public static final Creator<ADownloader> CREATOR = new Creator<ADownloader>() {
        @Override
        public ADownloader createFromParcel(Parcel in) {
            return new ADownloader(in);
        }

        @Override
        public ADownloader[] newArray(int size) {
            return new ADownloader[size];
        }
    };
}
