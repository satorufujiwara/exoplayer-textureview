package jp.satorufujiwara.player;


import android.net.Uri;

public class VideoSource {

    public Type type = Type.HLS;
    public Uri uri;
    public String contentId;

    public VideoSource(final Uri uri, final Type type) {
        this(uri, type, uri.toString());
    }

    public VideoSource(final Uri uri, final Type type, final String contentId) {
        this.uri = uri;
        this.type = type;
        this.contentId = contentId;
    }

    public enum Type {
        HLS
    }

}
