package jp.satorufujiwara.player;

import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.TransferListener;

import android.content.Context;

public interface DataSourceCreator {

    DataSource create(Context context, TransferListener listener, String userAgent);

}
