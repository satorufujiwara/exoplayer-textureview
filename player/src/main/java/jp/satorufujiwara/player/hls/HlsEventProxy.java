package jp.satorufujiwara.player.hls;

import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.hls.HlsSampleSource;

import java.io.IOException;

import jp.satorufujiwara.player.EventProxy;
import jp.satorufujiwara.player.Player;

public class HlsEventProxy extends EventProxy implements HlsSampleSource.EventListener {

    public interface InfoListener {

        void onVideoFormatEnabled(Format format, int trigger, int mediaTimeMs);

        void onAudioFormatEnabled(Format format, int trigger, int mediaTimeMs);

        void onLoadStarted(int sourceId, long length, int type, int trigger, Format format,
                int mediaStartTimeMs, int mediaEndTimeMs);

        void onLoadCompleted(int sourceId, long bytesLoaded, int type, int trigger, Format format,
                int mediaStartTimeMs, int mediaEndTimeMs, long elapsedRealtimeMs,
                long loadDurationMs);
    }

    public interface InternalErrorListener {

        void onLoadError(int sourceId, IOException e);
    }

    private InfoListener infoListener;
    private InternalErrorListener internalErrorListener;

    public void setHlsInfoListener(InfoListener infoListener) {
        this.infoListener = infoListener;
    }

    public void setHlsInternalErrorListener(
            InternalErrorListener internalErrorListener) {
        this.internalErrorListener = internalErrorListener;
    }

    /** HlsSampleSource.EventListener */
    @Override
    public void onLoadStarted(int sourceId, long length, int type, int trigger, Format format,
            int mediaStartTimeMs, int mediaEndTimeMs) {
        if (infoListener != null) {
            infoListener.onLoadStarted(sourceId, length, type, trigger, format, mediaStartTimeMs,
                    mediaEndTimeMs);
        }
    }

    /** HlsSampleSource.EventListener */
    @Override
    public void onLoadCompleted(int sourceId, long bytesLoaded, int type, int trigger, Format format,
            int mediaStartTimeMs, int mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs) {
        if (infoListener != null) {
            infoListener.onLoadCompleted(sourceId, bytesLoaded, type, trigger, format, mediaStartTimeMs,
                    mediaEndTimeMs, elapsedRealtimeMs, loadDurationMs);
        }
    }

    /** HlsSampleSource.EventListener */
    @Override
    public void onLoadCanceled(int sourceId, long bytesLoaded) {
        // Do nothing.
    }

    /** HlsSampleSource.EventListener */
    @Override
    public void onLoadError(int sourceId, IOException e) {
        if (internalErrorListener != null) {
            internalErrorListener.onLoadError(sourceId, e);
        }
    }

    /** HlsSampleSource.EventListener */
    @Override
    public void onUpstreamDiscarded(int sourceId, int mediaStartTimeMs, int mediaEndTimeMs) {
        // Do nothing.
    }

    /** HlsSampleSource.EventListener */
    @Override
    public void onDownstreamFormatChanged(int sourceId, Format format, int trigger, int mediaTimeMs) {
        if (infoListener == null) {
            return;
        }
        if (sourceId == Player.TYPE_VIDEO) {
            setVideoFormat(format);
            infoListener.onVideoFormatEnabled(format, trigger, mediaTimeMs);
        } else if (sourceId == Player.TYPE_AUDIO) {
            infoListener.onAudioFormatEnabled(format, trigger, mediaTimeMs);
        }
    }

}
