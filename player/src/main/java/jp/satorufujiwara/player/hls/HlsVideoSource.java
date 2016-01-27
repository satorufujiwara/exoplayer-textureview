package jp.satorufujiwara.player.hls;


import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import jp.satorufujiwara.player.RendererBuilder;
import jp.satorufujiwara.player.VideoSource;

public class HlsVideoSource extends VideoSource {

    private final HlsEventProxy eventProxy;
    private final HlsChunkSourceCreator hlsChunkSourceCreator;
    private final int textBufferSegmentSize;

    private HlsVideoSource(Builder builder) {
        super(builder.uri, builder.userAgent, builder.eventHandler, builder.bufferSegmentSize,
                builder.bufferSegmentCount);
        eventProxy = builder.eventProxy;
        hlsChunkSourceCreator = builder.hlsChunkSourceCreator;
        textBufferSegmentSize = builder.textBufferSegmentCount;
    }

    @Override
    public RendererBuilder createRendererBuilder(Context context) {
        return new HlsRendererBuilder(context, eventHandler, eventProxy, userAgent, uri,
                bufferSegmentSize, bufferSegmentCount, textBufferSegmentSize,
                hlsChunkSourceCreator);
    }

    public static Builder newBuilder(final Uri uri, final String userAgent) {
        return new Builder(uri, userAgent);
    }

    public static class Builder {

        final String userAgent;
        final Uri uri;
        HlsEventProxy eventProxy;
        Handler eventHandler;
        int bufferSegmentSize = RendererBuilder.DEFAULT_BUFFER_SEGMENT_SIZE;
        int bufferSegmentCount = RendererBuilder.DEFAULT_BUFFER_SEGMENT_COUNT;
        int textBufferSegmentCount = RendererBuilder.DEFAULT_TEXT_BUFFER_SEGMENT_COUNT;
        HlsChunkSourceCreator hlsChunkSourceCreator;

        private Builder(final Uri uri, final String userAgent) {
            this.uri = uri;
            this.userAgent = userAgent;
        }

        public Builder eventProxy(HlsEventProxy eventProxy) {
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

        public Builder textBufferSegmentCount(int count) {
            textBufferSegmentCount = count;
            return this;
        }

        public Builder hlsChunkSourceCreator(HlsChunkSourceCreator creator) {
            hlsChunkSourceCreator = creator;
            return this;
        }

        public HlsVideoSource build() {
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
                eventProxy = new HlsEventProxy();
            }
            return new HlsVideoSource(this);
        }
    }
}
