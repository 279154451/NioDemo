package com.single.code.app.niodemo.nio;

/**
 * 创建时间：2022/3/2
 * 创建人：singleCode
 * 功能描述：
 **/
public class NioServer {
    private static NioServerHandle handle;
    public static void main(String[] args) {
        handle = new NioServerHandle(8080);
        new Thread(handle,"server").start();
    }
}
