package org.elasticsearch.primaryaware;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateTaskExecutor;
import org.elasticsearch.cluster.ClusterStateTaskListener;
import org.elasticsearch.cluster.coordination.NodeRemovalClusterStateTaskExecutor;
import org.elasticsearch.cluster.node.DiscoveryNode;

import java.util.List;

public class PrimarySwapClusterStateTaskExecutor implements ClusterStateTaskExecutor<PrimarySwapClusterStateTaskExecutor.Task>,
        ClusterStateTaskListener  {

    private Logger logger;

    public static class Task {

        private final String primaryAwareAttribute;
        private final String primaryAwareAttributeValue;
        private final String reason;

        public Task(final String primaryAwareAttribute, final String primaryAwareAttributeValue, final String reason) {
            this.primaryAwareAttribute = primaryAwareAttribute;
            this.primaryAwareAttributeValue = primaryAwareAttributeValue;
            this.reason = reason;
        }

        public String primaryAwareAttribute() {
            return primaryAwareAttribute;
        }

        public String primaryAwareAttributeValue() {return primaryAwareAttributeValue;}

        public String reason() {
            return reason;
        }

        @Override
        public String toString() {
            return "primaryAwareAttribute: " + primaryAwareAttribute + " value: " + primaryAwareAttributeValue + " reason: " + reason;
        }
    }

    public PrimarySwapClusterStateTaskExecutor(Logger logger) {
        this.logger = logger;
    }

    @Override
    public ClusterTasksResult<PrimarySwapClusterStateTaskExecutor.Task> execute(ClusterState currentState, List<PrimarySwapClusterStateTaskExecutor.Task> tasks) throws Exception {
        final ClusterTasksResult.Builder<PrimarySwapClusterStateTaskExecutor.Task> resultBuilder = ClusterTasksResult.<PrimarySwapClusterStateTaskExecutor.Task>builder().successes(tasks);
        ClusterState newClusterState = new PrimaryForceswap(currentState, tasks.get(0).primaryAwareAttribute,
                tasks.get(0).primaryAwareAttributeValue, logger).forceSwap();
        return resultBuilder.build(newClusterState);
    }

    @Override
    public void onFailure(final String source, final Exception e) {
        logger.error(() -> new ParameterizedMessage("unexpected failure during [{}]", source), e);
    }

    @Override
    public void onNoLongerMaster(String source) {
        logger.debug("no longer master while processing primary swap [{}]", source);
    }
}
