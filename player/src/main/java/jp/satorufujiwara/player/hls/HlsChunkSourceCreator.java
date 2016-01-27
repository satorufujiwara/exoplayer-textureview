package jp.satorufujiwara.player.hls;

import com.google.android.exoplayer.hls.HlsChunkSource;
import com.google.android.exoplayer.hls.HlsPlaylist;
import com.google.android.exoplayer.hls.PtsTimestampAdjusterProvider;
import com.google.android.exoplayer.upstream.BandwidthMeter;
import com.google.android.exoplayer.upstream.DataSource;

public interface HlsChunkSourceCreator {

    HlsChunkSource create(DataSource dataSource, String playlistUrl, HlsPlaylist playlist,
            BandwidthMeter bandwidthMeter, PtsTimestampAdjusterProvider timestampAdjusterProvider,
            int[] variantIndices);

}
