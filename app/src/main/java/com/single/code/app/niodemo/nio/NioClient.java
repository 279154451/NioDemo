package com.single.code.app.niodemo.nio;

import java.util.Scanner;

/**
 * 创建时间：2022/3/2
 * 创建人：singleCode
 * 功能描述：
 **/
public class NioClient {
    private static NioClientHandle handle;

    public static void main(String[] args) {
        startClient();
        Scanner scanner = new Scanner(System.in);
        while (true){
            sendMsg(scanner.next());
        }
    }

    public static void sendMsg(String msg) {
        if (handle != null) {
            handle.sendMsg(msg);
        }
    }

    public static void startClient() {
        handle = new NioClientHandle(8080, "127.0.0.1");
        new Thread(handle, "client").start();
    }
}
