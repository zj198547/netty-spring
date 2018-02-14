package com.dundun.nettyspring.main;

import java.util.concurrent.Callable;

import javax.servlet.ServletException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import com.dundun.nettyspring.common.ServerUtil;
import com.dundun.nettyspring.common.ShutdownHookThread;
import com.dundun.nettyspring.netty.NettyHttpServer;

public class ServerStartup {
    
    private static Logger          log         = LoggerFactory.getLogger(ServerStartup.class);
    
    private static NettyHttpServer server;
    
    public static CommandLine      commandLine = null;
    
    public static void main(String[] args) throws Exception {
        
        main0(args);
    }
    
    private static void main0(String[] args) {
        
        try {
            
            Options options = ServerUtil.buildCommandlineOptions(new Options());
            commandLine = ServerUtil.parseCmdLine("nettyspringsrv", args, options, new PosixParser());
            if (null == commandLine) {
                System.exit(-1);
                return;
            }
            
            boolean initResult = init();
            if (!initResult) {
                if (null != server) {
                    server.stop();
                }
                System.exit(-3);
            }
            
            server.start();
        }
        catch (Throwable e) {
            e.printStackTrace();
            System.exit(-1);
        }
        
    }
    
    /**
     * init
    */
    private static boolean init() {
        
        // servlet context mock
        MockServletContext servletContext = new MockServletContext();
        MockServletConfig servletConfig = new MockServletConfig(servletContext, "SrpingMVC");
        
        XmlWebApplicationContext wac = new XmlWebApplicationContext();
        wac.setServletContext(servletContext);
        wac.setServletConfig(servletConfig);
        // 可以扩展dubbo
        wac.setConfigLocations(new String[] {"classpath:spring-config.xml", "classpath:spring-beans.xml"});
        wac.refresh();
        
        // spring mvc dispatcherServlet
        DispatcherServlet dispatcherServlet = wac.getBean(DispatcherServlet.class);
        dispatcherServlet.setApplicationContext(wac);
        try {
            dispatcherServlet.init(servletConfig);
        }
        catch (ServletException e) {
            log.error(e.getMessage());
            e.printStackTrace();
            return false;
        }
        
        // netty http server
        server = wac.getBean(NettyHttpServer.class);
        
        // add shutdown hook
        Runtime.getRuntime().addShutdownHook(new ShutdownHookThread(log, new Callable<Void>() {
            
            @Override
            public Void call() throws Exception {
                
                server.stop();
                return null;
            }
        }));
        
        return true;
    }
    
}
