package com.dundun.nettyspring.event.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.dundun.nettyspring.event.TestEvent;

@Component
public class TestListener {
    
    @Async
    @EventListener
    public void onApplicationEvent(TestEvent event) {
        
        // TODO Auto-generated method stub
        String source = (String)event.getSource();
        System.out.println("doing something ... source = " + source);
    }
    
}
