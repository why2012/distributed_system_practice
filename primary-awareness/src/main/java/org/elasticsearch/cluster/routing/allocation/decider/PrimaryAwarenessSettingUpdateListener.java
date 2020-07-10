package org.elasticsearch.cluster.routing.allocation.decider;

public interface PrimaryAwarenessSettingUpdateListener {

    void onPrimaryAwarenessAttributeUpdate(String primaryAwarenessAttribute, String primaryAwarenessAttributeValue);
}
