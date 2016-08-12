package jp.satorufujiwara.player.hls;

import com.google.android.exoplayer.DefaultLoadControl;
import com.google.android.exoplayer.LoadControl;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.chunk.VideoFormatSelectorUtil;
import com.google.android.exoplayer.hls.DefaultHlsTrackSelector;
import com.google.android.exoplayer.hls.HlsChunkSource;
import com.google.android.exoplayer.hls.HlsMasterPlaylist;
import com.google.android.exoplayer.hls.HlsPlaylist;
import com.google.android.exoplayer.hls.HlsPlaylistParser;
import com.google.android.exoplayer.hls.HlsSampleSource;
import com.google.android.exoplayer.hls.PtsTimestampAdjusterProvider;
import com.google.android.exoplayer.metadata.MetadataTrackRenderer;
import com.google.android.exoplayer.metadata.id3.Id3Frame;
import com.google.android.exoplayer.metadata.id3.Id3Parser;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.text.eia608.Eia608TrackRenderer;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;
import com.google.android.exoplayer.util.ManifestFetcher.ManifestCallback;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.Handler;

import java.io.IOException;
import java.util.List;

import jp.satorufujiwara.player.DataSourceCreator;
import jp.satorufujiwara.player.LimitedBandwidthMeter;
import jp.satorufujiwara.player.Player;
import jp.satorufujiwara.player.RendererBuilder;
import jp.satorufujiwara.player.RendererBuilderCallback;

/**
 * A {@link RendererBuilder} for HLS.
 */
public class HlsRendererBuilder extends RendererBuilder<HlsEventProxy> {

    long limitBitrate = Long.MAX_VALUE;
    LimitedBandwidthMeter bandwidthMeter;
    final DataSourceCreator dataSourceCreator;
    final HlsChunkSourceCreator hlsChunkSourceCreator;
    final int audioBufferSegmentCount;
    final int textBufferSegmentCount;
    private AsyncRendererBuilder currentAsyncBuilder;

    HlsRendererBuilder(Context context, Handler eventHandler, HlsEventProxy eventProxy,
            String userAgent, Uri uri, int bufferSegmentSize, int bufferSegmentCount,
            int audioBufferSegmentCount, int textBufferSegmentCount,
            DataSourceCreator dataSourceCreator, HlsChunkSourceCreator hlsChunkSourceCreator) {
        super(context, eventHandler, eventProxy, userAgent, uri, bufferSegmentSize,
                bufferSegmentCount);
        this.audioBufferSegmentCount = audioBufferSegmentCount;
        this.textBufferSegmentCount = textBufferSegmentCount;
        this.dataSourceCreator = dataSourceCreator;
        this.hlsChunkSourceCreator = hlsChunkSourceCreator;
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
            playlistFetcher = new ManifestFetcher<>(rendererBuilder.uri.toString(),
                    new DefaultUriDataSource(rendererBuilder.context,
                            rendererBuilder.userAgent), parser);
        }

        public void init() {
            playlistFetcher.singleLoad(rendererBuilder.eventHandler.getLooper(), this);
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
            final Context context = rendererBuilder.context;
            final Handler handler = rendererBuilder.eventHandler;
            final LoadControl loadControl = new DefaultLoadControl(
                    new DefaultAllocator(rendererBuilder.bufferSegmentSize));
            final LimitedBandwidthMeter bandwidthMeter = new LimitedBandwidthMeter(handler,
                    eventProxy);
            bandwidthMeter.setLimitBitrate(limitBitrate);
            rendererBuilder.bandwidthMeter = bandwidthMeter;
            PtsTimestampAdjusterProvider timestampAdjusterProvider
                    = new PtsTimestampAdjusterProvider();

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
            final HlsEventProxy eventProxy = rendererBuilder.eventProxy;

            boolean haveSubtitles = false;
            boolean haveAudios = false;
            if (manifest instanceof HlsMasterPlaylist) {
                HlsMasterPlaylist masterPlaylist = (HlsMasterPlaylist) manifest;
                haveSubtitles = !masterPlaylist.subtitles.isEmpty();
                haveAudios = !masterPlaylist.audios.isEmpty();
            }

            // Build the video/metadata renderers.
            final DataSource dataSource;
            if (rendererBuilder.dataSourceCreator != null) {
                dataSource = rendererBuilder.dataSourceCreator.create(context, bandwidthMeter,
                        rendererBuilder.userAgent);
            } else {
                dataSource = new DefaultUriDataSource(context, bandwidthMeter,
                        rendererBuilder.userAgent);
            }

            final HlsChunkSource chunkSource;
            if (rendererBuilder.hlsChunkSourceCreator != null) {
                chunkSource = rendererBuilder.hlsChunkSourceCreator.create(dataSource,
                        manifest, bandwidthMeter, timestampAdjusterProvider, variantIndices);
            } else {
                chunkSource = new HlsChunkSource(true /* isMaster */, dataSource, manifest,
                        DefaultHlsTrackSelector.newDefaultInstance(context), bandwidthMeter,
                        timestampAdjusterProvider);

            }
            HlsSampleSource sampleSource = new HlsSampleSource(chunkSource, loadControl,
                    rendererBuilder.bufferSegmentSize * rendererBuilder.bufferSegmentCount,
                    handler, eventProxy, Player.TYPE_VIDEO);
            MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(context,
                    sampleSource, MediaCodecSelector.DEFAULT,
                    MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000, handler, eventProxy, 50);
            MetadataTrackRenderer<List<Id3Frame>> id3Renderer = new MetadataTrackRenderer<>(
                    sampleSource, new Id3Parser(), eventProxy, handler.getLooper());

            // Build the audio renderer.
            MediaCodecAudioTrackRenderer audioRenderer;
            if (haveAudios) {
                DataSource audioDataSource = new DefaultUriDataSource(context, bandwidthMeter,
                        userAgent);
                HlsChunkSource audioChunkSource = new HlsChunkSource(false /* isMaster */,
                        audioDataSource, manifest,
                        DefaultHlsTrackSelector.newAudioInstance(),
                        bandwidthMeter, timestampAdjusterProvider);
                HlsSampleSource audioSampleSource = new HlsSampleSource(audioChunkSource,
                        loadControl,
                        rendererBuilder.bufferSegmentSize * rendererBuilder.audioBufferSegmentCount,
                        handler, eventProxy, Player.TYPE_AUDIO);
                audioRenderer = new MediaCodecAudioTrackRenderer(
                        new SampleSource[]{sampleSource, audioSampleSource},
                        MediaCodecSelector.DEFAULT, null,
                        true, handler, eventProxy, AudioCapabilities.getCapabilities(context),
                        AudioManager.STREAM_MUSIC);
            } else {
                audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource,
                        MediaCodecSelector.DEFAULT, null, true, handler, eventProxy,
                        AudioCapabilities.getCapabilities(context), AudioManager.STREAM_MUSIC);
            }

            // Build the text renderer.
            TrackRenderer textRenderer;
            if (haveSubtitles) {
                DataSource textDataSource = new DefaultUriDataSource(context, bandwidthMeter,
                        userAgent);
                HlsChunkSource textChunkSource = new HlsChunkSource(false /* isMaster */,
                        textDataSource, manifest,
                        DefaultHlsTrackSelector.newSubtitleInstance(), bandwidthMeter,
                        timestampAdjusterProvider);
                HlsSampleSource textSampleSource = new HlsSampleSource(textChunkSource, loadControl,
                        rendererBuilder.textBufferSegmentCount * bufferSegmentSize, handler,
                        eventProxy,
                        Player.TYPE_TEXT);
                textRenderer = new TextTrackRenderer(textSampleSource, eventProxy,
                        handler.getLooper());
            } else {
                textRenderer = new Eia608TrackRenderer(sampleSource, eventProxy,
                        handler.getLooper());
            }

            TrackRenderer[] renderers = new TrackRenderer[Player.RENDERER_COUNT];
            renderers[Player.TYPE_VIDEO] = videoRenderer;
            renderers[Player.TYPE_AUDIO] = audioRenderer;
            renderers[Player.TYPE_METADATA] = id3Renderer;
            renderers[Player.TYPE_TEXT] = textRenderer;
            callback.onRenderers(renderers, bandwidthMeter);
        }

    }

}