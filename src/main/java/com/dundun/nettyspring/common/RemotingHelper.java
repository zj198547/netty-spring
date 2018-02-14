package com.dundun.nettyspring.common;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;

public class RemotingHelper {
    
    public static final String  REMOTING        = "Remoting";
    
    public static final String  DEFAULT_CHARSET = "UTF-8";
    
    @SuppressWarnings("unused")
    private static final Logger log             = LoggerFactory.getLogger(REMOTING);
    
    public static String exceptionSimpleDesc(final Throwable e) {
        
        StringBuffer sb = new StringBuffer();
        if (e != null) {
            sb.append(e.toString());
            
            StackTraceElement[] stackTrace = e.getStackTrace();
            if (stackTrace != null && stackTrace.length > 0) {
                StackTraceElement elment = stackTrace[0];
                sb.append(", ");
                sb.append(elment.toString());
            }
        }
        
        return sb.toString();
    }
    
    public static SocketAddress string2SocketAddress(final String addr) {
        
        String[] s = addr.split(":");
        InetSocketAddress isa = new InetSocketAddress(s[0], Integer.parseInt(s[1]));
        return isa;
    }
    
    public static String parseChannelRemoteAddr(final Channel channel) {
        
        if (null == channel) {
            return "";
        }
        SocketAddress remote = channel.remoteAddress();
        final String addr = remote != null ? remote.toString() : "";
        
        if (addr.length() > 0) {
            int index = addr.lastIndexOf("/");
            if (index >= 0) {
                return addr.substring(index + 1);
            }
            
            return addr;
        }
        
        return "";
    }
    
    public static String parseSocketAddressAddr(SocketAddress socketAddress) {
        
        if (socketAddress != null) {
            final String addr = socketAddress.toString();
            
            if (addr.length() > 0) {
                return addr.substring(1);
            }
        }
        return "";
    }
    
}
