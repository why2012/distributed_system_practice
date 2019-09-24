package com.why.dubboconsumer.controller;

/**
 * Created by wanghaiyang on 2019/9/19.
 */

import com.why.springbootpractice.provider.DemoProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by wanghaiyang on 2019/9/17.
 */
@Component
public class ScheduledTaskController {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Autowired
    DemoProvider demoProvider;

    @Scheduled(fixedRate = 2000)
    public void reportCurrentTime() {
        System.out.println(demoProvider.say("CurrentTime: " + dateFormat.format(new Date())));
    }
}
