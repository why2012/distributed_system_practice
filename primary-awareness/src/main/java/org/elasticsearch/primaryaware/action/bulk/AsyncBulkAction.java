package org.elasticsearch.primaryaware.action.bulk;

import org.elasticsearch.action.ActionType;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.transport.TransportRequestOptions;

public class AsyncBulkAction extends ActionType<BulkResponse> {
    public static final AsyncBulkAction INSTANCE = new AsyncBulkAction();
    public static final String NAME = "indices:data/write/async_bulk";

    private AsyncBulkAction() {
        super(NAME, BulkResponse::new);
    }

    @Override
    public TransportRequestOptions transportOptions(Settings settings) {
        return TransportRequestOptions.builder().withType(TransportRequestOptions.Type.BULK).build();
    }
}
