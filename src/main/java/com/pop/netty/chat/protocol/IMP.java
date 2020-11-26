package com.pop.netty.chat.protocol;


/**
 * 自定义 IMP (Instant Messaging Protocol)
 * 即时通信协议
 * */
public enum IMP {

    /**
     * 系统消息
     */
    SYSTEM("SYSTEM"),
    /**
     * 登陆指令
     */
    LOGIN("LOGIN"),
    /**
     * 退出指令
     */
    LOGOUT("LOGOUT"),
    /**
     * 聊天消息
     */
    CHAT("CHAT"),
    /**
     * 送鲜花
     */
    FLOWER("FLOWER");
    /**
     * 是否属于本消息协议
     * @param content
     * @return
     */
    public static boolean isIMP(String content){
        return content.matches("^\\[(SYSTEM|LOGIN|LOGOUT|CHAT)\\]");
    }

    public String getName(){
        return this.name;
    }

    public String toString(){
        return this.name;
    }

    private String name;
    IMP(String name){
        this.name = name;
    }

}
