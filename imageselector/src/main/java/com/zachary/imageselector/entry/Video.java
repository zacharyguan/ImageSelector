package com.zachary.imageselector.entry;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class Video extends Image implements Parcelable {
    private String duration;

    public Video(String path, long time, String name, String mimeType, Uri uri, String duration) {
        super(path, time, name, mimeType, uri);
        this.duration = duration;
    }

    protected Video(Parcel in) {
        super(in);
        duration = in.readString();
    }

    public static final Creator<Video> CREATOR = new Creator<Video>() {
        @Override
        public Video createFromParcel(Parcel in) {
            return new Video(in);
        }

        @Override
        public Video[] newArray(int size) {
            return new Video[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(duration);
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}
