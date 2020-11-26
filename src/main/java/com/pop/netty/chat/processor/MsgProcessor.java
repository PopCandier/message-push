package com.pop.netty.chat.processor;

import com.alibaba.fastjson.JSONObject;
import com.pop.netty.chat.protocol.IMDecoder;
import com.pop.netty.chat.protocol.IMEncoder;
import com.pop.netty.chat.protocol.IMMessage;
import com.pop.netty.chat.protocol.IMP;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.rmi.server.ExportException;


/**
 * @program: message-push
 * @description: 主要处理用户登录、退出、上线、下线、发送消息等行为动作
 * @author: Pop
 * @create: 2020-11-26 22:09
 **/
public class MsgProcessor {

    //记录在线用户 不定时的扫描
    private static ChannelGroup onlineUsers = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    //定义一些扩展属性
    public static final AttributeKey<String> NICK_NAME = AttributeKey.valueOf("nickName");
    public static final AttributeKey<String> IP_ADDR = AttributeKey.valueOf("ipAddr");
    public static final AttributeKey<JSONObject> ATTRS = AttributeKey.valueOf("attrs");
    public static final AttributeKey<String> FORM = AttributeKey.valueOf("from");

    //自定义解码器
    private IMDecoder decoder = new IMDecoder();
    //自定义编码器
    private IMEncoder encoder = new IMEncoder();

    /**
     * 获取用户昵称
     * @param client
     * @return
     */
    public String getNickName(Channel client){
        return client.attr(NICK_NAME).get();
    }

    /**
     * 获取远程用户地ip地址
     * @param client
     * @return
     */
    public String getAddress(Channel client){
        return client.remoteAddress().toString().replaceFirst("/","");
    }

    /**
     * 获取扩展属性
     * @param client
     * @return
     */
    public JSONObject getAttrs(Channel client){
        try{
            return client.attr(ATTRS).get();
        }catch (Exception e){
            return null;
        }
    }

    /**
     * 获取扩展属性
     * @param client
     * @param key
     * @param value
     */
    private void setAttrs(Channel client,String key, Object value){
        try{
            JSONObject json=client.attr(ATTRS).get();
            json.put(key, value);
            client.attr(ATTRS).set(json);
        }catch (Exception e){
            //说明是为空地情况，初始化一个新值
            JSONObject json = new JSONObject();
            json.put(key, value);
            client.attr(ATTRS).set(json);
        }
    }

    /**
     * 获取系统时间
     * @return
     */
    private Long sysTime(){
        return System.currentTimeMillis();
    }

    /**
     *  退出通知
     * @param client
     */
    public void logout(Channel client){
        //如果nickName 为 null ，说明没有遵守聊天协议地连接，表示为非法登陆
        if(getNickName(client)==null){return ;}
        for(Channel channel:onlineUsers){
            //构建离开的通知
            IMMessage request = new IMMessage(IMP.SYSTEM.getName(),sysTime(),
                    onlineUsers.size(),getNickName(client)+"离开");
            String content = encoder.encode(request);
            //向每个连接都发送消息,告知这个用户离开
            channel.writeAndFlush(new TextWebSocketFrame(content));
        }
        //移除
        onlineUsers.remove(client);
    }

    /**
     * 发送消息
     * @param client
     * @param msg
     */
    public void sendMsg(Channel client,IMMessage msg){
        sendMsg(client, encoder.encode(msg));
    }

    /**
     * 发送消息地核心逻辑，将会区别命令进行响应地处理
     * @param client
     * @param msg
     */
    public void sendMsg(Channel client,String msg){
         IMMessage request = decoder.decode(msg);
        if(null==request){return ;}
        String addr = getAddress(client);

        //指令地判断
        if(eq(request,IMP.LOGIN)){
            //关于登陆指令地处理
            //赋值
            client.attr(NICK_NAME).getAndSet(request.getSender());
            client.attr(IP_ADDR).getAndSet(addr);
            client.attr(FORM).getAndSet(request.getTerminal());
            System.out.println(client.attr(FORM).get());
            //增加一名用户
            onlineUsers.add(client);
            for(Channel channel:onlineUsers){
                boolean isself = (channel==client);//看看当前是不是自己
                if(!isself){
                    request = new IMMessage(IMP.SYSTEM.getName(),sysTime(),
                            onlineUsers.size(),getNickName(client)+" 加入");
                }else{
                    request = new IMMessage(IMP.SYSTEM.getName(),sysTime(),
                            onlineUsers.size(),"已与服务器建立连接");
                }
                String content = encoder.encode(request);
                //写出来
                channel.writeAndFlush(new TextWebSocketFrame(content));
            }
        }else if(eq(request,IMP.CHAT)){
            //关于聊天地指令
        }else if(eq(request,IMP.FLOWER)){
            //关于送花地指令
        }
    }

    private boolean eq(IMMessage request,IMP imp){
        return request.getCmd().equals(imp.getName());
    }

}
