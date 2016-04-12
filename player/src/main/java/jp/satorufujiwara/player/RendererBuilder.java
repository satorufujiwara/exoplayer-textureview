package jp.satorufujiwara.player;


import android.content.Context;
import android.net.Uri;
import android.os.Handler;

public abstract class RendererBuilder<T extends EventProxy> {

    public static final int DEFAULT_BUFFER_SEGMENT_SIZE = 64 * 1024;
    public static final int DEFAULT_MAIN_BUFFER_SEGMENT_COUNT = 256;
    public static final int DEFAULT_AUDIO_BUFFER_SEGMENTS = 54;
    public static final int DEFAULT_TEXT_BUFFER_SEGMENT_COUNT = 2;

    public final Context context;
    public final Handler eventHandler;
    public final String userAgent;
    public final Uri uri;
    public final T eventProxy;
    public final int bufferSegmentSize;
    public final int bufferSegmentCount;

    protected RendererBuilder(Context context, Handler eventHandler, T eventProxy,
            String userAgent, Uri uri, int bufferSegmentSize, int bufferSegmentCount) {
        this.context = context;
        this.eventHandler = eventHandler;
        this.eventProxy = eventProxy;
        this.userAgent = userAgent;
        this.uri = uri;
        this.bufferSegmentSize = bufferSegmentSize;
        this.bufferSegmentCount = bufferSegmentCount;
    }

    /**
     * Builds renderers for playback.
     *
     * @param callback The player for which renderers are being built.
     *                 {@link RendererBuilderCallback#onRenderers}
     *                 should be invoked once the renderers have been built. If building fails,
     *                 {@link RendererBuilderCallback#onRenderersError} should be invoked.
     */
    protected abstract void buildRenderers(RendererBuilderCallback callback);

    /**
     * Cancels the current build operation, if there is one. Else does nothing.
     * A canceled build operation must not invoke {@link RendererBuilderCallback#onRenderers} or
     * {@link RendererBuilderCallback#onRenderersError} on the player, which may have been
     * released.
     */
    protected abstract void cancel();

    protected abstract void setLimitBitrate(long bitrate);

}
