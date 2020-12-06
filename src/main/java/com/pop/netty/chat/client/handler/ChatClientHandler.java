package com.pop.netty.chat.client.handler;

import com.pop.netty.chat.client.ChatClient;
import com.pop.netty.chat.protocol.IMMessage;
import com.pop.netty.chat.protocol.IMP;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Pop
 * @date 2020/12/6 15:47
 */
@Slf4j
public class ChatClientHandler extends SimpleChannelInboundHandler<IMMessage> {

    private ChannelHandlerContext ctx;

    private String nickName;

    public ChatClientHandler(String nickName){
        this.nickName = nickName;
    }

    //启动客户端控制台
    private void session() throws IOException{
        new Thread(){
            @Override
            public void run() {
                System.out.println(nickName+", 你好，请在控制台输入对话内容");
                IMMessage message = null;
                Scanner scanner = new Scanner(System.in);

                do {
                    if(scanner.hasNext()){
                        String input = scanner.nextLine();
                        if("exit".equals(input)){
                            message = new IMMessage(IMP.LOGOUT.getName(),"Console",System.currentTimeMillis(),nickName);
                        }else{
                            message = new IMMessage(IMP.CHAT.getName(),System.currentTimeMillis(),nickName,input);
                        }
                    }
                }while (sendMsg(message));
                scanner.close();

            }
        }.start();
    }

    /**
     * tcp链路建立连接成功后
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        IMMessage message = new IMMessage(IMP.LOGIN.getName(),"Console",System.currentTimeMillis(),this.nickName);
        sendMsg(message);
        log.info("成功连接服务器，已经执行登陆动作");
        session();//开始会话
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("与服务器断开连接:"+cause.getMessage());
        ctx.close();
    }

    /**
     *
     * 发送消息
     * @param msg
     * @return
     */
    private boolean sendMsg(IMMessage msg){
        ctx.channel().writeAndFlush(msg);
        System.out.println("继续输入开始对话...");
        return msg.getCmd().equals(IMP.LOGOUT)?false:true;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IMMessage msg) throws Exception {
            IMMessage m =  msg;
        System.out.println((null==m.getSender()?"":(m.getSender()+":"))+removeHtmlTag(m.getContent()));
    }

    /**
     * 将html的前后标签移除掉
     * @param htmlStr
     * @return
     */
    public static String removeHtmlTag(String htmlStr){
        String regEx_script="<script[^>]*?>[\\s\\S]*?<\\/script>";//定义Script正则表达式
        String regEx_style="<style[^>]*?[\\s\\S]*?<\\/style>>";//定义style正则表达式
        String regEx_html = "<[^>]+>";//定义html标签的正则表达式

        Pattern p_script = Pattern.compile(regEx_script,Pattern.CASE_INSENSITIVE);
        Matcher m_script = p_script.matcher(htmlStr);
        htmlStr = m_script.replaceAll("");//过滤掉Script标签

        Pattern p_style = Pattern.compile(regEx_style,Pattern.CASE_INSENSITIVE);
        Matcher m_style = p_style.matcher(htmlStr);
        htmlStr = m_style.replaceAll("");//过滤掉style标签

        Pattern p_html = Pattern.compile(regEx_html,Pattern.CASE_INSENSITIVE);
        Matcher m_html = p_html.matcher(htmlStr);
        htmlStr = m_html.replaceAll("");//过滤掉html标签

        return htmlStr.trim();

    }

}
