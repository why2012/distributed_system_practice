package org.elasticsearch.primaryaware.rest;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.primaryaware.PrimaryAwarenessPlugin;
import org.elasticsearch.primaryaware.action.PrimaryShardSwapAction;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;

import java.io.IOException;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.elasticsearch.primaryaware.action.PrimaryShardSwapAction.PrimaryShardSwapRequest;
import static org.elasticsearch.rest.RestRequest.Method.POST;

public class RestPrimaryShardSwapAction extends BaseRestHandler {

    private PrimaryAwarenessPlugin.PrimaryAwarenessSettingStore primaryAwarenessSettingStore;

    public RestPrimaryShardSwapAction(PrimaryAwarenessPlugin.PrimaryAwarenessSettingStore primaryAwarenessSettingStore) {
        this.primaryAwarenessSettingStore = primaryAwarenessSettingStore;
    }

    @Override
    public String getName() {
        return "primary_shard_forceswap_action";
    }

    @Override
    public List<Route> routes() {
        return singletonList(new Route(POST, "/_cluster/_primary_forceswap"));
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        final PrimaryShardSwapRequest primaryShardSwapRequest = primaryShardSwapRequest(request);

        return restChannel -> client.execute(PrimaryShardSwapAction.INSTANCE, primaryShardSwapRequest ,
                new RestToXContentListener<>(restChannel));
    }

    private PrimaryShardSwapRequest primaryShardSwapRequest(RestRequest request) {
        PrimaryShardSwapRequest primaryShardSwapRequest = new PrimaryShardSwapRequest(
                primaryAwarenessSettingStore.getPrimaryAwarenessAttribute(),
                primaryAwarenessSettingStore.getPrimaryAwarenessAttributeValue()
        );
        return primaryShardSwapRequest;
    }
}
