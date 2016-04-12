package jp.satorufujiwara.player;

import com.google.android.exoplayer.upstream.BandwidthMeter;
import com.google.android.exoplayer.util.Assertions;
import com.google.android.exoplayer.util.Clock;
import com.google.android.exoplayer.util.SlidingPercentile;
import com.google.android.exoplayer.util.SystemClock;

import android.os.Handler;

public class LimitedBandwidthMeter implements BandwidthMeter {

    public static final int DEFAULT_MAX_WEIGHT = 2000;

    private long limitBitrate = Long.MAX_VALUE;
    private final Handler eventHandler;
    private final BandwidthMeter.EventListener eventListener;
    private final Clock clock;
    private final SlidingPercentile slidingPercentile;
    private long bytesAccumulator;
    private long startTimeMs;
    private long bitrateEstimate;
    private int streamCount;

    public LimitedBandwidthMeter() {
        this(null, null);
    }

    public LimitedBandwidthMeter(Handler eventHandler, EventListener eventListener) {
        this(eventHandler, eventListener, DEFAULT_MAX_WEIGHT);
    }

    public LimitedBandwidthMeter(Handler eventHandler, EventListener eventListener, int maxWeight) {
        this(eventHandler, eventListener, new SystemClock(), maxWeight);
    }

    public LimitedBandwidthMeter(Handler eventHandler, EventListener eventListener, Clock clock,
            int maxWeight) {
        this.eventHandler = eventHandler;
        this.eventListener = eventListener;
        this.clock = clock;
        this.slidingPercentile = new SlidingPercentile(maxWeight);
        bitrateEstimate = NO_ESTIMATE;
    }

    @Override
    public synchronized long getBitrateEstimate() {
        return Math.min(bitrateEstimate, limitBitrate);
    }

    @Override
    public synchronized void onTransferStart() {
        if (streamCount == 0) {
            startTimeMs = clock.elapsedRealtime();
        }
        streamCount++;
    }

    @Override
    public synchronized void onBytesTransferred(int bytes) {
        bytesAccumulator += bytes;
    }

    @Override
    public synchronized void onTransferEnd() {
        Assertions.checkState(streamCount > 0);
        long nowMs = clock.elapsedRealtime();
        int elapsedMs = (int) (nowMs - startTimeMs);
        if (elapsedMs > 0) {
            float bitsPerSecond = (bytesAccumulator * 8000) / elapsedMs;
            slidingPercentile.addSample((int) Math.sqrt(bytesAccumulator), bitsPerSecond);
            float bandwidthEstimateFloat = slidingPercentile.getPercentile(0.5f);
            bitrateEstimate = Float.isNaN(bandwidthEstimateFloat) ? NO_ESTIMATE
                    : (long) bandwidthEstimateFloat;
            notifyBandwidthSample(elapsedMs, bytesAccumulator, bitrateEstimate);
        }
        streamCount--;
        if (streamCount > 0) {
            startTimeMs = nowMs;
        }
        bytesAccumulator = 0;
    }

    public void setLimitBitrate(long limitBitrate) {
        this.limitBitrate = limitBitrate;
    }

    private void notifyBandwidthSample(final int elapsedMs, final long bytes, final long bitrate) {
        if (eventHandler != null && eventListener != null) {
            eventHandler.post(new Runnable() {
                @Override
                public void run() {
                    eventListener.onBandwidthSample(elapsedMs, bytes, bitrate);
                }
            });
        }
    }

}
