package jp.satorufujiwara.player.streaming;


import com.google.android.exoplayer.drm.StreamingDrmSessionManager;

import jp.satorufujiwara.player.EventProxy;

public class StreamingEventProxy extends EventProxy implements
        StreamingDrmSessionManager.EventListener {

    public interface InternalErrorListener {

        void onDrmSessionManagerError(Exception e);
    }

    private InternalErrorListener internalErrorListener;

    public void setStreamingInternalErrorListener(InternalErrorListener internalErrorListener) {
        this.internalErrorListener = internalErrorListener;
    }

    /** StreamingDrmSessionManager.EventListener */
    @Override
    public void onDrmSessionManagerError(Exception e) {
        if (internalErrorListener != null) {
            internalErrorListener.onDrmSessionManagerError(e);
        }
    }

}
