package jp.satorufujiwara.player.assets;

import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
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
import android.media.AudioManager;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.Handler;

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

    AssetsRendererBuilder(Context context, Handler eventHandler, AssetsEventProxy eventProxy,
            String userAgent, Uri uri, int bufferSegmentSize, int bufferSegmentCount) {
        super(context, eventHandler, eventProxy, userAgent, uri, bufferSegmentSize,
                bufferSegmentCount);
    }

    @Override
    protected void buildRenderers(RendererBuilderCallback callback) {
        Allocator allocator = new DefaultAllocator(bufferSegmentSize);
        DataSource dataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(uri, dataSource,
                allocator, bufferSegmentSize * bufferSegmentCount);
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(context,
                sampleSource, MediaCodecSelector.DEFAULT,
                MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000,
                eventHandler, eventProxy, 50);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource,
                MediaCodecSelector.DEFAULT, null, true, eventHandler, eventProxy,
                AudioCapabilities.getCapabilities(context), AudioManager.STREAM_MUSIC);

        TrackRenderer textRenderer = new TextTrackRenderer(sampleSource, eventProxy,
                eventHandler.getLooper());

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

}