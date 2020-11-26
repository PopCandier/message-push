package com.pop.netty.chat.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.msgpack.MessagePack;

/**
 * @program: message-push
 * @description: 对IMP消息进行编码
 * @author: Pop
 * @create: 2020-11-24 22:18
 **/
public class IMEncoder extends MessageToByteEncoder<IMMessage> {
    @Override
    protected void encode(ChannelHandlerContext ctx, IMMessage msg, ByteBuf out) throws Exception {
        //用组件直接将对象直接转化为字节写出
        out.writeBytes(new MessagePack().write(msg));
    }

    /**
     * 将对象编码成协议字符串
     * @param msg
     * @return
     */
    public String encode(IMMessage msg){
        if(null==msg){return "";}
        String cmd = msg.getCmd();
        String prex = "["+cmd+"]"+"["+msg.getTime()+"]";
        if(IMP.LOGIN.getName().equals(cmd)||IMP.FLOWER.getName().equals(cmd)){
            prex+=("["+msg.getSender()+"]["+msg.getTerminal()+"]");
        }else if(IMP.CHAT.getName().equals(cmd)){
            prex+=("["+msg.getSender()+"]");//将发送者放置进去
        }else if(IMP.SYSTEM.getName().equals(cmd)){
            prex+=("["+msg.getOnline()+"]");//系统消息，在线人数
        }
        if(null==msg.getContent()||"".equals(msg.getContent())){
            prex+=(" - "+msg.getContent());
        }
        return prex;
    }

}
