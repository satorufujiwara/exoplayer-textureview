package jp.satorufujiwara.player;

import com.google.android.exoplayer.CodecCounters;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioTrack;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.metadata.MetadataTrackRenderer;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.text.TextRenderer;
import com.google.android.exoplayer.upstream.BandwidthMeter;
import com.google.android.exoplayer.util.DebugTextViewHelper;

import android.media.MediaCodec;
import android.view.Surface;

import java.util.List;
import java.util.Map;

public class EventProxy implements
        RendererBuilderCallback,
        TextRenderer,
        MetadataTrackRenderer.MetadataRenderer<Map<String, Object>>,
        BandwidthMeter.EventListener,
        MediaCodecVideoTrackRenderer.EventListener,
        MediaCodecAudioTrackRenderer.EventListener,
        DebugTextViewHelper.Provider {

    /**
     * A listener for debugging information.
     */
    public interface InfoListener {

        void onDroppedFrames(int count, long elapsed);

        void onBandwidthSample(int elapsedMs, long bytes, long bitrateEstimate);

        void onDecoderInitialized(String decoderName, long elapsedRealtimeMs,
                long initializationDurationMs);
    }

    /**
     * A listener for internal errors.
     * These errors are not visible to the user, and hence this listener is provided for
     * informational purposes only. Note however that an internal error may cause a fatal
     * error if the player fails to recover. If this happens, {@link Player.Listener#onError(Exception)}
     * will be invoked.
     */
    public interface InternalErrorListener {

        void onRendererInitializationError(Exception e);

        void onAudioTrackInitializationError(AudioTrack.InitializationException e);

        void onAudioTrackWriteError(AudioTrack.WriteException e);

        void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs);

        void onDecoderInitializationError(MediaCodecTrackRenderer.DecoderInitializationException e);

        void onCryptoError(MediaCodec.CryptoException e);
    }

    /**
     * A listener for receiving notifications of timed text.
     */
    public interface CaptionListener {

        void onCues(List<Cue> cues);
    }

    /**
     * A listener for receiving ID3 metadata parsed from the media stream.
     */
    public interface Id3MetadataListener {

        void onId3Metadata(Map<String, Object> metadata);
    }

    private InfoListener infoListener;
    private InternalErrorListener internalErrorListener;
    private CaptionListener captionListener;
    private Id3MetadataListener id3MetadataListener;
    private Player player;

    // for debug
    private Format videoFormat;
    private BandwidthMeter bandwidthMeter;
    private CodecCounters codecCounters;

    public void setInfoListener(InfoListener infoListener) {
        this.infoListener = infoListener;
    }

    public void setInternalErrorListener(InternalErrorListener internalErrorListener) {
        this.internalErrorListener = internalErrorListener;
    }

    public void setCaptionListener(CaptionListener captionListener) {
        this.captionListener = captionListener;
    }

    public void setId3MetadataListener(Id3MetadataListener id3MetadataListener) {
        this.id3MetadataListener = id3MetadataListener;
    }

    protected void setVideoFormat(Format videoFormat) {
        this.videoFormat = videoFormat;
    }

    void setPlayer(Player player) {
        this.player = player;
    }

    void invokeOnCues(List<Cue> cues) {
        if (captionListener != null) {
            captionListener.onCues(cues);
        }
    }

    /** RendererBuilderCallback */
    @Override
    public void onRenderers(TrackRenderer[] renderers, BandwidthMeter bandwidthMeter) {
        player.invokeOnRenderersBuilt(renderers, bandwidthMeter);
        this.bandwidthMeter = bandwidthMeter;
        this.codecCounters = renderers[Player.TYPE_VIDEO] instanceof MediaCodecTrackRenderer
                ? ((MediaCodecTrackRenderer) renderers[Player.TYPE_VIDEO]).codecCounters
                : renderers[Player.TYPE_AUDIO] instanceof MediaCodecTrackRenderer
                        ? ((MediaCodecTrackRenderer) renderers[Player.TYPE_AUDIO]).codecCounters
                        : null;
    }

    /** RendererBuilderCallback */
    @Override
    public void onRenderersError(Exception e) {
        player.invokeOnRenderersError(e);
        if (internalErrorListener != null) {
            internalErrorListener.onRendererInitializationError(e);
        }
    }

    /** TextRenderer */
    @Override
    public void onCues(List<Cue> cues) {
        if (captionListener != null && !player.isDisabledTrack(Player.TYPE_TEXT)) {
            captionListener.onCues(cues);
        }
    }

    /** MetadataTrackRenderer.MetadataRenderer */
    @Override
    public void onMetadata(Map<String, Object> metadata) {
        if (id3MetadataListener != null && !player.isDisabledTrack(Player.TYPE_METADATA)) {
            id3MetadataListener.onId3Metadata(metadata);
        }
    }

    /** BandwidthMeter.EventListener */
    @Override
    public void onBandwidthSample(int elapsedMs, long bytes, long bitrateEstimate) {
        if (infoListener != null) {
            infoListener.onBandwidthSample(elapsedMs, bytes, bitrateEstimate);
        }
    }

    /** MediaCodecVideoTrackRenderer.EventListener */
    @Override
    public void onDroppedFrames(int count, long elapsed) {
        if (infoListener != null) {
            infoListener.onDroppedFrames(count, elapsed);
        }
    }

    /** MediaCodecVideoTrackRenderer.EventListener */
    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
            float pixelWidthHeightRatio) {
        player.invokeOnVideoSizeChanged(width, height, unappliedRotationDegrees,
                pixelWidthHeightRatio);
    }

    /** MediaCodecVideoTrackRenderer.EventListener */
    @Override
    public void onDrawnToSurface(Surface surface) {
        // Do nothing.
    }

    /** MediaCodecVideoTrackRenderer.EventListener */
    @Override
    public void onDecoderInitializationError(
            MediaCodecTrackRenderer.DecoderInitializationException e) {
        if (internalErrorListener != null) {
            internalErrorListener.onDecoderInitializationError(e);
        }
    }

    /** MediaCodecVideoTrackRenderer.EventListener */
    @Override
    public void onCryptoError(MediaCodec.CryptoException e) {
        if (internalErrorListener != null) {
            internalErrorListener.onCryptoError(e);
        }
    }

    /** MediaCodecVideoTrackRenderer.EventListener */
    @Override
    public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs,
            long initializationDurationMs) {
        if (infoListener != null) {
            infoListener.onDecoderInitialized(decoderName, elapsedRealtimeMs,
                    initializationDurationMs);
        }
    }

    /** MediaCodecAudioTrackRenderer.EventListener */
    @Override
    public void onAudioTrackInitializationError(AudioTrack.InitializationException e) {
        if (internalErrorListener != null) {
            internalErrorListener.onAudioTrackInitializationError(e);
        }
    }

    /** MediaCodecAudioTrackRenderer.EventListener */
    @Override
    public void onAudioTrackWriteError(AudioTrack.WriteException e) {
        if (internalErrorListener != null) {
            internalErrorListener.onAudioTrackWriteError(e);
        }
    }

    /** MediaCodecAudioTrackRenderer.EventListener */
    @Override
    public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs,
            long elapsedSinceLastFeedMs) {
        if (internalErrorListener != null) {
            internalErrorListener.onAudioTrackUnderrun(bufferSize, bufferSizeMs,
                    elapsedSinceLastFeedMs);
        }
    }

    @Override
    public long getCurrentPosition() {
        return player.getCurrentPosition();
    }

    @Override
    public Format getFormat() {
        return videoFormat;
    }

    @Override
    public BandwidthMeter getBandwidthMeter() {
        return bandwidthMeter;
    }

    @Override
    public CodecCounters getCodecCounters() {
        return codecCounters;
    }
}
