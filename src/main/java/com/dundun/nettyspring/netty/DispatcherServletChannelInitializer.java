package com.dundun.nettyspring.netty;

import java.util.concurrent.ExecutorService;

import org.springframework.web.servlet.DispatcherServlet;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.EventExecutorGroup;

public class DispatcherServletChannelInitializer extends
    ChannelInitializer<SocketChannel> {
    
    private final DispatcherServlet  dispatcherServlet;
    
    private final EventExecutorGroup defaultEventExecutorGroup;
    
    private final ExecutorService    executor;
    
    public DispatcherServletChannelInitializer(DispatcherServlet dispatcherServlet,
        EventExecutorGroup defaultEventExecutorGroup, ExecutorService executor) {
        
        this.dispatcherServlet = dispatcherServlet;
        this.defaultEventExecutorGroup = defaultEventExecutorGroup;
        this.executor = executor;
    }
    
    @Override
    public void initChannel(SocketChannel channel) throws Exception {
        
        // Create a default pipeline implementation.
        ChannelPipeline pipeline = channel.pipeline();
        
        // Uncomment the following line if you want HTTPS
        // SSLEngine engine =
        // SecureChatSslContextFactory.getServerContext().createSSLEngine();
        // engine.setUseClientMode(false);
        // pipeline.addLast("ssl", new SslHandler(engine));
        //            .addLast("compressor", new HttpContentCompressor(9))
        pipeline.addLast(defaultEventExecutorGroup,
            new HttpServerCodec(),
            new HttpObjectAggregator(65536),
            new ChunkedWriteHandler(),
            new ReadTimeoutHandler(300),
            new HttpServerHandler(this.dispatcherServlet, this.executor));
    }
    
}
