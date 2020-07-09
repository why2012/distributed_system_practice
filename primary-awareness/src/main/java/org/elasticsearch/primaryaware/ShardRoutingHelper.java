package org.elasticsearch.primaryaware;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.cluster.coordination.Coordinator;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.routing.RoutingNode;
import org.elasticsearch.cluster.routing.RoutingNodes;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.UnassignedInfo;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;

public class ShardRoutingHelper {
    private static final Logger logger = LogManager.getLogger(Coordinator.class);

    private final ShardRouting targetShard;
    private final RoutingNode localNode;
    private RoutingNodes routingNodes;

    private RoutingNodes.UnassignedShards unassignedShards;
    private Method removeShard;
    private Method promoteActiveReplicaShardToPrimary;

    public ShardRoutingHelper(ShardRouting targetShard, RoutingNode localNode, RoutingNodes routingNodes) {
        this.targetShard = targetShard;
        this.localNode = localNode;
        this.routingNodes = routingNodes;

        try {
            Field field = RoutingNodes.class.getDeclaredField("unassignedShards");
            field.setAccessible(true);
            this.unassignedShards = (RoutingNodes.UnassignedShards)field.get(routingNodes);

            this.removeShard =  RoutingNodes.class.getDeclaredMethod("remove");
            this.removeShard.setAccessible(true);

            this.promoteActiveReplicaShardToPrimary = RoutingNodes.class.getDeclaredMethod("promoteReplicaToPrimary");
            this.promoteActiveReplicaShardToPrimary.setAccessible(true);
        } catch (Exception e) {
            logger.error("turnToReplica: " + targetShard, e);
        }
    }

    public void turnToReplica(UnassignedInfo unassignedInfo) {
        assert targetShard.primary() : "only primary can be demoted to replica (" + targetShard + ")";
        remove(targetShard);
        ShardRouting unassigned = targetShard.moveToUnassigned(unassignedInfo).moveUnassignedFromPrimary();
        unassignedShards.add(unassigned);
    }

    public ShardRouting turnToPrimary() {
        return promoteActiveReplicaShardToPrimary(targetShard);
    }

    public ShardRouting findCandidateReplica(String primaryAwareAttribute, String primaryAwareAttributeValue) {
        return routingNodes.assignedShards(targetShard.shardId()).stream()
                .filter(shr -> !shr.primary() && shr.active())
                .filter(shr -> routingNodes.node(shr.currentNodeId()) != null)
                .max(Comparator.comparing(shr -> routingNodes.node(shr.currentNodeId()).node(),
                        Comparator.nullsFirst(Comparator.comparing(DiscoveryNode::getVersion))))
                .orElse(null);
    }

    private void remove(ShardRouting shardRouting) {
        try {
            removeShard.invoke(routingNodes, shardRouting);
        } catch (Exception e) {
            logger.error("removeShard: " + targetShard, e);
        }
    }

    private ShardRouting promoteActiveReplicaShardToPrimary(ShardRouting activeReplica) {
        assert activeReplica.started() : "replica relocation should have been cancelled: " + activeReplica;
        ShardRouting newPrimary = null;
        try {
            newPrimary = (ShardRouting) promoteActiveReplicaShardToPrimary.invoke(routingNodes, activeReplica);
        } catch (Exception e) {
            logger.error("promoteActiveReplicaShardToPrimary: " + activeReplica, e);
        }
        return newPrimary;
    }
}
