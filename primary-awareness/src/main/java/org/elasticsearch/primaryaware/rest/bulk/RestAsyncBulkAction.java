package org.elasticsearch.primaryaware.rest.bulk;

import org.apache.logging.log4j.LogManager;
import org.elasticsearch.primaryaware.action.bulk.AsyncBulkAction;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkShardRequest;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.logging.DeprecationLogger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestStatusToXContentListener;
import org.elasticsearch.rest.action.document.RestBulkAction;
import org.elasticsearch.rest.action.search.RestSearchAction;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestRequest.Method.PUT;

public class RestAsyncBulkAction extends BaseRestHandler {

    private final boolean allowExplicitIndex;
    private static final DeprecationLogger deprecationLogger = new DeprecationLogger(LogManager.getLogger(RestAsyncBulkAction.class));
    public static final String TYPES_DEPRECATION_MESSAGE = "[types removal]" +
            " Specifying types in bulk requests is deprecated.";

    public RestAsyncBulkAction(Settings settings) {
        this.allowExplicitIndex = MULTI_ALLOW_EXPLICIT_INDEX.get(settings);
    }

    @Override
    public List<Route> routes() {
        return unmodifiableList(asList(
                new Route(POST, "/_async_bulk"),
                new Route(PUT, "/_async_bulk"),
                new Route(POST, "/{index}/_async_bulk"),
                new Route(PUT, "/{index}/_async_bulk"),
                // Deprecated typed endpoints.
                new Route(POST, "/{index}/{type}/_async_bulk"),
                new Route(PUT, "/{index}/{type}/_async_bulk")));
    }

    @Override
    public String getName() {
        return "bulk_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        BulkRequest bulkRequest = Requests.bulkRequest();
        String defaultIndex = request.param("index");
        String defaultType = request.param("type");
        if (defaultType == null) {
            defaultType = MapperService.SINGLE_MAPPING_NAME;
        } else {
            deprecationLogger.deprecatedAndMaybeLog("bulk_with_types", RestBulkAction.TYPES_DEPRECATION_MESSAGE);
        }
        String defaultRouting = request.param("routing");
        FetchSourceContext defaultFetchSourceContext = FetchSourceContext.parseFromRestRequest(request);
        String defaultPipeline = request.param("pipeline");
        String waitForActiveShards = request.param("wait_for_active_shards");
        if (waitForActiveShards != null) {
            bulkRequest.waitForActiveShards(ActiveShardCount.parseString(waitForActiveShards));
        }
        bulkRequest.timeout(request.paramAsTime("timeout", BulkShardRequest.DEFAULT_TIMEOUT));
        bulkRequest.setRefreshPolicy(request.param("refresh"));
        bulkRequest.add(request.requiredContent(), defaultIndex, defaultType, defaultRouting,
                defaultFetchSourceContext, defaultPipeline, allowExplicitIndex, request.getXContentType());

        return channel -> client.execute(AsyncBulkAction.INSTANCE, bulkRequest, new RestStatusToXContentListener<>(channel));
    }

    @Override
    public boolean supportsContentStream() {
        return true;
    }

    @Override
    public boolean allowsUnsafeBuffers() {
        return true;
    }
}
