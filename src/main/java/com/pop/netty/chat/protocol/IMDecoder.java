package com.pop.netty.chat.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.msgpack.MessagePack;
import org.msgpack.MessageTypeException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @program: message-push
 * @description: 自定义 解码器
 * @author: Pop
 * @create: 2020-11-24 21:25
 **/
public class IMDecoder extends ByteToMessageDecoder {

    /**
     * 返回请求内容的正则
     * [命令][命令发送时间][命令发送人][命令接收人] - 聊天内容
     * [CHAT][123456][Pop][ALL] - 你们好
     */
    private Pattern pattern = Pattern.compile("^\\[(.*)\\](\\s\\-\\s(.*))?");

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        try{
            //先获取可读的字节数
            final int length = byteBuf.readableBytes();
            final byte[] array = new byte[length];
            //将可读地区域转化为字符串          从哪里开始解码     解码到哪里
            String content = new String(array,byteBuf.readerIndex(),length);

            //空消息不解析
            if(!(null==content||"".equals(content.trim()))){
                if(!IMP.isIMP(content)){
                    //不符合规范将会被移除
                    channelHandlerContext.channel().pipeline().remove(this);
                    return;
                }
            }
            //将byteBuf的内容写到目标数组去
            byteBuf.getBytes(byteBuf.readerIndex(),array,0,length);
            //将目标数组解析成IMMessage对象
            list.add(new MessagePack().read(array,IMMessage.class));
            byteBuf.clear();
        }catch (MessageTypeException e){
            channelHandlerContext.channel().pipeline().remove(this);
        }
    }

    /**
     * 字符串解析成自定义即时通信协议
     * @param msg
     * @return
     */
    public IMMessage decode(String msg){
        if(null==msg||"".equals(msg.trim())){return null;}
        try {
            //匹配内容
            Matcher m = pattern.matcher(msg);
            String header = "";
            String content = "";
            if(m.matches()){
                header = m.group(1);
                content = m.group(3);
            }

            String [] heards = header.split("\\]\\[");
            long time = 0;
            try{ time = Long.parseLong(heards[1]);}catch (Exception e){}
            String nickName = heards[2];
            //昵称最多是个字符长度
            nickName = nickName.length()<10?nickName:nickName.substring(0,9);
            //判断消息地类型
            if(isType(msg,IMP.LOGIN.getName())){
                //是否是登陆命令    [命令]
                return new IMMessage(heards[0],heards[3],time,nickName);
            }else if(isType(msg,IMP.CHAT.getName())){
                //聊天信息          [CHAT][发送时间][发送人] - 发送内容
                return new IMMessage(heards[0],time,nickName,content);
            }else if(isType(msg,IMP.FLOWER.getName())){
                return new IMMessage(heards[0],heards[3],time,nickName);
            }else{
                return null;
            }
        }catch (Exception e){
            e.printStackTrace();
            return  null;
        }

    }

    private boolean isType(String target,String type){
        return target.startsWith("["+type+"]");
    }
}
