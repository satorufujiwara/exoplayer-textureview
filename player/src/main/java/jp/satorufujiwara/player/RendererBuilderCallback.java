package jp.satorufujiwara.player;


import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.upstream.BandwidthMeter;

public interface RendererBuilderCallback {

    /**
     * Invoked with the results from a {@link RendererBuilder}.
     *
     * @param renderers      Renderers indexed by {@link Player} TYPE_* constants. An individual
     *                       element may be null if there do not exist tracks of the corresponding
     *                       type.
     * @param bandwidthMeter Provides an estimate of the currently available bandwidth. May be
     *                       null.
     */
    void onRenderers(TrackRenderer[] renderers, BandwidthMeter bandwidthMeter);

    /**
     * Invoked if a {@link RendererBuilder} encounters an error.
     *
     * @param e Describes the error.
     */
    void onRenderersError(Exception e);

}
