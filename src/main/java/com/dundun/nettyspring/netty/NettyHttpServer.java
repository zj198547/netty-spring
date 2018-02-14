package com.dundun.nettyspring.netty;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.DispatcherServlet;

import com.dundun.nettyspring.common.RemotingUtil;
import com.dundun.nettyspring.common.ThreadFactoryImpl;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

@Component
public class NettyHttpServer {
    
    private static final Logger   log             = LoggerFactory.getLogger(NettyHttpServer.class);
    
    private final ServerBootstrap serverBootstrap = new ServerBootstrap();
    
    private final EventLoopGroup  eventLoopGroupBoss;
    
    private final EventLoopGroup  eventLoopGroupSelector;
    
    @Autowired
    private DispatcherServlet     dispatcherServlet;
    
    @Value("${netty.host:127.0.0.1}")
    private String                host;
    
    @Value("${netty.ioThreadNum:4}")
    private int                   ioThreadNum;
    
    @Value("${netty.backlog:1024}")
    private int                   backlog;
    
    @Value("${netty.port:9090}")
    private int                   port;
    
    @Value("${netty.so_sndbuf:65535}")
    private int                   so_sndbuf;
    
    @Value("${netty.so_rcvbuf:65535}")
    private int                   so_rcvbuf;
    
    @Value("${netty.use_epoll:false}")
    private boolean               useEpollNativeSelector;
    
    public static final int       CPUS            = Math.max(2, Runtime.getRuntime().availableProcessors());
    
    // FIXME:线程池设置成可配置
    private final ExecutorService executor        = Executors.newFixedThreadPool(2 * CPUS, new ThreadFactoryImpl(
        "HttpServerThread_"));
    
    private EventExecutorGroup    eventExecutorGroup;
    
    public NettyHttpServer() {
        
        this.eventLoopGroupBoss = new NioEventLoopGroup();
        
        if (useEpoll()) {
            this.eventLoopGroupSelector = new EpollEventLoopGroup(
                2 * CPUS, // FIXME: 可配置
                new ThreadFactory() {
                    
                    private AtomicInteger threadIndex = new AtomicInteger(0);
                    
                    @Override
                    public Thread newThread(Runnable r) {
                        
                        return new Thread(r, "EpollSelectorThread_" + this.threadIndex.incrementAndGet());
                    }
                });
        } else {
            this.eventLoopGroupSelector = new NioEventLoopGroup(
                2 * CPUS, // FIXME: 可配置
                new ThreadFactory() {
                    
                    private AtomicInteger threadIndex = new AtomicInteger(0);
                    
                    @Override
                    public Thread newThread(Runnable r) {
                        
                        return new Thread(r, "NioSelectorThread_" + this.threadIndex.incrementAndGet());
                    }
                });
        }
        
        this.eventExecutorGroup = new DefaultEventExecutorGroup(
            2 * CPUS, // FIXME: 可配置
            new ThreadFactory() {
                
                private AtomicInteger threadIndex = new AtomicInteger(0);
                
                @Override
                public Thread newThread(Runnable r) {
                    
                    return new Thread(r, "NettyServerCodecThread_" + this.threadIndex.incrementAndGet());
                }
            });
        
    }
    
    public void start() throws InterruptedException {
        
        ServerBootstrap childHandler = this.serverBootstrap.group(eventLoopGroupBoss, eventLoopGroupSelector)
            .channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, backlog)
            .option(ChannelOption.SO_REUSEADDR, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true) //是否保持连接
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_SNDBUF, so_sndbuf)
            .childOption(ChannelOption.SO_RCVBUF, so_rcvbuf)
            .localAddress(new InetSocketAddress(port))
            .childHandler(new DispatcherServletChannelInitializer(
                this.dispatcherServlet,
                this.eventExecutorGroup,
                this.executor));
        // FIXME:netty其他配置
        //        if (nettyServerConfig.isServerPooledByteBufAllocatorEnable()) {
        childHandler.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        //        }
        
        try {
            ChannelFuture sync = this.serverBootstrap.bind().sync();
            InetSocketAddress addr = (InetSocketAddress)sync.channel().localAddress();
            
            this.port = addr.getPort();
            log.info("start NettyHttpServer port {} success ", this.port);
        }
        catch (InterruptedException e) {
            throw new RuntimeException("this.serverBootstrap.bind().sync() InterruptedException", e);
        }
        
    }
    
    public void stop() {
        
        try {
            this.eventLoopGroupBoss.shutdownGracefully();
            this.eventLoopGroupSelector.shutdownGracefully();
        }
        catch (Exception e) {
            log.error("NettyHttpServer shutdown exception, ", e);
        }
        
        this.eventExecutorGroup.shutdownGracefully();
        this.executor.shutdown();
        
        log.info("stop NettyHttpServer");
    }
    
    private boolean useEpoll() {
        
        return RemotingUtil.isLinuxPlatform() && useEpollNativeSelector && Epoll.isAvailable();
    }
}
