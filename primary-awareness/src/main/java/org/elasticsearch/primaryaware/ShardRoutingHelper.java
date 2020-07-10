package org.elasticsearch.primaryaware;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.cluster.coordination.Coordinator;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.routing.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;

public class ShardRoutingHelper {
    private static final Logger logger = LogManager.getLogger(Coordinator.class);

    private final ShardRouting targetShard;
    private RoutingNodes routingNodes;

    private Method moveToUnassigned;
    private Method movePrimaryToUnassignedAndDemoteToReplica;
    private Method promoteActiveReplicaShardToPrimary;

    public ShardRoutingHelper(ShardRouting targetShard, RoutingNodes routingNodes) {
        this.targetShard = targetShard;
        this.routingNodes = routingNodes;

        try {
            this.moveToUnassigned = RoutingNodes.class.getDeclaredMethod("moveToUnassigned", ShardRouting.class, UnassignedInfo.class);
            this.moveToUnassigned.setAccessible(true);

            this.movePrimaryToUnassignedAndDemoteToReplica = RoutingNodes.class.getDeclaredMethod(
                    "movePrimaryToUnassignedAndDemoteToReplica", ShardRouting.class, UnassignedInfo.class);
            this.movePrimaryToUnassignedAndDemoteToReplica.setAccessible(true);

            this.promoteActiveReplicaShardToPrimary = RoutingNodes.class.getDeclaredMethod(
                    "promoteActiveReplicaShardToPrimary", ShardRouting.class);
            this.promoteActiveReplicaShardToPrimary.setAccessible(true);
        } catch (Exception e) {
            logger.error("turnToReplica: " + targetShard, e);
        }
    }

    public ShardRouting turnToUnassignedReplica(UnassignedInfo unassignedInfo) {
        // node.remove
        // assigned.remove
        // unassgined.add
        return movePrimaryToUnassignedAndDemoteToReplica(targetShard, unassignedInfo);
    }

    public void assignToNode(String nodeId, RoutingChangesObserver routingChangesObserver) {
        // node.add
        // assigned.add
        assert targetShard.unassigned(): "target shard is not unassigned";
        String allocationId = targetShard.allocationId() == null ? null : targetShard.allocationId().getId();
        routingNodes.initializeShard(targetShard, nodeId, allocationId,
                ShardRouting.UNAVAILABLE_EXPECTED_SHARD_SIZE, routingChangesObserver);
    }

    public ShardRouting turnToUnassigned(UnassignedInfo unassignedInfo) {
        return moveToUnassigned(targetShard, unassignedInfo);
    }

    public ShardRouting turnToActivePrimary() {
        return promoteActiveReplicaShardToPrimary(targetShard);
    }

    public ShardRouting directTurnToPrimary() {
        try {
            assert !targetShard.primary() && targetShard.active() : "target shard must be replica and active";
            Field primary = ShardRouting.class.getDeclaredField("primary");
            primary.setAccessible(true);
            primary.set(targetShard, true);
        } catch (Exception e) {
            logger.error("directTurnToPrimary: " + targetShard, e);
        }
        return targetShard;
    }

    public ShardRouting directTurnToReplica() {
        try {
            assert targetShard.primary() && targetShard.active() : "target shard must be primary and active";
            Field primary = ShardRouting.class.getDeclaredField("primary");
            primary.setAccessible(true);
            primary.set(targetShard, false);
        } catch (Exception e) {
            logger.error("directTurnToReplica: " + targetShard, e);
        }
        return targetShard;
    }

    public ShardRouting findCandidateReplica(String primaryAwareAttribute, String primaryAwareAttributeValue) {
        return routingNodes.assignedShards(targetShard.shardId()).stream()
                .filter(shr -> !shr.primary() && shr.active())
                .filter(shr -> routingNodes.node(shr.currentNodeId()) != null && routingNodes.node(shr.currentNodeId()).node() != null)
                .filter(shr -> StringUtils.equalsIgnoreCase(
                        routingNodes.node(shr.currentNodeId()).node().getAttributes().get(primaryAwareAttribute),
                        primaryAwareAttributeValue))
                .max(Comparator.comparing(shr -> routingNodes.node(shr.currentNodeId()).node(),
                        Comparator.nullsFirst(Comparator.comparing(DiscoveryNode::getVersion))))
                .orElse(null);
    }

    private ShardRouting moveToUnassigned(ShardRouting shardRouting, UnassignedInfo unassignedInfo) {
        ShardRouting newPrimary = null;
        try {
            newPrimary = (ShardRouting) moveToUnassigned.invoke(routingNodes, shardRouting, unassignedInfo);
        } catch (Exception e) {
            logger.error("moveToUnassigned: " + targetShard, e);
        }
        return newPrimary;
    }

    private ShardRouting movePrimaryToUnassignedAndDemoteToReplica(ShardRouting shardRouting, UnassignedInfo unassignedInfo) {
        ShardRouting newPrimary = null;
        try {
            newPrimary = (ShardRouting) movePrimaryToUnassignedAndDemoteToReplica.invoke(routingNodes, shardRouting, unassignedInfo);
        } catch (Exception e) {
            logger.error("movePrimaryToUnassignedAndDemoteToReplica: " + targetShard, e);
        }
        return newPrimary;
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
