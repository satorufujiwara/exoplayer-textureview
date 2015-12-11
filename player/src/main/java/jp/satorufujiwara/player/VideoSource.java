package jp.satorufujiwara.player;


import android.content.Context;
import android.net.Uri;
import android.os.Handler;

public abstract class VideoSource {

    public final Uri uri;
    public final String contentId;
    public final String userAgent;
    public final Handler eventHandler;
    public final int bufferSegmentSize;
    public final int bufferSegmentCount;

    protected VideoSource(final Uri uri, final String userAgent, final Handler eventHandler,
            final int bufferSegmentSize, int bufferSegmentCount) {
        this(uri, uri.toString(), userAgent, eventHandler, bufferSegmentSize, bufferSegmentCount);
    }

    protected VideoSource(final Uri uri, final String contentId, final String userAgent,
            final Handler eventHandler, final int bufferSegmentSize, int bufferSegmentCount) {
        this.uri = uri;
        this.contentId = contentId;
        this.userAgent = userAgent;
        this.eventHandler = eventHandler;
        this.bufferSegmentSize = bufferSegmentSize;
        this.bufferSegmentCount = bufferSegmentCount;
    }

    public abstract RendererBuilder createRendererBuilder(final Context context);

}
