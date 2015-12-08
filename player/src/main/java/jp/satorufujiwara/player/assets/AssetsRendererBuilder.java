package jp.satorufujiwara.player.assets;

import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;

import android.content.Context;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import jp.satorufujiwara.player.LimitedBandwidthMeter;
import jp.satorufujiwara.player.Player;
import jp.satorufujiwara.player.RendererBuilder;
import jp.satorufujiwara.player.RendererBuilderCallback;

/**
 * A {@link RendererBuilder} for assets.
 */
public class AssetsRendererBuilder extends RendererBuilder<AssetsEventProxy> {

    long limitBitrate = Long.MAX_VALUE;
    LimitedBandwidthMeter bandwidthMeter;

    private AssetsRendererBuilder(Builder builder) {
        super(builder.context, builder.eventHandler, builder.eventProxy, builder.userAgent,
                builder.uri, builder.bufferSegmentSize, builder.bufferSegmentCount);
    }

    @Override
    protected void buildRenderers(RendererBuilderCallback callback) {
        final Context context = getContext();
        Allocator allocator = new DefaultAllocator(getBufferSegmentSize());
        DataSource dataSource = new DefaultUriDataSource(getContext(), bandwidthMeter,
                getUserAgent());
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(getUri(), dataSource,
                allocator, getBufferSegmentSize() * getBufferSegmentCount());
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(context,
                sampleSource, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000,
                getEventHandler(), getEventProxy(), 50);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource,
                null, true, getEventHandler(), getEventProxy(),
                AudioCapabilities.getCapabilities(context));
        TrackRenderer textRenderer = new TextTrackRenderer(sampleSource, getEventProxy(),
                getEventHandler().getLooper());

        // Invoke the callback.
        TrackRenderer[] renderers = new TrackRenderer[Player.RENDERER_COUNT];
        renderers[Player.TYPE_VIDEO] = videoRenderer;
        renderers[Player.TYPE_AUDIO] = audioRenderer;
        renderers[Player.TYPE_TEXT] = textRenderer;
        callback.onRenderers(renderers, bandwidthMeter);
    }

    @Override
    protected void cancel() {
        //do nothing
    }

    @Override
    protected void setLimitBitrate(long bitrate) {
        limitBitrate = bitrate;
        if (bandwidthMeter != null) {
            bandwidthMeter.setLimitBitrate(bitrate);
        }
    }

    public static class Builder {

        final Context context;
        String userAgent;
        Uri uri;
        AssetsEventProxy eventProxy;
        Handler eventHandler;
        int bufferSegmentSize;
        int bufferSegmentCount;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder uri(Uri uri) {
            this.uri = uri;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
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

        public AssetsRendererBuilder build() {
            if (TextUtils.isEmpty(userAgent)) {
                throw new IllegalArgumentException("UserAgent must not be null.");
            }
            if (uri == null) {
                throw new IllegalArgumentException("Uri must not be null.");
            }
            if (eventHandler == null) {
                eventHandler = new Handler(Looper.getMainLooper());
            }
            if (eventProxy == null) {
                eventProxy = new AssetsEventProxy();
            }
            return new AssetsRendererBuilder(this);
        }
    }

}