package jp.satorufujiwara.player.hls;

import com.google.android.exoplayer.DefaultLoadControl;
import com.google.android.exoplayer.LoadControl;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.chunk.VideoFormatSelectorUtil;
import com.google.android.exoplayer.hls.HlsChunkSource;
import com.google.android.exoplayer.hls.HlsMasterPlaylist;
import com.google.android.exoplayer.hls.HlsPlaylist;
import com.google.android.exoplayer.hls.HlsPlaylistParser;
import com.google.android.exoplayer.hls.HlsSampleSource;
import com.google.android.exoplayer.metadata.Id3Parser;
import com.google.android.exoplayer.metadata.MetadataTrackRenderer;
import com.google.android.exoplayer.text.eia608.Eia608TrackRenderer;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;
import com.google.android.exoplayer.util.ManifestFetcher.ManifestCallback;

import android.content.Context;
import android.media.MediaCodec;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.Map;

import jp.satorufujiwara.player.LimitedBandwidthMeter;
import jp.satorufujiwara.player.Player;
import jp.satorufujiwara.player.RendererBuilder;
import jp.satorufujiwara.player.RendererBuilderCallback;

/**
 * A {@link RendererBuilder} for HLS.
 */
public class HlsRendererBuilder extends RendererBuilder<HlsEventProxy> {

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENTS = 256;

    long limitBitrate = Long.MAX_VALUE;
    LimitedBandwidthMeter bandwidthMeter;
    private AsyncRendererBuilder currentAsyncBuilder;

    private HlsRendererBuilder(Builder builder) {
        super(builder.context, builder.eventHandler, builder.eventProxy, builder.userAgent,
                builder.url);
    }

    @Override
    protected void buildRenderers(RendererBuilderCallback callback) {
        currentAsyncBuilder = new AsyncRendererBuilder(this, callback);
        currentAsyncBuilder.init();
    }

    @Override
    protected void cancel() {
        if (currentAsyncBuilder != null) {
            currentAsyncBuilder.cancel();
            currentAsyncBuilder = null;
        }
    }

    @Override
    protected void setLimitBitrate(long bitrate) {
        limitBitrate = bitrate;
        if (bandwidthMeter != null) {
            bandwidthMeter.setLimitBitrate(bitrate);
        }
    }

    private final class AsyncRendererBuilder implements ManifestCallback<HlsPlaylist> {

        private HlsRendererBuilder rendererBuilder;
        private final RendererBuilderCallback callback;
        private final ManifestFetcher<HlsPlaylist> playlistFetcher;

        private boolean canceled;

        public AsyncRendererBuilder(HlsRendererBuilder rendererBuilder,
                RendererBuilderCallback callback) {
            this.rendererBuilder = rendererBuilder;
            this.callback = callback;
            HlsPlaylistParser parser = new HlsPlaylistParser();
            playlistFetcher = new ManifestFetcher<>(rendererBuilder.getUrl(),
                    new DefaultUriDataSource(rendererBuilder.getContext(),
                            rendererBuilder.getUserAgent()), parser);
        }

        public void init() {
            playlistFetcher.singleLoad(rendererBuilder.getEventHandler().getLooper(), this);
        }

        public void cancel() {
            canceled = true;
            rendererBuilder = null;
        }

        @Override
        public void onSingleManifestError(IOException e) {
            if (canceled) {
                return;
            }
            callback.onRenderersError(e);
        }

        @Override
        public void onSingleManifest(HlsPlaylist manifest) {
            if (canceled) {
                return;
            }
            final Context context = rendererBuilder.getContext();
            final Handler handler = rendererBuilder.getEventHandler();
            final LoadControl loadControl = new DefaultLoadControl(
                    new DefaultAllocator(BUFFER_SEGMENT_SIZE));

            final LimitedBandwidthMeter bandwidthMeter = new LimitedBandwidthMeter();
            bandwidthMeter.setLimitBitrate(limitBitrate);
            rendererBuilder.bandwidthMeter = bandwidthMeter;

            int[] variantIndices = null;
            if (manifest instanceof HlsMasterPlaylist) {
                HlsMasterPlaylist masterPlaylist = (HlsMasterPlaylist) manifest;
                try {
                    variantIndices = VideoFormatSelectorUtil.selectVideoFormatsForDefaultDisplay(
                            context, masterPlaylist.variants, null, false);
                } catch (DecoderQueryException e) {
                    callback.onRenderersError(e);
                    return;
                }
            }

            final HlsEventProxy eventProxy = rendererBuilder.getEventProxy();
            final DataSource dataSource = new DefaultUriDataSource(context, bandwidthMeter,
                    rendererBuilder.getUserAgent());
            HlsChunkSource chunkSource = new HlsChunkSource(dataSource, rendererBuilder.getUrl(),
                    manifest, bandwidthMeter,
                    variantIndices, HlsChunkSource.ADAPTIVE_MODE_SPLICE);
            HlsSampleSource sampleSource = new HlsSampleSource(chunkSource, loadControl,
                    BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, handler, eventProxy, Player.TYPE_VIDEO);
            MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(context,
                    sampleSource, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000, handler,
                    eventProxy, 50);
            MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(
                    sampleSource,
                    null, true, handler, eventProxy, AudioCapabilities.getCapabilities(context));
            MetadataTrackRenderer<Map<String, Object>> id3Renderer = new MetadataTrackRenderer<>(
                    sampleSource, new Id3Parser(), eventProxy, handler.getLooper());
            Eia608TrackRenderer closedCaptionRenderer = new Eia608TrackRenderer(sampleSource,
                    eventProxy,
                    handler.getLooper());

            TrackRenderer[] renderers = new TrackRenderer[Player.RENDERER_COUNT];
            renderers[Player.TYPE_VIDEO] = videoRenderer;
            renderers[Player.TYPE_AUDIO] = audioRenderer;
            renderers[Player.TYPE_METADATA] = id3Renderer;
            renderers[Player.TYPE_TEXT] = closedCaptionRenderer;
            callback.onRenderers(renderers, bandwidthMeter);
        }

    }

    public static class Builder {

        final Context context;
        String userAgent;
        String url;
        HlsEventProxy eventProxy;
        Handler eventHandler;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder eventProxy(HlsEventProxy eventProxy) {
            this.eventProxy = eventProxy;
            return this;
        }

        public Builder eventHandler(Handler eventHandler) {
            this.eventHandler = eventHandler;
            return this;
        }

        public HlsRendererBuilder build() {
            if (userAgent == null) {
                throw new IllegalArgumentException("UserAgent must not be null.");
            }
            if (url == null) {
                throw new IllegalArgumentException("Url must not be null.");
            }
            if (eventHandler == null) {
                eventHandler = new Handler(Looper.getMainLooper());
            }
            if (eventProxy == null) {
                eventProxy = new HlsEventProxy();
            }
            return new HlsRendererBuilder(this);
        }
    }

}