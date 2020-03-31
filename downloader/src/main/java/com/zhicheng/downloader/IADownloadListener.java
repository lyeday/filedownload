package com.zhicheng.downloader;

import java.io.File;

/**
 * Project: DownloaderDemo
 * ClassName: IADownloadListener
 * Date: 2020/3/30 14:09
 * Creator: wuzhicheng
 * Email: voisen@icloud.com
 * Version: 1.0
 * Description:  this is IADownloadListener description !
 */
public interface IADownloadListener {
    void onDownloadStatusUpdate(ADownloader downloader, int status);
    void onDownloadProgress(ADownloader downloader, long completeBytes, long totalBytes, long speed);
}
