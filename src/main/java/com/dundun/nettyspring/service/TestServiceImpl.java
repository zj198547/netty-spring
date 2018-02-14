package com.dundun.nettyspring.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;

import com.dundun.nettyspring.event.TestEvent;

/**
* TODO(这里用一句话描述这个类的作用)
* @author dundun
* @date 2018年2月12日
*
*/
@Service
public class TestServiceImpl implements ApplicationEventPublisherAware {
    
    private ApplicationEventPublisher publisher;
    
    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        
        this.publisher = applicationEventPublisher;
        
    }
    
    public void testEvent() {
        
        TestEvent event = new TestEvent("test", "test");
        publisher.publishEvent(event);;
    }
    
}
