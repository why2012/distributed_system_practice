package org.elasticsearch.primaryaware.action;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.TransportMasterNodeAction;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.routing.allocation.AllocationService;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.primaryaware.PrimaryForceswapTrigger;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;

import static org.elasticsearch.primaryaware.action.PrimaryShardSwapAction.PrimaryShardSwapRequest;
import static org.elasticsearch.primaryaware.action.PrimaryShardSwapAction.PrimaryShardSwapResponse;

public class TransportPrimaryShardSwapAction extends TransportMasterNodeAction<PrimaryShardSwapRequest, PrimaryShardSwapResponse> {

    private PrimaryForceswapTrigger primaryForceswapTrigger;
    private AllocationService allocationService;

    @Inject
    public TransportPrimaryShardSwapAction(TransportService transportService, ClusterService clusterService,
                                           ThreadPool threadPool, ActionFilters actionFilters,
                                           IndexNameExpressionResolver indexNameExpressionResolver,
                                           PrimaryForceswapTrigger primaryForceswapTrigger,
                                           AllocationService allocationService) {
        super(PrimaryShardSwapAction.NAME, transportService, clusterService, threadPool, actionFilters,
                PrimaryShardSwapRequest::new, indexNameExpressionResolver);
        this.primaryForceswapTrigger = primaryForceswapTrigger;
        this.allocationService = allocationService;
    }
    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Override
    protected PrimaryShardSwapResponse newResponse() {
        return new PrimaryShardSwapResponse();
    }

    @Override
    protected PrimaryShardSwapResponse read(StreamInput in) throws IOException {
        return new PrimaryShardSwapResponse(in);
    }

    @Override
    protected void masterOperation(PrimaryShardSwapRequest request, ClusterState state, ActionListener<PrimaryShardSwapResponse> listener) throws Exception {
        this.primaryForceswapTrigger.trigger(request.getPrimaryAwarenessAttribute(), request.getPrimaryAwarenessAttributeValue(),
                this.allocationService);
        PrimaryShardSwapResponse primaryShardSwapResponse = new PrimaryShardSwapResponse();
        listener.onResponse(primaryShardSwapResponse);
    }

    @Override
    protected ClusterBlockException checkBlock(PrimaryShardSwapRequest request, ClusterState state) {
        return null;
    }
}
