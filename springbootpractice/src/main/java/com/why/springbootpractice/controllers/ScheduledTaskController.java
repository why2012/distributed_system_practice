package com.why.springbootpractice.controllers;

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

    @Scheduled(fixedRate = 2000)
    public void reportCurrentTime() {
        System.out.println("CurrentTime: " + dateFormat.format(new Date()));
    }
}
