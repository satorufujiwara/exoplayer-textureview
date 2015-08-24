package jp.satorufujiwara.player;

import com.google.android.exoplayer.DummyTrackRenderer;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.chunk.MultiTrackChunkSource;
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

        void onVideoSizeChanged(int width, int height, float pixelWidthHeightRatio);
    }

    // Constants pulled into this class for convenience.
    public static final int STATE_IDLE = ExoPlayer.STATE_IDLE;
    public static final int STATE_PREPARING = ExoPlayer.STATE_PREPARING;
    public static final int STATE_BUFFERING = ExoPlayer.STATE_BUFFERING;
    public static final int STATE_READY = ExoPlayer.STATE_READY;
    public static final int STATE_ENDED = ExoPlayer.STATE_ENDED;

    public static final int DISABLED_TRACK = -1;
    public static final int PRIMARY_TRACK = 0;

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

    private MultiTrackChunkSource[] multiTrackSources;
    private String[][] trackNames;
    private int[] selectedTracks;
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
        selectedTracks = new int[RENDERER_COUNT];
        // Disable text initially.
        selectedTracks[TYPE_TEXT] = DISABLED_TRACK;
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
        return !player.getRendererHasMedia(type) ? 0 : trackNames[type].length;
    }

    public String getTrackName(int type, int index) {
        return trackNames[type][index];
    }

    public int getSelectedTrackIndex(int type) {
        return selectedTracks[type];
    }

    public void selectTrack(int type, int index) {
        if (selectedTracks[type] == index) {
            return;
        }
        selectedTracks[type] = index;
        pushTrackSelection(type, true);
        if (type == TYPE_TEXT && index == DISABLED_TRACK) {
            eventProxy.invokeOnCues(Collections.<Cue>emptyList());
        }
    }

    public void setBackgrounded(boolean backgrounded) {
        if (this.backgrounded == backgrounded) {
            return;
        }
        this.backgrounded = backgrounded;
        if (backgrounded) {
            videoTrackToRestore = getSelectedTrackIndex(TYPE_VIDEO);
            selectTrack(TYPE_VIDEO, DISABLED_TRACK);
            blockingClearSurface();
        } else {
            selectTrack(TYPE_VIDEO, videoTrackToRestore);
        }
    }

    public void setMute(boolean isMute) {
        if (this.isMute == isMute) {
            return;
        }
        this.isMute = isMute;
        if (isMute) {
            audioTrackToRestore = getSelectedTrackIndex(TYPE_AUDIO);
            selectTrack(TYPE_AUDIO, DISABLED_TRACK);
        } else {
            selectTrack(TYPE_AUDIO, audioTrackToRestore);
        }
    }

    public void prepare() {
        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILT) {
            player.stop();
        }
        rendererBuilder.cancel();
        eventProxy.setVideoFormat(null);
        videoRenderer = null;
        multiTrackSources = null;
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
     * @param trackNames        The names of the available tracks, indexed by {@link Player} TYPE_*
     *                          constants. May be null if the track names are unknown. An
     *                          individual
     *                          element may be null
     *                          if the track names are unknown for the corresponding type.
     * @param multiTrackSources Sources capable of switching between multiple available tracks,
     *                          indexed by {@link Player} TYPE_* constants. May be null if there
     *                          are
     *                          no types with
     *                          multiple tracks. An individual element may be null if it does not
     *                          have multiple tracks.
     * @param renderers         Renderers indexed by {@link Player} TYPE_* constants. An individual
     *                          element may be null if there do not exist tracks of the
     *                          corresponding type.
     * @param bandwidthMeter    Provides an estimate of the currently available bandwidth. May be
     *                          null.
     */
    void invokeOnRenderersBuilt(String[][] trackNames, MultiTrackChunkSource[] multiTrackSources,
            TrackRenderer[] renderers,
            BandwidthMeter bandwidthMeter) {
        // Normalize the results.
        if (trackNames == null) {
            trackNames = new String[RENDERER_COUNT][];
        }
        if (multiTrackSources == null) {
            multiTrackSources = new MultiTrackChunkSource[RENDERER_COUNT];
        }
        for (int rendererIndex = 0; rendererIndex < RENDERER_COUNT; rendererIndex++) {
            if (renderers[rendererIndex] == null) {
                // Convert a null renderer to a dummy renderer.
                renderers[rendererIndex] = new DummyTrackRenderer();
            }
            if (trackNames[rendererIndex] == null) {
                // Convert a null trackNames to an array of suitable length.
                int trackCount = multiTrackSources[rendererIndex] != null
                        ? multiTrackSources[rendererIndex].getTrackCount() : 1;
                trackNames[rendererIndex] = new String[trackCount];
            }
        }
        // Complete preparation.
        this.trackNames = trackNames;
        this.videoRenderer = renderers[TYPE_VIDEO];
        this.multiTrackSources = multiTrackSources;
        pushSurface(false);
        pushTrackSelection(TYPE_VIDEO, true);
        pushTrackSelection(TYPE_AUDIO, true);
        pushTrackSelection(TYPE_TEXT, true);
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

    void invokeOnVideoSizeChanged(int width, int height, float pixelWidthHeightRatio) {
        for (Listener listener : listeners) {
            listener.onVideoSizeChanged(width, height, pixelWidthHeightRatio);
        }
    }

    boolean isDisabledTrack(int type) {
        return selectedTracks[type] == DISABLED_TRACK;
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

    private void pushTrackSelection(int type, boolean allowRendererEnable) {
        if (multiTrackSources == null) {
            return;
        }

        int trackIndex = selectedTracks[type];
        if (trackIndex == DISABLED_TRACK) {
            player.setRendererEnabled(type, false);
        } else if (multiTrackSources[type] == null) {
            player.setRendererEnabled(type, allowRendererEnable);
        } else {
            boolean playWhenReady = player.getPlayWhenReady();
            player.setPlayWhenReady(false);
            player.setRendererEnabled(type, false);
            player.sendMessage(multiTrackSources[type], MultiTrackChunkSource.MSG_SELECT_TRACK,
                    trackIndex);
            player.setRendererEnabled(type, allowRendererEnable);
            player.setPlayWhenReady(playWhenReady);
        }
    }

}