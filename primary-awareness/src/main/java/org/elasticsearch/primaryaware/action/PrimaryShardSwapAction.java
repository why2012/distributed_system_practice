package org.elasticsearch.primaryaware.action;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.ActionType;
import org.elasticsearch.action.support.master.MasterNodeRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.StatusToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;

public class PrimaryShardSwapAction extends ActionType<PrimaryShardSwapAction.PrimaryShardSwapResponse> {

    public static final PrimaryShardSwapAction INSTANCE = new PrimaryShardSwapAction();
    public static final String NAME = "cluster:primary_awareness/swap";

    private PrimaryShardSwapAction() {
        super(NAME, PrimaryShardSwapResponse::new);
    }

    public static class PrimaryShardSwapRequest extends MasterNodeRequest<PrimaryShardSwapRequest> {
        private final String primaryAwarenessAttribute;
        private final String primaryAwarenessAttributeValue;

        public PrimaryShardSwapRequest(String primaryAwarenessAttribute, String primaryAwarenessAttributeValue) {
            this.primaryAwarenessAttribute = primaryAwarenessAttribute;
            this.primaryAwarenessAttributeValue = primaryAwarenessAttributeValue;
        }

        protected PrimaryShardSwapRequest(StreamInput in) throws IOException {
            super(in);
            primaryAwarenessAttribute = in.readString();
            primaryAwarenessAttributeValue = in.readString();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(primaryAwarenessAttribute);
            out.writeString(primaryAwarenessAttributeValue);
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        public String getPrimaryAwarenessAttribute() {
            return primaryAwarenessAttribute;
        }

        public String getPrimaryAwarenessAttributeValue() {
            return primaryAwarenessAttributeValue;
        }
    }

    public static class PrimaryShardSwapResponse extends ActionResponse implements StatusToXContentObject {

        public PrimaryShardSwapResponse() {}

        public PrimaryShardSwapResponse(StreamInput in) throws IOException {
            super(in);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {

        }

        @Override
        public RestStatus status() {
            return RestStatus.OK;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field("msg", "task submitted");
            builder.endObject();
            return null;
        }
    }
}
