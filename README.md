exoplayer-textureview
===

[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Download](https://api.bintray.com/packages/satorufujiwara/maven/exoplayer-textureview/images/download.svg)](https://bintray.com/satorufujiwara/maven/exoplayer-textureview/_latestVersion)

*This library is experimental. API and all codes will be changed without notice*

[ExoPlayer](https://github.com/google/ExoPlayer)'s wrapper for using with TextureView.

ExoPlayer's version is [r1.5.7](https://github.com/google/ExoPlayer/blob/master/RELEASENOTES.md#r157)

# Features
* Play HLS playlist
* Play local videos in assets folder.
* Set bitrate limit
* Mute / unmute

# Gradle

```groovy
repositories {
    jcenter()
}
dependencies {
    compile 'jp.satorufujiwara:exoplayer-textureview:0.6.5'
    compile 'com.google.android.exoplayer:exoplayer:r1.5.7'
}
```

# Usage

Create `VideoTexturePresenter`'s instance and bind to `Fragment`'s or `Activity`'s lifecycle.

```java
@Override
public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    videoTexturePresenter = new VideoTexturePresenter(videoTextureView);
    videoTexturePresenter.onCreate();
    VideoSource source = HlsVideoSource
                .newBuilder(Uri.parse("hls playlist url."), "UserAgent")
                .bufferSegmentSize(64 * 1024)
                .bufferSegmentCount(512)
                .build();
    videoTexturePresenter.setSource(source);
    videoTexturePresenter.prepare();
}

@Override
public void onDestroyView() {
    videoTexturePresenter.release();
    videoTexturePresenter.onDestroy();
    super.onDestroyView();
}
```

And play or pause.

```java
videoTexturePresenter.play();
videoTexturePresenter.seekTo(0);
videoTexturePresenter.setMute(true);
videoTexturePresenter.pause();
```

License
-------

    Copyright 2015 Satoru Fujiwara

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

ExoPlayer and ExoPlayer demo.

    Copyright (C) 2014 The Android Open Source Project
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
