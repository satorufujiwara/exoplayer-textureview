package jp.satorufujiwara.player;

import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.List;

public class VideoTexturePresenter implements Player.Listener,
        AudioCapabilitiesReceiver.Listener {

    private final VideoTextureView textureView;
    private final List<OnStateChangedListener> onStateChangedListeners = new ArrayList<>();
    private final List<OnErrorListener> onErrorListeners = new ArrayList<>();
    private final List<OnVideoSizeChangedListener> onVideoSizeChangedListeners = new ArrayList<>();

    private Player player;
    private AudioCapabilitiesReceiver audioCapabilitiesReceiver;
    private RendererBuilder rendererBuilder;
    private long limitBitrate = Long.MAX_VALUE;
    private boolean playerNeedsPrepare;
    private SurfaceTexture surfaceTexture;

    public VideoTexturePresenter(final VideoTextureView view) {
        this.textureView = view;
        audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(view.getContext(), this);
        textureView.setSurfaceTextureListener(
                new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
                            int height) {
                        surfaceTexture = surface;
                        if (player != null) {
                            player.setSurface(new Surface(surface));
                        }
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
                            int height) {
                        //no op
                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                        surfaceTexture = null;
                        if (player != null) {
                            player.blockingClearSurface();
                        }
                        playerNeedsPrepare = true;
                        return true;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                        //no op
                    }
                }

        );
    }

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        fireOnStateChanged(playWhenReady, playbackState);
    }

    @Override
    public void onError(Exception e) {
        fireOnError(e);
    }


    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
            float pixelWidthHeightRatio) {
        textureView.setVideoWidthHeightRatio(
                height == 0 ? 1 : (width * pixelWidthHeightRatio) / height);
        fireOnVideoSizeChanged(width, height, pixelWidthHeightRatio);
    }

    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        if (player == null) {
            return;
        }
        boolean backgrounded = player.getBackgrounded();
        boolean playWhenReady = player.getPlayWhenReady();
        release();
        prepare();
        if (playWhenReady) {
            play();
        }
        player.setBackgrounded(backgrounded);
    }

    public EventProxy eventListeners() {
        return rendererBuilder != null ? rendererBuilder.eventProxy : null;
    }

    public void setLimitBitrate(final long limitBitrate) {
        this.limitBitrate = limitBitrate;
        if (rendererBuilder != null) {
            rendererBuilder.setLimitBitrate(limitBitrate);
        }
    }

    public void setSource(final VideoSource source) {
        rendererBuilder = source.createRendererBuilder(textureView.getContext());
        rendererBuilder.setLimitBitrate(limitBitrate);
        playerNeedsPrepare = true;
    }

    public void onCreate() {
        audioCapabilitiesReceiver.register();
    }

    public void onDestroy() {
        audioCapabilitiesReceiver.unregister();
    }

    public void prepare() {
        if (player == null) {
            player = new Player();
            player.addListener(this);
            if (surfaceTexture != null) {
                player.setSurface(new Surface(surfaceTexture));
            }
        }
        playerNeedsPrepare = true;
        if (rendererBuilder == null) {
            return;
        }
        player.setRendererBuilder(rendererBuilder);
    }

    public void release() {
        if (player == null) {
            return;
        }
        player.removeListener(this);
        player.release();
        player = null;
    }

    public void play() {
        if (player == null) {
            prepare();
        }
        if (rendererBuilder == null) {
            return;
        }
        if (playerNeedsPrepare) {
            player.prepare();
            playerNeedsPrepare = false;
        }
        player.setPlayWhenReady(true);
    }

    public void pause() {
        if (player == null) {
            return;
        }
        player.setPlayWhenReady(false);
    }

    public void stop() {
        if (player == null) {
            return;
        }
        player.stop();
        player.seekTo(0);
    }

    public void seekTo(final long positionMs) {
        if (player == null) {
            return;
        }
        player.seekTo(positionMs);
    }

    public void setMute(final boolean isMute) {
        if (player == null) {
            return;
        }
        player.setMute(isMute);
    }

    public long getDuration() {
        return player == null ? 0 : player.getDuration();
    }

    public long getCurrentPosition() {
        return player == null ? 0 : player.getCurrentPosition();
    }

    public int getBufferedPercentage() {
        return player == null ? 0 : player.getBufferedPercentage();
    }

    public long getBufferedPosition() {
        return player == null ? 0 : player.getBufferedPosition();
    }

    public boolean isPlaying() {
        return player != null && player.getPlayWhenReady();
    }

    public int getPlaybackState() {
        return player == null ? -1 : player.getPlaybackState();
    }

    public void addOnStateChangedListener(OnStateChangedListener l) {
        if (l != null) {
            onStateChangedListeners.add(l);
        }
    }

    public void removeOnStateChangedListener(OnStateChangedListener l) {
        onStateChangedListeners.remove(l);
    }

    public void clearAllOnStateChangedListener() {
        onStateChangedListeners.clear();
    }

    public void addOnErrorListener(OnErrorListener l) {
        if (l != null) {
            onErrorListeners.add(l);
        }
    }

    public void removeOnErrorListener(OnErrorListener l) {
        onErrorListeners.remove(l);
    }

    public void clearOnErrorListener() {
        onErrorListeners.clear();
    }

    public void addOnVideoSizeChangedListener(OnVideoSizeChangedListener l) {
        if (l != null) {
            onVideoSizeChangedListeners.add(l);
        }
    }

    public void removeOnVideoSizeChangedListener(OnVideoSizeChangedListener l) {
        onVideoSizeChangedListeners.remove(l);
    }

    public void clearOnVideoSizeChangedListener() {
        onVideoSizeChangedListeners.clear();
    }

    private void fireOnStateChanged(final boolean playWhenReady, final int playbackState) {
        for (OnStateChangedListener l : onStateChangedListeners) {
            l.onStateChanged(playWhenReady, playbackState);
        }
    }

    private void fireOnError(final Exception e) {
        for (OnErrorListener l : onErrorListeners) {
            l.onError(e);
        }
    }

    private void fireOnVideoSizeChanged(final int width, final int height,
            final float pixelWidthHeightRatio) {
        for (OnVideoSizeChangedListener l : onVideoSizeChangedListeners) {
            l.onVideoSizeChanged(width, height, pixelWidthHeightRatio);
        }
    }

    public interface OnStateChangedListener {

        void onStateChanged(boolean playWhenReady, int playbackState);
    }

    public interface OnErrorListener {

        void onError(Exception e);
    }

    public interface OnVideoSizeChangedListener {

        void onVideoSizeChanged(int width, int height, float pixelWidthHeightRatio);
    }
}
