package com.zhicheng.downloader;

import android.os.Parcel;
import android.os.Parcelable;

import com.zhicheng.downloader.runable.DownloadRunable;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {


    private class Demo implements Parcelable{
        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {

        }
    }

    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void writeConfig(){
        Demo demo = new Demo();
        Parcel obtain = Parcel.obtain();
        obtain.writeTypedObject(demo,0);
        byte[] bytes = obtain.marshall();
        try {

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}