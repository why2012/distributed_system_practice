package org.elasticsearch.primaryaware.action.bulk;

import org.elasticsearch.action.StreamableResponseActionType;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.primaryaware.utils.ReflectionUtils;
import org.elasticsearch.transport.TransportRequestOptions;

public class AsyncBulkAction extends StreamableResponseActionType<BulkResponse> {
    public static final AsyncBulkAction INSTANCE = new AsyncBulkAction();
    public static final String NAME = "indices:data/write/async_bulk";

    private AsyncBulkAction() {
        super(NAME);
    }

    @Override
    public BulkResponse newResponse() {
        return ReflectionUtils.newInstance(BulkResponse.class, new Class[]{}, new Object[]{});
    }

    @Override
    public TransportRequestOptions transportOptions(Settings settings) {
        return TransportRequestOptions.builder().withType(TransportRequestOptions.Type.BULK).build();
    }
}
