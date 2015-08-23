package jp.satorufujiwara.player.sample;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;
import butterknife.InjectView;
import jp.satorufujiwara.player.VideoTexturePresenter;
import jp.satorufujiwara.player.VideoTextureView;

public class PlayerSampleFragment extends Fragment {

    public static PlayerSampleFragment newInstance() {
        return new PlayerSampleFragment();
    }

    @InjectView(R.id.videoTextureView)
    VideoTextureView videoTextureView;

    VideoTexturePresenter videoTexturePresenter;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_player_sample, container, false);
        ButterKnife.inject(this, v);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        videoTexturePresenter = new VideoTexturePresenter(videoTextureView);

        //TODO play hls
    }
}
