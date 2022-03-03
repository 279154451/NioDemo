package com.single.code.app.niodemo.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 创建时间：2022/3/2
 * 创建人：singleCode
 * 功能描述：
 **/
public class NioServerHandle implements Runnable{
    private int port;
    private volatile boolean started = false;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    public NioServerHandle(int port) {
        this.port = port;
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            //设置通道为非阻塞模式
            serverSocketChannel.configureBlocking(false);
            //绑定端口
            serverSocketChannel.socket().bind(new InetSocketAddress(port));
            //注册接收客户端链接事件
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            started = true;
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void stop(){
        started = false;
    }

    @Override
    public void run() {
        while (started){
            try {
                selector.select(1000);//每隔一秒被唤醒一次阻塞状态，同时如果有事件产生也会被立马唤醒
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                if(selectionKeys!= null && !selectionKeys.isEmpty()){
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    SelectionKey selectionKey = null;
                    while (iterator.hasNext()){
                        selectionKey = iterator.next();
                        iterator.remove();//需要将这个事件remove，否则下一次新事件来时之前的事件会被重复处理
                        try {
                            if(selectionKey.isValid()){
                                if(selectionKey.isAcceptable()){//有连接事件
                                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
                                    SocketChannel socketChannel = serverSocketChannel.accept();//接收客户端连接
                                    socketChannel.configureBlocking(false);//设置为非阻塞模式
                                    socketChannel.register(selector,SelectionKey.OP_READ);//注册读事件，当有数据过来时通知服务端
                                    System.out.println("有客户端连接进来了");
                                }
                                if(selectionKey.isReadable()){//有数据过来
                                    SocketChannel channel = (SocketChannel) selectionKey.channel();
                                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                                    int readBytes = channel.read(byteBuffer);//readBytes表示从Channel中读了多少字节数据
                                    if(readBytes>0){
                                        byteBuffer.flip();//从Channel对Buffer的写模式切换到读模式
                                        byte[] buffer = new byte[readBytes];
                                        byteBuffer.get(buffer);//获取从Channel写入Buffer的数据
                                        String message = new String(buffer,"UTF-8");
                                        System.out.println("服务器收到消息:"+message);
                                        //服务器响应
//                                      doResponse(channel,message);//直接通过通道写数据
                                      doResponseByRegister(channel,message);//通过注册写事件写数据
                                    }else if(readBytes<0){//链路已经关闭，释放资源
                                        selectionKey.cancel();
                                        channel.close();
                                    }
                                }
                                if(selectionKey.isWritable()){
                                    SocketChannel channel = (SocketChannel) selectionKey.channel();
                                    ByteBuffer writeBuffer = (ByteBuffer) selectionKey.attachment();
                                    if(writeBuffer.hasRemaining()){//有要发送的数据
                                        channel.write(writeBuffer);
                                    }else {
                                       selectionKey.interestOps(SelectionKey.OP_READ);//写完后，只关注读事件，也就是注销写事件。这样能够避免isWritable一直true导致重复写的问题
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

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            if(selector !=null){
                selector.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 直接通过通道写数据到对端
     * @param channel
     * @param message
     * @throws IOException
     */
    public void doResponse(SocketChannel channel,String message) throws IOException {
        String response =  getResponse(message);
        byte[] resBytes = response.getBytes();
        ByteBuffer writeBuffer = ByteBuffer.allocate(resBytes.length);
        writeBuffer.put(resBytes);//将字节写入缓存中去
        writeBuffer.flip();//从当前写模式切换到读模式
        channel.write(writeBuffer);//channel从缓存中读响应数据，发送到对端
        System.out.println("服务器响应消息:"+response);
    }
    /**
     * 通过注释写事件来写数据到对端
     * @param channel
     * @param message
     * @throws IOException
     */
    public void doResponseByRegister(SocketChannel channel,String message) throws IOException {
        String response =  getResponse(message);
        byte[] resBytes = response.getBytes();
        ByteBuffer writeBuffer = ByteBuffer.allocate(resBytes.length);
        writeBuffer.put(resBytes);//将字节写入缓存中去
        writeBuffer.flip();//从当前写模式切换到读模式
        channel.register(selector,SelectionKey.OP_READ|SelectionKey.OP_WRITE,writeBuffer);//通过注册读写事件来写数据到对端
    }
    private String getResponse(String msg){
        return "Hello "+msg+" has received";
    }
}
