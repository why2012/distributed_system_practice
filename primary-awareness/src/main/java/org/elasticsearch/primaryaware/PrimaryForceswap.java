package org.elasticsearch.primaryaware;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.RestoreInProgress;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.routing.*;
import org.elasticsearch.cluster.routing.allocation.AllocationService;
import org.elasticsearch.cluster.routing.allocation.RoutingAllocation;
import org.elasticsearch.common.collect.ImmutableOpenMap;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static org.elasticsearch.cluster.routing.UnassignedInfo.INDEX_DELAYED_NODE_LEFT_TIMEOUT_SETTING;

public class PrimaryForceswap {
    private Logger logger;
    private ClusterState clusterState;
    private RoutingNodes routingNodes;
    private RoutingAllocation allocation;
    private AllocationService allocationService;
    private final String primaryAwareAttribute;
    private final String primaryAwareAttributeValue;

    public PrimaryForceswap(ClusterState currentClusterState, AllocationService allocationService,
                            final String primaryAwareAttribute, final String primaryAwareAttributeValue, Logger logger) {
        this.clusterState = currentClusterState;
        this.allocationService = allocationService;
        this.routingNodes = getMutableRoutingNodes(currentClusterState);
        this.allocation = new RoutingAllocation(null, routingNodes, clusterState, null, currentNanoTime());
        this.primaryAwareAttribute = primaryAwareAttribute;
        this.primaryAwareAttributeValue = primaryAwareAttributeValue;
        this.logger = logger;
    }

    public ClusterState forceSwap() {
        ClusterState newClusterState = clusterState;

        relocatePrimaryShards();

        if (allocation.routingNodesChanged()) {
            newClusterState = buildResult(clusterState, allocation);
        }

        if (this.allocationService != null) {
            return this.allocationService.reroute(newClusterState, "primary force swap");
        }

        return newClusterState;
    }

    private void relocatePrimaryShards() {
        List<ShardRouting> primaryShards = allocation.routingNodes().shards(buildShardFilter());
        for (ShardRouting waitSwapShard: primaryShards) {
            RoutingNode node = allocation.routingNodes().node(waitSwapShard.currentNodeId());
            final IndexMetaData indexMetaData = allocation.metaData().getIndexSafe(waitSwapShard.index());
            boolean delayed = INDEX_DELAYED_NODE_LEFT_TIMEOUT_SETTING.get(indexMetaData.getSettings()).nanos() > 0;
            UnassignedInfo unassignedInfo = new UnassignedInfo(UnassignedInfo.Reason.MANUAL_ALLOCATION, "primary force swap [" + node.nodeId() + "]",
                    null, 0, allocation.getCurrentNanoTime(), System.currentTimeMillis(), delayed, UnassignedInfo.AllocationStatus.NO_ATTEMPT);
            unassignPrimaryAndPromoteActiveReplicaIfExists(waitSwapShard, unassignedInfo);
        }
    }

    private Predicate<ShardRouting> buildShardFilter() {
        Predicate<ShardRouting> mustPrimary = (shard) -> shard.primary();
        Predicate<ShardRouting> mustAssigned = (shard) -> shard.assignedToNode() &&
                routingNodes.node(shard.currentNodeId()) != null &&
                routingNodes.node(shard.currentNodeId()).node() != null;
        Predicate<ShardRouting> mustActive = (shard) -> shard.active();
        Predicate<ShardRouting> mustInStaleZone = (shard) ->
                !StringUtils.equalsIgnoreCase(primaryAwareAttributeValue,
                        routingNodes.node(shard.currentNodeId()).node().getAttributes().get(primaryAwareAttribute));
        return mustPrimary.and(mustAssigned).and(mustActive).and(mustInStaleZone);
    }

    private void unassignPrimaryAndPromoteActiveReplicaIfExists(ShardRouting waitSwapShard, UnassignedInfo unassignedInfo) {
        ShardRoutingHelper waitSwapShardHelper = new ShardRoutingHelper(waitSwapShard, routingNodes);
        ShardRouting candidateReplica = waitSwapShardHelper.findCandidateReplica(primaryAwareAttribute, primaryAwareAttributeValue);
        if (candidateReplica == null) {
            // do nothing
            // ShardRouting unassignedShardRouting = waitSwapShardHelper.turnToUnassigned(unassignedInfo);
            // allocation.changes().unassignedInfoUpdated(unassignedShardRouting, unassignedInfo);
        } else {
            ShardRouting unassignedShardRouting = waitSwapShardHelper.turnToUnassignedReplica(unassignedInfo);
            allocation.changes().unassignedInfoUpdated(unassignedShardRouting, unassignedInfo);
            ShardRouting newPrimary = new ShardRoutingHelper(candidateReplica, routingNodes).turnToActivePrimary();
            allocation.changes().replicaPromoted(newPrimary);
            allocation.changes().shardFailed(waitSwapShard, unassignedInfo);
        }
    }

    private RoutingNodes getMutableRoutingNodes(ClusterState clusterState) {
        RoutingNodes routingNodes = new RoutingNodes(clusterState, false); // this is a costly operation - only call this once!
        return routingNodes;
    }

    private ClusterState buildResult(ClusterState oldState, RoutingAllocation allocation) {
        final RoutingTable oldRoutingTable = oldState.routingTable();
        final RoutingNodes newRoutingNodes = allocation.routingNodes();
        final RoutingTable newRoutingTable = new RoutingTable.Builder().updateNodes(oldRoutingTable.version(), newRoutingNodes).build();
        final MetaData newMetaData = allocation.updateMetaDataWithRoutingChanges(newRoutingTable);
        assert newRoutingTable.validate(newMetaData); // validates the routing table is coherent with the cluster state metadata

        final ClusterState.Builder newStateBuilder = ClusterState.builder(oldState)
                .routingTable(newRoutingTable)
                .metaData(newMetaData);
        final RestoreInProgress restoreInProgress = allocation.custom(RestoreInProgress.TYPE);
        if (restoreInProgress != null) {
            RestoreInProgress updatedRestoreInProgress = allocation.updateRestoreInfoWithRoutingChanges(restoreInProgress);
            if (updatedRestoreInProgress != restoreInProgress) {
                ImmutableOpenMap.Builder<String, ClusterState.Custom> customsBuilder = ImmutableOpenMap.builder(allocation.getCustoms());
                customsBuilder.put(RestoreInProgress.TYPE, updatedRestoreInProgress);
                newStateBuilder.customs(customsBuilder.build());
            }
        }
        return newStateBuilder.build();
    }

    protected long currentNanoTime() {
        return System.nanoTime();
    }
}
