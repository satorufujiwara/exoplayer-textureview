package jp.satorufujiwara.player.dash;

import com.google.android.exoplayer.TimeRange;
import com.google.android.exoplayer.dash.DashChunkSource;

import jp.satorufujiwara.player.EventProxy;

public class DashEventProxy extends EventProxy implements DashChunkSource.EventListener {

    /**
     * A listener for debugging information.
     */
    public interface InfoListener {

        void onSeekRangeChanged(TimeRange seekRange);
    }

    private InfoListener infoListener;

    public void setDashInfoListener(InfoListener infoListener) {
        this.infoListener = infoListener;
    }

    /** DashChunkSource.EventListener */
    @Override
    public void onSeekRangeChanged(TimeRange timeRange) {
        if (infoListener != null) {
            infoListener.onSeekRangeChanged(timeRange);
        }
    }

}
