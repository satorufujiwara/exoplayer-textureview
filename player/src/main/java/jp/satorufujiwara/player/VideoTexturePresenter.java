package jp.satorufujiwara.player;

import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;

import jp.satorufujiwara.player.hls.HlsRendererBuilder;

public class VideoTexturePresenter implements Player.Listener,
        AudioCapabilitiesReceiver.Listener {

    private final VideoTextureView textureView;
    private final Callbacks callbacks = new Callbacks();

    private Player player;
    private AudioCapabilitiesReceiver audioCapabilitiesReceiver;
    private RendererBuilder rendererBuilder;
    private long limitBitrate = Long.MAX_VALUE;
    private boolean playerNeedsPrepare;
    private long playerPosition;

    public VideoTexturePresenter(final VideoTextureView view) {
        this.textureView = view;
        audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(view.getContext(), this);
        textureView.setSurfaceTextureListener(
                new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
                            int height) {
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
        callbacks.fireOnStateChanged(playWhenReady, playbackState);
    }

    @Override
    public void onError(Exception e) {
        callbacks.fireOnError(e);
    }


    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
            float pixelWidthHeightRatio) {
        textureView.setVideoWidthHeightRatio(
                height == 0 ? 1 : (width * pixelWidthHeightRatio) / height);
        callbacks.fireOnVideoSizeChanged(width, height, pixelWidthHeightRatio);
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

    public Callbacks callback() {
        return callbacks;
    }

    public EventProxy eventListeners() {
        return rendererBuilder != null ? rendererBuilder.getEventProxy() : null;
    }

    public void setLimitBitrate(final long limitBitrate) {
        this.limitBitrate = limitBitrate;
        if (rendererBuilder != null) {
            rendererBuilder.setLimitBitrate(limitBitrate);
        }
    }

    public void setSource(final VideoSource source, final String userAgent) {
        rendererBuilder = createRendererBuilder(source, userAgent);
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
        playerPosition = player.getCurrentPosition();
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

    private RendererBuilder createRendererBuilder(final VideoSource source,
            final String userAgent) {
        switch (source.type) {
            case HLS:
                return new HlsRendererBuilder.Builder(textureView.getContext())
                        .userAgent(userAgent)
                        .url(source.uri.toString())
                        .build();
        }
        throw new IllegalArgumentException("Current source.type is not supported.");
    }

    public boolean isPlaying() {
        return player != null && player.getPlayWhenReady();
    }

    public int getPlaybackState() {
        return player == null ? -1 : player.getPlaybackState();
    }

    public static class Callbacks {

        private OnStateChangedListener onStateChangedListener;

        private OnErrorListener onErrorListener;

        private OnVideoSizeChangedListener onVideoSizeChangedListener;

        public Callbacks onStateChanged(final OnStateChangedListener l) {
            onStateChangedListener = l;
            return this;
        }

        void fireOnStateChanged(boolean playWhenReady, int playbackState) {
            if (onStateChangedListener != null) {
                onStateChangedListener.onStateChanged(playWhenReady, playbackState);
            }
        }

        public Callbacks onError(final OnErrorListener l) {
            onErrorListener = l;
            return this;
        }

        void fireOnError(final Exception e) {
            if (onErrorListener != null) {
                onErrorListener.onError(e);
            }
        }

        public Callbacks onVideoSizeChanged(final OnVideoSizeChangedListener l) {
            onVideoSizeChangedListener = l;
            return this;
        }

        void fireOnVideoSizeChanged(final int width, final int height,
                final float pixelWidthHeightRatio) {
            if (onVideoSizeChangedListener != null) {
                onVideoSizeChangedListener.onVideoSizeChanged(width, height, pixelWidthHeightRatio);
            }
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
