package com.zhicheng.downloaderdemo;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        String test = "bytes 0-5135182/5135183";
        System.out.println(test.split("/")[1]);
    }
}