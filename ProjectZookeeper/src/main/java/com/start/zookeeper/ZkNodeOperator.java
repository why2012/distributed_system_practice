package com.start.zookeeper;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Created by wanghaiyang on 2019/9/21.
 */
public class ZkNodeOperator implements Watcher {
    private static final Logger logger = LoggerFactory.getLogger(ZkReconnect.class);
    private CountDownLatch countDownLatch = new CountDownLatch(2);
    private ZooKeeper zooKeeper = null;

    public ZkNodeOperator() {}

    public ZkNodeOperator(String connectString) {
        try {
            zooKeeper = new ZooKeeper(connectString, 5000, this);
        } catch (IOException e) {
            e.printStackTrace();
            if (zooKeeper != null) {
                try {
                    zooKeeper.close();
                } catch (InterruptedException interrE) {
                    interrE.printStackTrace();
                }
            }
        }
    }

    public void createNode(String path, byte[] data) {
        try {
            String result = zooKeeper.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            logger.debug("Create node {} successfully, result {}", path, result);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void createNodeAsync(String path, byte[] data, AsyncCallback.StringCallback createCallback) {
        zooKeeper.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL, createCallback, "create successfully");
    }

    public static void main(String[] args) throws Exception {
        ZkNodeOperator zkNodeOperator = new ZkNodeOperator("127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183");

        zkNodeOperator.createNode("/test/xyz", "abc".getBytes());
        zkNodeOperator.createNodeAsync("/test/uvw", "xyz".getBytes(), (rc, path, ctx, name) ->
            {
                logger.debug("Create Async callback: {}", String.valueOf(rc) + ", " + path + ", " + (String)ctx + ", " + name);
                zkNodeOperator.countDownLatch.countDown();
            });
        Stat stat = zkNodeOperator.getZooKeeper().setData("/test/uvw", "123".getBytes(), 0);
        logger.debug("Current node dataVersion: {}", stat.getVersion());

        byte[] resData = zkNodeOperator.getZooKeeper().getData("/test/xyz", true, stat);
        String data = new String(resData);
        logger.debug("Node {} data is: {}, version is {}", new Object[] {"/test/xyz", data, String.valueOf(stat.getVersion())});

        zkNodeOperator.countDownLatch.await();
    }

    public ZooKeeper getZooKeeper() {
        return zooKeeper;
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        logger.debug("Received watcher notification: {}", watchedEvent);
        try {
            switch (watchedEvent.getType()) {
                case NodeDataChanged:
                    Stat resStat = new Stat();
                    byte[] byteData = this.zooKeeper.getData("/test/xyz", false, resStat);
                    String data = new String(byteData);
                    logger.debug("Changed data: {}, current version: {}", data, resStat.getVersion());
                    countDownLatch.countDown();
                    break;
                case NodeCreated:
                    break;
                case NodeChildrenChanged:
                    break;
                case NodeDeleted:
                    break;
                default:
            }
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }
}
