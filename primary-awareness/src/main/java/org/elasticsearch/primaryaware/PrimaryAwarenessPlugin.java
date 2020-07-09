package org.elasticsearch.primaryaware;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterStateTaskConfig;
import org.elasticsearch.cluster.coordination.Coordinator;
import org.elasticsearch.cluster.coordination.NodeRemovalClusterStateTaskExecutor;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.allocation.decider.AllocationDecider;
import org.elasticsearch.cluster.routing.allocation.decider.PrimaryAwarenessAllocationDecider;
import org.elasticsearch.cluster.routing.allocation.decider.PrimaryAwarenessSettingUpdateListener;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.Priority;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.ClusterPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class PrimaryAwarenessPlugin extends Plugin implements ActionPlugin, ClusterPlugin {
    private static final Logger logger = LogManager.getLogger(Coordinator.class);
    public static final Setting<Boolean> CLUSTER_ROUTING_ALLOCATION_PRIMARY_AWARENESS_FORCESWAP =
            Setting.boolSetting("cluster.routing.allocation.awareness.primary.forceswap", true,
                    Setting.Property.Dynamic, Setting.Property.NodeScope);

    private Settings settings;
    private ClusterService clusterService;
    private PrimaryAwarenessSettingStore primaryAwarenessSettingStore;
    private PrimarySwapClusterStateTaskExecutor primarySwapClusterStateTaskExecutor;
    private PrimaryForceswapTrigger primaryForceswapTrigger;

    public PrimaryAwarenessPlugin(Settings settings) {
        this.settings = settings;
        this.primaryAwarenessSettingStore = new PrimaryAwarenessSettingStore();
        this.primarySwapClusterStateTaskExecutor = new PrimarySwapClusterStateTaskExecutor(logger);
    }

    @Override
    public List<RestHandler> getRestHandlers(Settings settings, RestController restController, ClusterSettings clusterSettings,
                                             IndexScopedSettings indexScopedSettings, SettingsFilter settingsFilter,
                                             IndexNameExpressionResolver indexNameExpressionResolver,
                                             Supplier<DiscoveryNodes> nodesInCluster) {
        clusterSettings.addSettingsUpdateConsumer(CLUSTER_ROUTING_ALLOCATION_PRIMARY_AWARENESS_FORCESWAP,
                primaryAwarenessSettingStore::updatePrimaryForceswap);
        return Collections.emptyList();
    }

    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        return Collections.emptyList();
    }

    @Override
    public Collection<AllocationDecider> createAllocationDeciders(Settings settings, ClusterSettings clusterSettings) {
        return Collections.singletonList(new PrimaryAwarenessAllocationDecider(settings, clusterSettings, primaryAwarenessSettingStore));
    }

    @Override
    public Collection<Object> createComponents(Client client, ClusterService clusterService, ThreadPool threadPool,
                                               ResourceWatcherService resourceWatcherService, ScriptService scriptService,
                                               NamedXContentRegistry xContentRegistry, Environment environment,
                                               NodeEnvironment nodeEnvironment, NamedWriteableRegistry namedWriteableRegistry,
                                               IndexNameExpressionResolver indexNameExpressionResolver) {
        this.clusterService = clusterService;
        primaryForceswapTrigger = new PrimaryForceswapTrigger(clusterService, primarySwapClusterStateTaskExecutor, logger);
        return Collections.emptyList();
    }

    @Override
    public List<Setting<?>> getSettings() {
        return Arrays.asList(
                PrimaryAwarenessAllocationDecider.CLUSTER_ROUTING_ALLOCATION_PRIMARY_AWARENESS_ATTRIBUTE
        );
    }

    class PrimaryAwarenessSettingStore implements PrimaryAwarenessSettingUpdateListener {
        private volatile String primaryAwarenessAttribute;
        private volatile String primaryAwarenessAttributeValue;
        private volatile Boolean primaryForceswap;

        @Override
        public void onPrimaryAwarenessAttributeUpdate(String primaryAwarenessAttribute, String primaryAwarenessAttributeValue) {
            String oldPrimaryAwarenessAttribute = this.primaryAwarenessAttribute;
            String oldPrimaryAwarenessAttributeValue = this.primaryAwarenessAttributeValue;
            this.primaryAwarenessAttribute = primaryAwarenessAttribute;
            this.primaryAwarenessAttributeValue = primaryAwarenessAttributeValue;
            // primary aware settings change
            if (primaryForceswap &&
                    (!oldPrimaryAwarenessAttribute.equals(primaryAwarenessAttribute) ||
                    !oldPrimaryAwarenessAttributeValue.equals(primaryAwarenessAttributeValue))) {
                primaryForceswapTrigger.trigger(primaryAwarenessAttribute, primaryAwarenessAttributeValue);
            }
        }

        private void updatePrimaryForceswap(boolean forceswap) {
            primaryForceswap = forceswap;
        }

        public String getPrimaryAwarenessAttribute() {
            return primaryAwarenessAttribute;
        }

        public String getPrimaryAwarenessAttributeValue() {
            return primaryAwarenessAttributeValue;
        }
    }
}
