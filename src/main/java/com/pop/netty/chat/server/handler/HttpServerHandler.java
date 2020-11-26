package com.pop.netty.chat.server.handler;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URL;

/**
 * @program: message-push
 * @description: 处理服务端分发请求地逻辑
 * @author: Pop
 * @create: 2020-11-25 22:30
 **/
@Slf4j
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    // 获取 class 路径
    private URL baseURL = HttpServerHandler.class.getResource("");
    private final String webroot = "webroot";

    private File getResource(String fileName) throws Exception{
        String basePath  = baseURL.toURI().toString();
        int start = basePath.indexOf("classes/");
        basePath = (basePath.substring(0,start)+"/"+"classes/").replaceAll("/+","/");
        String path = basePath + webroot+"/"+fileName;
        // 去掉 file 前缀
        path = !path.contains("file:")?path:path.substring(5);
        path = path.replaceAll("//","/");
        return new File(path);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String uri = request.uri();
        RandomAccessFile file = null;
        try{
            String page = uri.equals("/")?"chat.html":uri;
            file = new RandomAccessFile(getResource(page),"r");
        }catch (Exception e){
            ctx.fireChannelRead(request.retain());
            return ;
        }
        //响应地版本和状态
        HttpResponse response = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);
        String contextType = "text/html;";
        //当请求非页面地其它资源，css 或者 js
        if(uri.endsWith(".css")){
            contextType = "text/css;";
        }else if(uri.endsWith(".js")){
            contextType = "text/javascript;";
        }else if(uri.toLowerCase().matches(".*\\.(jpg|png|gif)$")){
            //图片资源
            String ext = uri.substring(uri.lastIndexOf("."));
            contextType = "image/"+ext;
        }
        response.headers().set(HttpHeaderNames.CONTENT_TYPE,contextType+"charset=utf-8;");
        boolean keepAlive = HttpUtil.isKeepAlive(request);

        //是否保持长连接
        if(keepAlive){
            //设置返回头
            //文件长度
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH,file.length());
            //保持长连接
            response.headers().set(HttpHeaderNames.CONNECTION,HttpHeaderValues.KEEP_ALIVE);
        }

       ctx.write(response);
        //将整个文件，变成字节码写出去
        ctx.write(new DefaultFileRegion(file.getChannel(),0,file.length()));
        //结尾
        ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if(!keepAlive){
            //如果不是长连接，将通道关闭
            future.addListener(ChannelFutureListener.CLOSE);
        }
        file.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //发生异常
        Channel client = ctx.channel();
        log.info("Client: "+client.remoteAddress()+" 异常");
        //异常就关闭
        cause.printStackTrace();
        ctx.close();
    }
}
