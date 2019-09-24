package com.start.zookeeper;


import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by wanghaiyang on 2019/9/21.
 */
public class ZkConnect implements Watcher {

    private static final Logger logger = LoggerFactory.getLogger(ZkConnect.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        ZooKeeper zk = new ZooKeeper("127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183", 5000, new ZkConnect());

        logger.debug("Connection state: {}", zk.getState());

        TimeUnit.SECONDS.sleep(2);

        logger.debug("Connection state: {}", zk.getState());

    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        logger.debug("Received watcher notification: {}", watchedEvent);
    }
}
