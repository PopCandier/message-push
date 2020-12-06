package com.pop.netty.chat.server.handler;

import com.pop.netty.chat.processor.MsgProcessor;
import com.pop.netty.chat.protocol.IMMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Pop
 * @date 2020/12/6 15:22
 *
 * 用于处理java控制台发过来的javaObject消息体
 */
@Slf4j
public class TerminalServerHandler extends SimpleChannelInboundHandler<IMMessage> {

    private MsgProcessor processor = new MsgProcessor();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IMMessage msg) throws Exception {
        processor.sendMsg(ctx.channel(),msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
       log.info("Socket Client: 与服务器断开连接："+cause.getMessage());
       cause.printStackTrace();
       ctx.close();
    }
}
