package jp.satorufujiwara.player;


import android.content.Context;
import android.net.Uri;
import android.os.Handler;

public abstract class RendererBuilder<T extends EventProxy> {

    static final int DEFAULT_BUFFER_SEGMENT_SIZE = 64 * 1024;
    static final int DEFAULT_BUFFER_SEGMENT_COUNT = 256;

    private final Context context;
    private final Handler eventHandler;
    private final String userAgent;
    private final Uri uri;
    private final T eventProxy;
    private final int bufferSegmentSize;
    private final int bufferSegmentCount;

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

    public T getEventProxy() {
        return eventProxy;
    }

    public Context getContext() {
        return context;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Uri getUri() {
        return uri;
    }

    public Handler getEventHandler() {
        return eventHandler;
    }

    public int getBufferSegmentSize() {
        return bufferSegmentSize;
    }

    public int getBufferSegmentCount() {
        return bufferSegmentCount;
    }

}
