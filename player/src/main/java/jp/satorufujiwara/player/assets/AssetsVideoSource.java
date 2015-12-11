package jp.satorufujiwara.player.assets;


import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import jp.satorufujiwara.player.RendererBuilder;
import jp.satorufujiwara.player.VideoSource;

public class AssetsVideoSource extends VideoSource {

    private final AssetsEventProxy eventProxy;

    private AssetsVideoSource(Builder builder) {
        super(builder.uri, builder.userAgent, builder.eventHandler, builder.bufferSegmentSize,
                builder.bufferSegmentCount);
        eventProxy = builder.eventProxy;
    }

    @Override
    public RendererBuilder createRendererBuilder(Context context) {
        return new AssetsRendererBuilder(context, eventHandler, eventProxy, userAgent, uri,
                bufferSegmentSize, bufferSegmentCount);
    }

    public static Builder newBuilder(final Uri uri, final String userAgent) {
        return new Builder(uri, userAgent);
    }

    public static class Builder {

        final String userAgent;
        final Uri uri;
        AssetsEventProxy eventProxy;
        Handler eventHandler;
        int bufferSegmentSize = RendererBuilder.DEFAULT_BUFFER_SEGMENT_SIZE;
        int bufferSegmentCount = RendererBuilder.DEFAULT_BUFFER_SEGMENT_COUNT;

        private Builder(final Uri uri, final String userAgent) {
            this.uri = uri;
            this.userAgent = userAgent;
        }

        public Builder eventProxy(AssetsEventProxy eventProxy) {
            this.eventProxy = eventProxy;
            return this;
        }

        public Builder eventHandler(Handler eventHandler) {
            this.eventHandler = eventHandler;
            return this;
        }

        public Builder bufferSegmentSize(int size) {
            bufferSegmentSize = size;
            return this;
        }

        public Builder bufferSegmentCount(int count) {
            bufferSegmentCount = count;
            return this;
        }

        public AssetsVideoSource build() {
            if (TextUtils.isEmpty(userAgent)) {
                throw new IllegalArgumentException("UserAgent must not be null.");
            }
            if (uri == null) {
                throw new IllegalArgumentException("Url must not be null.");
            }
            if (eventHandler == null) {
                eventHandler = new Handler(Looper.getMainLooper());
            }
            if (eventProxy == null) {
                eventProxy = new AssetsEventProxy();
            }
            return new AssetsVideoSource(this);
        }
    }
}
