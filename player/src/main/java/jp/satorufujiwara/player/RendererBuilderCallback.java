package jp.satorufujiwara.player;


import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.chunk.MultiTrackChunkSource;
import com.google.android.exoplayer.upstream.BandwidthMeter;

public interface RendererBuilderCallback {

    /**
     * Invoked with the results from a {@link RendererBuilder}.
     *
     * @param trackNames        The names of the available tracks, indexed by {@link Player} TYPE_*
     *                          constants. May be null if the track names are unknown. An individual element may be null
     *                          if the track names are unknown for the corresponding type.
     * @param multiTrackSources Sources capable of switching between multiple available tracks,
     *                          indexed by {@link Player} TYPE_* constants. May be null if there are no types with
     *                          multiple tracks. An individual element may be null if it does not have multiple tracks.
     * @param renderers         Renderers indexed by {@link Player} TYPE_* constants. An individual
     *                          element may be null if there do not exist tracks of the corresponding type.
     * @param bandwidthMeter    Provides an estimate of the currently available bandwidth. May be null.
     */
    void onRenderersBuilt(String[][] trackNames, MultiTrackChunkSource[] multiTrackSources,
            TrackRenderer[] renderers,
            BandwidthMeter bandwidthMeter);

    /**
     * Invoked if a {@link RendererBuilder} encounters an error.
     *
     * @param e Describes the error.
     */
    void onRenderersError(Exception e);

}
