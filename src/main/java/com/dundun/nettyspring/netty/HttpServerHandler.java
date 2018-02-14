package com.dundun.nettyspring.netty;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import com.dundun.nettyspring.http.HttpServletRequestWapper;
import com.dundun.nettyspring.http.HttpServletResponseWapper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.CharsetUtil;

/**
* TODO(这里用一句话描述这个类的作用)
* @author dundun
* @date 2018年2月8日
*
*/
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    
    private final DispatcherServlet servlet;
    
    private final ServletContext    servletContext;
    
    private final ExecutorService   executorService;
    
    public HttpServerHandler(DispatcherServlet servlet, ExecutorService executorService) {
        this.servlet = servlet;
        this.servletContext = servlet.getServletConfig().getServletContext();
        this.executorService = executorService;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        
        boolean isKeepAlive = HttpUtil.isKeepAlive(req);
        
        if (!req.decoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST);
            return;
        }
        
        executorService.execute(new Runnable() {
            
            @Override
            public void run() {
                
                HttpServletRequest servletRequest = createServletRequest(req);
                HttpServletResponseWapper servletResponse = new HttpServletResponseWapper();
                
                try {
                    servlet.service(servletRequest, servletResponse);
                }
                catch (ServletException e) {
                    // TODO Auto-generated catch block
                }
                catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
                FullHttpResponse response = createFullHttpResponse(servletResponse);
                
                ChannelFuture writeFuture = ctx.writeAndFlush(response);
                if (!isKeepAlive) {
                    writeFuture.addListener(ChannelFutureListener.CLOSE);
                }
            }
            
        });
        
    }
    
    private HttpServletRequest createServletRequest(FullHttpRequest fullHttpRequest) {
        
        UriComponents uriComponents = UriComponentsBuilder.fromUriString(fullHttpRequest.uri()).build();
        
        HttpServletRequestWapper servletRequest = new HttpServletRequestWapper(this.servletContext);
        servletRequest.setRequestURI(uriComponents.getPath());
        servletRequest.setPathInfo(uriComponents.getPath());
        servletRequest.setMethod(fullHttpRequest.method().name());
        
        if (uriComponents.getScheme() != null) {
            servletRequest.setScheme(uriComponents.getScheme());
        }
        if (uriComponents.getHost() != null) {
            servletRequest.setServerName(uriComponents.getHost());
        }
        if (uriComponents.getPort() != -1) {
            servletRequest.setServerPort(uriComponents.getPort());
        }
        
        for (String name : fullHttpRequest.headers().names()) {
            servletRequest.addHeader(name, fullHttpRequest.headers().get(name));
        }
        
        ByteBuf bbContent = fullHttpRequest.content();
        if (bbContent.hasArray()) {
            byte[] baContent = bbContent.array();
            servletRequest.setContent(baContent);
        }
        
        try {
            if (uriComponents.getQuery() != null) {
                String query = UriUtils.decode(uriComponents.getQuery(), "UTF-8");
                servletRequest.setQueryString(query);
            }
            
            for (Entry<String, List<String>> entry : uriComponents.getQueryParams().entrySet()) {
                for (String value : entry.getValue()) {
                    servletRequest.addParameter(
                        UriUtils.decode(entry.getKey(), "UTF-8"),
                        UriUtils.decode(value, "UTF-8"));
                }
            }
        }
        catch (UnsupportedEncodingException e) {
            // shouldn't happen
        }
        
        return servletRequest;
    }
    
    private FullHttpResponse createFullHttpResponse(HttpServletResponseWapper servletResponse) {
        
        HttpResponseStatus status = HttpResponseStatus.valueOf(servletResponse.getStatus());
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status);
        
        for (String name : servletResponse.getHeaderNames()) {
            for (Object value : servletResponse.getHeaderValues(name)) {
                response.headers().add(name, value);
            }
        }
        
        String respStr = null;
        try {
            respStr = servletResponse.getContentAsString();
        }
        catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (respStr != null) {
            Charset encoding = Charset.forName(servletResponse.getCharacterEncoding());
            response.content().writeBytes(Unpooled.copiedBuffer(respStr, encoding));
        }
        return response;
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }
    
    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        
        ByteBuf content = Unpooled.copiedBuffer(
            "Failure: " + status.toString() + "\r\n",
            CharsetUtil.UTF_8);
        
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(
            HTTP_1_1,
            status,
            content);
        fullHttpResponse.headers().add(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        
        // Close the connection as soon as the error message is sent.
        ctx.write(fullHttpResponse).addListener(ChannelFutureListener.CLOSE);
    }
}
