package com.zhicheng.downloaderdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.zhicheng.downloader.ADownloader;
import com.zhicheng.downloader.IADownloadListener;

import java.io.File;
import java.io.ObjectOutputStream;

public class MainActivity extends AppCompatActivity implements IADownloadListener {

    private String TAG = "MainActivity";
    private TextView mThreadInfoView;
    private TextView mSpeedView;
    private ProgressBar mProgressBar;

    private Handler mHandler = new Handler();
    ADownloader aDownloader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();

        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1000);
    }

    private void initUI() {
        mThreadInfoView = findViewById(R.id.tv_thread_info);
        mSpeedView = findViewById(R.id.tv_speed);
        mProgressBar = findViewById(R.id.pb_progress);
//        aDownloader = new ADownloader(new File(Environment.getExternalStorageDirectory(), "奇门遁甲.mp4"), "http://down.phpzuida.com/2003/奇门遁甲.HD1280高清国语中字版.mp4");
        aDownloader = new ADownloader(new File(Environment.getExternalStorageDirectory(), "驯龙高手.mp4"), "http://vfx.mtime.cn/Video/2019/02/04/mp4/190204084208765161.mp4");
        aDownloader.setDownloadListener(this);
        findViewById(R.id.btn_download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aDownloader.start();
            }
        });

        findViewById(R.id.btn_pause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aDownloader.pause();
            }
        });

        findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aDownloader.saveToDisk(MainActivity.this,"1111");
            }
        });

        findViewById(R.id.btn_read).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aDownloader = ADownloader.resumeWithKey(MainActivity.this,"1111",MainActivity.this);
                mProgressBar.setProgress((int) (aDownloader.getCompleteBytes()*1.0f/aDownloader.getTotalBytes()*100));
                mSpeedView.setText(String.format("0 Kb/s"));
                mThreadInfoView.setText(aDownloader.getActiveThreads()+"/"+aDownloader.getThreads());
            }
        });
    }

    @Override
    public void onDownloadProgress(ADownloader downloader, final long completeBytes, final long totalBytes, final long speed) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setProgress((int) (completeBytes*1.0f/totalBytes*100));
                mSpeedView.setText(String.format("%dKb/s",speed/1024));
                mThreadInfoView.setText(aDownloader.getActiveThreads()+"/"+aDownloader.getThreads());
            }
        });
    }

    @Override
    public void onDownloadStatusUpdate(ADownloader downloader, int status) {
        switch (status){
            case ADownloader.STATUS_FAILED:
                showToast("下载出错");
                break;
            case ADownloader.STATUS_PARSE:
                showToast("解析中");
                break;
            case ADownloader.STATUS_PAUSE:
                showToast("暂停下载");
                break;
            case ADownloader.STATUS_START:
                showToast("开始下载");
                break;
            case ADownloader.STATUS_COMPLETE:
                showToast("下载完成");
                break;
        }
    }

    /**
     * 显示吐司
     * @param toast
     */
    private Toast toast;
    private void showToast(final String msg){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (toast != null){
                    toast.cancel();
                }
                toast = Toast.makeText(MainActivity.this,"",Toast.LENGTH_LONG);
                toast.setText(msg);
                toast.show();
            }
        });
    }
}
