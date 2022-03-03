package com.single.code.app.niodemo.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 创建时间：2022/3/2
 * 创建人：singleCode
 * 功能描述：
 **/
public class NioClientHandle implements Runnable {
    private int port;
    private String host;
    private Selector selector;
    private SocketChannel socketChannel;
    private volatile boolean started = false;
    public NioClientHandle(int port, String host) {
        this.port = port;
        this.host = host;
        try{
            selector = Selector.open();
            socketChannel = SocketChannel.open();
            //设置非阻塞模式
            socketChannel.configureBlocking(false);
            started = true;
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    public void stop(){
        started = false;
    }

    @Override
    public void run() {
        try {
            connect();//连接服务端
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);//连接失败退出
        }
        while (started){
            try {
                selector.select(1000);
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                if(selectionKeys != null && !selectionKeys.isEmpty()){
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    SelectionKey selectionKey = null;
                    while (iterator.hasNext()){
                        selectionKey  = iterator.next();
                        iterator.remove();
                       try {
                           if(selectionKey.isValid()){
                               SocketChannel scChannel = (SocketChannel) selectionKey.channel();
                               if(selectionKey.isConnectable()){//连接完成事件通知
                                  if(scChannel.finishConnect()){
                                      socketChannel.register(selector, SelectionKey.OP_READ);//连接完成，注册读事件
                                  }else {//连接失败,退出
                                      System.exit(1);
                                  }
                               }
                               if(selectionKey.isReadable()){//接收到服务端响应事件
                                   SocketChannel channel = (SocketChannel) selectionKey.channel();
                                   ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                                   int readBytes = channel.read(byteBuffer);//readBytes表示从Channel中读了多少字节数据
                                   if(readBytes>0){
                                       byteBuffer.flip();//从Channel对Buffer的写模式切换到读模式
                                       byte[] buffer = new byte[readBytes];
                                       byteBuffer.get(buffer);//获取从Channel写入Buffer的数据
                                       String message = new String(buffer,"UTF-8");
                                       System.out.println("客户端收到消息:"+message);

                                   }else if(readBytes<0){//链路已经关闭，释放资源
                                       selectionKey.cancel();
                                       channel.close();
                                   }
                               }
                           }
                       }catch (IOException e){
                           e.printStackTrace();
                           if(selectionKey!= null){
                               selectionKey.cancel();
                               if(selectionKey.channel()!= null){
                                   selectionKey.channel().close();
                               }
                           }
                       }

                    }
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
    private void connect() throws IOException {
        //非阻塞的连接请求,当他返回的时候，连接不一定完成了，
        // 如果返回值=true,表示连接完成。
        // 返回值=false,表示连接还未完成，还在三次握手过程中
        boolean connect = socketChannel.connect(new InetSocketAddress(host, port));
        if(connect){
            socketChannel.register(selector, SelectionKey.OP_READ);//连接完成，注册读事件
        }else {
            socketChannel.register(selector,SelectionKey.OP_CONNECT);//连接还未完成在三次握手过程中，注册连接完成事件，当连接完成时通知客户端
        }
    }

    public void sendMsg(String msg) {
        byte[] bytes = msg.getBytes();
        ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
        writeBuffer.put(bytes);
        writeBuffer.flip();
        try {
            socketChannel.write(writeBuffer);
            System.out.println("客户端发送数据:"+msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
