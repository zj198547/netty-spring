package com.dundun.nettyspring.event;

import org.springframework.context.ApplicationEvent;

public class TestEvent extends ApplicationEvent {
    
    private static final long serialVersionUID = 461404066622737544L;
    
    //定义事件的核心成员
    @SuppressWarnings("unused")
    private String            test;
    
    public TestEvent(Object source, String test) {
        //source字面意思是根源，意指发送事件的根源，即我们的事件发布者
        super(source);
        this.test = test;
    }
    
}
