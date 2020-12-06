package com.pop.netty.chat.server;

import com.pop.netty.chat.protocol.IMDecoder;
import com.pop.netty.chat.protocol.IMEncoder;
import com.pop.netty.chat.server.handler.HttpServerHandler;
import com.pop.netty.chat.server.handler.TerminalServerHandler;
import com.pop.netty.chat.server.handler.WebSocketServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Pop
 * @date 2020/12/6 15:30
 *
 * 服务器基本思路是，所有客户端的消息全部发送到服务端的消息容器中，每一条消息都携带了客户端的
 * 标志信息，然后由服务器端转发给所有在线的客户端，先来看支持多协议的顶层设计
 */
@Slf4j
public class ChatServer {

    private int port = 8080;

    public void start(int port){

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try{

            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG,1024)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();

                            //自定义解析协议
                            pipeline.addLast(new IMDecoder());//inBound
                            pipeline.addLast(new IMEncoder());//outBound
                            pipeline.addLast(new TerminalServerHandler());//inBound

                            //解析HTTP 请求
                            pipeline.addLast(new HttpServerCodec());//outBound
                            // 主要是同一个Http 请求或响应的多个消息对象编程一个fullHttpRequest

                            // 用于处理大数据流，比如1gb的文件如果直接传送会占满JVM，加上这个handler便不用考虑这个问题
                            pipeline.addLast(new HttpObjectAggregator(64*1024));//Inbound

                            pipeline.addLast(new ChunkedWriteHandler());//InBound OutBound
                            pipeline.addLast(new HttpServerHandler());// InBound

                            //解析WebSocket 解码器
                            pipeline.addLast(new WebSocketServerProtocolHandler("/im"));

                            pipeline.addLast(new WebSocketServerHandler()); // Inbound

                        }
                    });

            ChannelFuture f = b.bind(this.port).sync();
            log.info("服务器已经启动，监听端口"+this.port);
            f.channel().closeFuture().sync();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }

    }

    public void start(){
        this.start(this.port);
    }

    public static void main(String[] args) {
        if(args.length>0){
            new ChatServer().start(Integer.valueOf(args[0]));
        }else{
            new ChatServer().start();
        }
    }

}
