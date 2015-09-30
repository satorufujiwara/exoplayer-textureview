package jp.satorufujiwara.player;

import com.google.android.exoplayer.DummyTrackRenderer;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.upstream.BandwidthMeter;
import com.google.android.exoplayer.util.PlayerControl;

import android.view.Surface;

import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A wrapper around {@link ExoPlayer} that provides a higher level interface. It can be prepared
 * with one of a number of {@link RendererBuilder} classes to suit different use cases (e.g. DASH,
 * SmoothStreaming and so on).
 */
public class Player implements ExoPlayer.Listener {

    /**
     * A listener for core events.
     */
    public interface Listener {

        void onStateChanged(boolean playWhenReady, int playbackState);

        void onError(Exception e);

        void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                float pixelWidthHeightRatio);
    }

    // Constants pulled into this class for convenience.
    public static final int STATE_IDLE = ExoPlayer.STATE_IDLE;
    public static final int STATE_PREPARING = ExoPlayer.STATE_PREPARING;
    public static final int STATE_BUFFERING = ExoPlayer.STATE_BUFFERING;
    public static final int STATE_READY = ExoPlayer.STATE_READY;
    public static final int STATE_ENDED = ExoPlayer.STATE_ENDED;

    public static final int TRACK_DISABLED = ExoPlayer.TRACK_DISABLED;
    public static final int TRACK_DEFAULT = ExoPlayer.TRACK_DEFAULT;

    public static final int RENDERER_COUNT = 4;
    public static final int TYPE_VIDEO = 0;
    public static final int TYPE_AUDIO = 1;
    public static final int TYPE_TEXT = 2;
    public static final int TYPE_METADATA = 3;

    private static final int RENDERER_BUILDING_STATE_IDLE = 1;
    private static final int RENDERER_BUILDING_STATE_BUILDING = 2;
    private static final int RENDERER_BUILDING_STATE_BUILT = 3;

    private final ExoPlayer player;
    private final PlayerControl playerControl;
    private final CopyOnWriteArrayList<Listener> listeners;

    private RendererBuilder rendererBuilder;
    private EventProxy eventProxy;
    private int rendererBuildingState;
    private int lastReportedPlaybackState;
    private boolean lastReportedPlayWhenReady;

    private Surface surface;
    private TrackRenderer videoRenderer;
    private int videoTrackToRestore;
    private int audioTrackToRestore;

    private boolean backgrounded;
    private boolean isMute;

    public Player() {
        eventProxy = new EventProxy();
        eventProxy.setPlayer(this);
        player = ExoPlayer.Factory.newInstance(RENDERER_COUNT, 1000, 5000);
        player.addListener(this);
        playerControl = new PlayerControl(player);
        listeners = new CopyOnWriteArrayList<>();
        lastReportedPlaybackState = STATE_IDLE;
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        // Disable text initially.
        player.setSelectedTrack(TYPE_TEXT, TRACK_DISABLED);
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int state) {
        maybeReportPlayerState();
    }

    @Override
    public void onPlayerError(ExoPlaybackException exception) {
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        for (Listener listener : listeners) {
            listener.onError(exception);
        }
    }

    @Override
    public void onPlayWhenReadyCommitted() {
        // Do nothing.
    }

    public PlayerControl getPlayerControl() {
        return playerControl;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void setRendererBuilder(RendererBuilder rendererBuilder) {
        this.rendererBuilder = rendererBuilder;
        eventProxy = rendererBuilder.getEventProxy();
        eventProxy.setPlayer(this);
        pushSurface(false);
    }

    public void setSurface(Surface surface) {
        this.surface = surface;
        pushSurface(false);
    }

    public Surface getSurface() {
        return surface;
    }

    public void blockingClearSurface() {
        surface = null;
        pushSurface(true);
    }

    public int getTrackCount(int type) {
        return player.getTrackCount(type);
    }

    public MediaFormat getTrackFormat(int type, int index) {
        return player.getTrackFormat(type, index);
    }

    public int getSelectedTrack(int type) {
        return player.getSelectedTrack(type);
    }

    public void setSelectedTrack(int type, int index) {
        player.setSelectedTrack(type, index);
        if (type == TYPE_TEXT && index < 0) {
            eventProxy.invokeOnCues(Collections.<Cue>emptyList());
        }
    }

    public boolean getBackgrounded() {
        return backgrounded;
    }

    public void setBackgrounded(boolean backgrounded) {
        if (this.backgrounded == backgrounded) {
            return;
        }
        this.backgrounded = backgrounded;
        if (backgrounded) {
            videoTrackToRestore = getSelectedTrack(TYPE_VIDEO);
            setSelectedTrack(TYPE_VIDEO, TRACK_DISABLED);
            blockingClearSurface();
        } else {
            setSelectedTrack(TYPE_VIDEO, videoTrackToRestore);
        }
    }

    public void setMute(boolean isMute) {
        if (this.isMute == isMute) {
            return;
        }
        this.isMute = isMute;
        if (isMute) {
            audioTrackToRestore = getSelectedTrack(TYPE_AUDIO);
            setSelectedTrack(TYPE_AUDIO, TRACK_DISABLED);
        } else {
            setSelectedTrack(TYPE_AUDIO, audioTrackToRestore);
        }
    }

    public void prepare() {
        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILT) {
            player.stop();
        }
        rendererBuilder.cancel();
        eventProxy.setVideoFormat(null);
        videoRenderer = null;
        rendererBuildingState = RENDERER_BUILDING_STATE_BUILDING;
        maybeReportPlayerState();
        rendererBuilder.buildRenderers(eventProxy);
    }

    public void release() {
        if (rendererBuilder != null) {
            rendererBuilder.cancel();
        }
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        surface = null;
        player.release();
    }

    public void stop() {
        player.stop();
    }

    public long getCurrentPosition() {
        return player.getCurrentPosition();
    }

    public long getDuration() {
        return player.getDuration();
    }

    public int getBufferedPercentage() {
        return player.getBufferedPercentage();
    }

    public long getBufferedPosition() {
        return player.getBufferedPosition();
    }

    public boolean getPlayWhenReady() {
        return player.getPlayWhenReady();
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        player.setPlayWhenReady(playWhenReady);
    }

    public void seekTo(long positionMs) {
        player.seekTo(positionMs);
    }

    public int getPlaybackState() {
        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILDING) {
            return STATE_PREPARING;
        }
        int playerState = player.getPlaybackState();
        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILT && playerState == STATE_IDLE) {
            // This is an edge case where the renderers are built, but are still being passed to the
            // player's playback thread.
            return STATE_PREPARING;
        }
        return playerState;
    }

    /**
     * Invoked with the results from a {@link RendererBuilder}.
     *
     * @param renderers      Renderers indexed by {@link Player} TYPE_* constants. An individual
     *                       element may be null if there do not exist tracks of the corresponding
     *                       type.
     * @param bandwidthMeter Provides an estimate of the currently available bandwidth. May be
     *                       null.
     */
    void invokeOnRenderersBuilt(TrackRenderer[] renderers, BandwidthMeter bandwidthMeter) {
        for (int i = 0; i < RENDERER_COUNT; i++) {
            if (renderers[i] == null) {
                // Convert a null renderer to a dummy renderer.
                renderers[i] = new DummyTrackRenderer();
            }
        }
        // Complete preparation.
        this.videoRenderer = renderers[TYPE_VIDEO];
        pushSurface(false);
        player.prepare(renderers);
        rendererBuildingState = RENDERER_BUILDING_STATE_BUILT;
    }

    void invokeOnRenderersError(Exception e) {
        for (Listener listener : listeners) {
            listener.onError(e);
        }
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        maybeReportPlayerState();
    }

    void invokeOnVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
            float pixelWidthHeightRatio) {
        for (Listener listener : listeners) {
            listener.onVideoSizeChanged(width, height,unappliedRotationDegrees, pixelWidthHeightRatio);
        }
    }

    boolean isDisabledTrack(int type) {
        return getSelectedTrack(TYPE_TEXT) != TRACK_DISABLED;
    }

    private void maybeReportPlayerState() {
        boolean playWhenReady = player.getPlayWhenReady();
        int playbackState = getPlaybackState();
        if (lastReportedPlayWhenReady != playWhenReady
                || lastReportedPlaybackState != playbackState) {
            for (Listener listener : listeners) {
                listener.onStateChanged(playWhenReady, playbackState);
            }
            lastReportedPlayWhenReady = playWhenReady;
            lastReportedPlaybackState = playbackState;
        }
    }

    private void pushSurface(boolean blockForSurfacePush) {
        if (videoRenderer == null || surface == null) {
            return;
        }
        if (blockForSurfacePush) {
            player.blockingSendMessage(videoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE,
                    surface);
        } else {
            player.sendMessage(videoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE,
                    surface);
        }
    }

}