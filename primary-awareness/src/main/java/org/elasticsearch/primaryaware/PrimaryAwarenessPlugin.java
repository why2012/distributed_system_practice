package org.elasticsearch.primaryaware;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.allocation.decider.AllocationDecider;
import org.elasticsearch.cluster.routing.allocation.decider.PrimaryAwarenessAllocationDecider;
import org.elasticsearch.cluster.routing.allocation.decider.PrimaryAwarenessSettingUpdateListener;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.*;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.ClusterPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.primaryaware.action.PrimaryShardSwapAction;
import org.elasticsearch.primaryaware.action.TransportPrimaryShardSwapAction;
import org.elasticsearch.primaryaware.action.bulk.AsyncBulkAction;
import org.elasticsearch.primaryaware.action.bulk.TransportAsyncBulkAction;
import org.elasticsearch.primaryaware.action.bulk.TransportAsyncShardBulkAction;
import org.elasticsearch.primaryaware.rest.RestPrimaryShardSwapAction;
import org.elasticsearch.primaryaware.rest.bulk.RestAsyncBulkAction;
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
    private static final Logger logger = LogManager.getLogger(PrimaryAwarenessPlugin.class);
    public static final Setting<Boolean> CLUSTER_ROUTING_ALLOCATION_PRIMARY_AWARENESS_FORCESWAP =
            Setting.boolSetting("cluster.routing.allocation.awareness.primary.forceswap", false,
                    Setting.Property.Dynamic, Setting.Property.NodeScope);
    public static final Setting<Boolean> PRIMARY_AWARENESS_ENABLED_SETTING =
            Setting.boolSetting("primary.awareness.enabled", true, Setting.Property.NodeScope);

    private Settings settings;
    private ClusterService clusterService;
    private PrimaryAwarenessSettingStore primaryAwarenessSettingStore;
    private PrimarySwapClusterStateTaskExecutor primarySwapClusterStateTaskExecutor;
    private PrimaryForceswapTrigger primaryForceswapTrigger;
    private boolean enable;

    public PrimaryAwarenessPlugin(Settings settings) {
        this.settings = settings;
        this.primaryAwarenessSettingStore = new PrimaryAwarenessSettingStore();
        this.primarySwapClusterStateTaskExecutor = new PrimarySwapClusterStateTaskExecutor(logger);
        this.primaryAwarenessSettingStore.updatePrimaryForceswap(CLUSTER_ROUTING_ALLOCATION_PRIMARY_AWARENESS_FORCESWAP.get(settings));
        enable = PRIMARY_AWARENESS_ENABLED_SETTING.get(settings);
    }

    @Override
    public List<RestHandler> getRestHandlers(Settings settings, RestController restController, ClusterSettings clusterSettings,
                                             IndexScopedSettings indexScopedSettings, SettingsFilter settingsFilter,
                                             IndexNameExpressionResolver indexNameExpressionResolver,
                                             Supplier<DiscoveryNodes> nodesInCluster) {
        if (enable == false) {
            return Collections.emptyList();
        }
        return Arrays.asList(
                new RestPrimaryShardSwapAction(settings, restController, primaryAwarenessSettingStore),
                new RestAsyncBulkAction(settings, restController));
    }

    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        if (enable == false) {
            return Collections.emptyList();
        }
        return Arrays.asList(
                new ActionHandler<>(PrimaryShardSwapAction.INSTANCE, TransportPrimaryShardSwapAction.class),
                new ActionHandler<>(AsyncBulkAction.INSTANCE, TransportAsyncBulkAction.class, TransportAsyncShardBulkAction.class));
    }

    @Override
    public Collection<AllocationDecider> createAllocationDeciders(Settings settings, ClusterSettings clusterSettings) {
        if (enable == false) {
            return Collections.emptyList();
        }
        clusterSettings.addSettingsUpdateConsumer(CLUSTER_ROUTING_ALLOCATION_PRIMARY_AWARENESS_FORCESWAP,
                primaryAwarenessSettingStore::updatePrimaryForceswap);
        return Collections.singletonList(new PrimaryAwarenessAllocationDecider(settings, clusterSettings, primaryAwarenessSettingStore));
    }

    @Override
    public Collection<Object> createComponents(Client client, ClusterService clusterService, ThreadPool threadPool,
                                               ResourceWatcherService resourceWatcherService, ScriptService scriptService,
                                               NamedXContentRegistry xContentRegistry, Environment environment,
                                               NodeEnvironment nodeEnvironment, NamedWriteableRegistry namedWriteableRegistry) {
        this.clusterService = clusterService;
        primaryForceswapTrigger = new PrimaryForceswapTrigger(clusterService, primarySwapClusterStateTaskExecutor, logger);
        return Collections.emptyList();
    }

    @Override
    public Collection<Module> createGuiceModules() {
        if (enable == false) {
            return Collections.emptyList();
        }
        return Collections.singleton(b -> {
            b.bind(PrimaryForceswapTrigger.class).toInstance(primaryForceswapTrigger);
            b.bind(PrimaryAwarenessSettingStore.class).toInstance(primaryAwarenessSettingStore);
        });
    }

    @Override
    public List<Setting<?>> getSettings() {
        if (enable == false) {
            return Collections.emptyList();
        }
        return Arrays.asList(
                CLUSTER_ROUTING_ALLOCATION_PRIMARY_AWARENESS_FORCESWAP,
                PRIMARY_AWARENESS_ENABLED_SETTING,
                PrimaryAwarenessAllocationDecider.CLUSTER_ROUTING_ALLOCATION_PRIMARY_AWARENESS_ATTRIBUTE
        );
    }

    public class PrimaryAwarenessSettingStore implements PrimaryAwarenessSettingUpdateListener {
        private volatile String primaryAwarenessAttribute;
        private volatile String primaryAwarenessAttributeValue;
        private volatile Boolean primaryForceswap;
        private volatile Boolean ignoreFirst = false;

        @Override
        public void onPrimaryAwarenessAttributeUpdate(String primaryAwarenessAttribute, String primaryAwarenessAttributeValue) {
            String oldPrimaryAwarenessAttribute = this.primaryAwarenessAttribute;
            String oldPrimaryAwarenessAttributeValue = this.primaryAwarenessAttributeValue;
            this.primaryAwarenessAttribute = primaryAwarenessAttribute;
            this.primaryAwarenessAttributeValue = primaryAwarenessAttributeValue;
            // primary aware settings change
            if (primaryForceswap && ignoreFirst) {
                primaryForceswapTrigger.trigger(primaryAwarenessAttribute, primaryAwarenessAttributeValue);
            }
            ignoreFirst = true;
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
