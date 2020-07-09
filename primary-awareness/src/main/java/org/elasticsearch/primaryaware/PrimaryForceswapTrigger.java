package org.elasticsearch.primaryaware;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.cluster.ClusterStateTaskConfig;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.cluster.service.MasterService;
import org.elasticsearch.common.Priority;

public class PrimaryForceswapTrigger {
    private Logger logger;
    private Object mutex;
    private ClusterService clusterService;
    private MasterService masterService;
    private PrimarySwapClusterStateTaskExecutor primarySwapClusterStateTaskExecutor;

    public PrimaryForceswapTrigger(ClusterService clusterService, PrimarySwapClusterStateTaskExecutor executor, Logger logger) {
        this.clusterService = clusterService;
        this.masterService = clusterService.getMasterService();
        this.primarySwapClusterStateTaskExecutor = executor;
        this.logger = logger;
    }

    public void trigger(final String primaryAwareAttribute, final String primaryAwareAttributeValue) {
        DiscoveryNode masterNode = clusterService.state().nodes().getMasterNode();
        DiscoveryNode localNode = clusterService.localNode();
        if (masterNode.equals(localNode)) {
            synchronized (mutex) {
                clusterService.getMasterService().submitStateUpdateTask("primary-awareness-settings-change",
                        new PrimarySwapClusterStateTaskExecutor.Task(primaryAwareAttribute,
                                primaryAwareAttributeValue, "primary-awareness-settings-change"),
                        ClusterStateTaskConfig.build(Priority.IMMEDIATE),
                        primarySwapClusterStateTaskExecutor,
                        primarySwapClusterStateTaskExecutor);
            }
        }
    }
}
