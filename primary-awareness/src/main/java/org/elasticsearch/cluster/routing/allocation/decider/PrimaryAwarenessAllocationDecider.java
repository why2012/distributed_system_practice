package org.elasticsearch.cluster.routing.allocation.decider;

import org.elasticsearch.cluster.routing.RoutingNode;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.allocation.RoutingAllocation;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;

public class PrimaryAwarenessAllocationDecider extends AllocationDecider {

    public static final String NAME = "primary_awareness";
    private static final String DELIMITER = ":";

    public static final Setting<String> CLUSTER_ROUTING_ALLOCATION_PRIMARY_AWARENESS_ATTRIBUTE =
            Setting.simpleString("cluster.routing.allocation.awareness.primary.attribute", PrimaryAwarenessAllocationDecider::validatePrimaryAwarenessSetting,
                    Setting.Property.Dynamic, Setting.Property.NodeScope);

    private PrimaryAwarenessSettingUpdateListener primaryAwarenessSettingUpdateListener;
    private volatile String primaryAwarenessAttribute;
    private volatile String primaryAwarenessAttributeValue;

    public PrimaryAwarenessAllocationDecider(Settings settings, ClusterSettings clusterSettings,
                                             PrimaryAwarenessSettingUpdateListener primaryAwarenessSettingUpdateListener) {
        setPrimaryAwarenessAttribute(CLUSTER_ROUTING_ALLOCATION_PRIMARY_AWARENESS_ATTRIBUTE.get(settings));
        clusterSettings.addSettingsUpdateConsumer(CLUSTER_ROUTING_ALLOCATION_PRIMARY_AWARENESS_ATTRIBUTE, this::setPrimaryAwarenessAttribute,
                PrimaryAwarenessAllocationDecider::validatePrimaryAwarenessSetting);
        this.primaryAwarenessSettingUpdateListener = primaryAwarenessSettingUpdateListener;
    }

    private void setPrimaryAwarenessAttribute(String primaryAwarenessAttributeAndValueStr) {
        if (Strings.isEmpty(primaryAwarenessAttributeAndValueStr)) {
            return;
        }
        String[] primaryAwarenessAttributeAndValueArr = Strings.split(primaryAwarenessAttributeAndValueStr, DELIMITER);
        this.primaryAwarenessAttribute = primaryAwarenessAttributeAndValueArr[0].trim();
        this.primaryAwarenessAttributeValue = primaryAwarenessAttributeAndValueArr[1].trim();
        this.primaryAwarenessSettingUpdateListener.onPrimaryAwarenessAttributeUpdate(primaryAwarenessAttribute, primaryAwarenessAttributeValue);
    }

    private static boolean validatePrimaryAwarenessSetting(String primaryAwarenessAttributeAndValueStr) {
        if (Strings.isEmpty(primaryAwarenessAttributeAndValueStr)) {
            return false;
        }
        String[] primaryAwarenessAttributeAndValueArr = Strings.split(primaryAwarenessAttributeAndValueStr, DELIMITER);
        if (primaryAwarenessAttributeAndValueArr == null || primaryAwarenessAttributeAndValueArr.length < 2) {
            throw new IllegalArgumentException(String.format("Illegal primary attribute value: %s, should in this format -> attr:value",
                    primaryAwarenessAttributeAndValueStr));
        }
        return true;
    }

    @Override
    public Decision canAllocate(ShardRouting shardRouting, RoutingNode node, RoutingAllocation allocation) {
        return inRightArea(shardRouting, node, allocation, true);
    }

    @Override
    public Decision canRemain(ShardRouting shardRouting, RoutingNode node, RoutingAllocation allocation) {
        return inRightArea(shardRouting, node, allocation, false);
    }

    private Decision inRightArea(ShardRouting shardRouting, RoutingNode node, RoutingAllocation allocation, boolean moveToNode) {
        if (Strings.isEmpty(primaryAwarenessAttribute) || Strings.isEmpty(primaryAwarenessAttributeValue)) {
            return allocation.decision(Decision.YES, NAME,
                    "primary allocation awareness is not enabled, set cluster setting [%s] to enable it",
                    CLUSTER_ROUTING_ALLOCATION_PRIMARY_AWARENESS_ATTRIBUTE.getKey());
        }

        if (node.node().getAttributes().containsKey(primaryAwarenessAttribute) == false) {
            return allocation.decision(Decision.NO, NAME,
                    "node does not contain the awareness attribute [%s]; required attributes cluster setting [%s]",
                    primaryAwarenessAttribute, CLUSTER_ROUTING_ALLOCATION_PRIMARY_AWARENESS_ATTRIBUTE.getKey());
        }
        String nodePrimaryAwarenessAttributeValue = node.node().getAttributes().get(primaryAwarenessAttribute);

        // unassigned replica shard
        // if (!shardRouting.primary() && !shardRouting.assignedToNode()) {
        //    return allocation.decision(Decision.YES, NAME, "node meets all awareness attribute requirements");
        // }
        if (!shardRouting.primary()) {
            return allocation.decision(Decision.YES, NAME, "node meets all awareness attribute requirements");
        }

        if (Strings.isEmpty(nodePrimaryAwarenessAttributeValue)) {
            return allocation.decision(Decision.NO, NAME, "node primary awareness value is empty");
        }

        if (!nodePrimaryAwarenessAttributeValue.equals(primaryAwarenessAttributeValue)) {
            return allocation.decision(Decision.NO, NAME, "node primary awareness value is %s, expect: %s",
                    nodePrimaryAwarenessAttributeValue, primaryAwarenessAttributeValue);
        }

        return allocation.decision(Decision.YES, NAME, "node meets all awareness attribute requirements");
    }
}
