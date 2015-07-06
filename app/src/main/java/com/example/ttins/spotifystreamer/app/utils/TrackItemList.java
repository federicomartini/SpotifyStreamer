package com.example.ttins.spotifystreamer.app.utils;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kaaes.spotify.webapi.android.models.Image;

public class TrackItemList implements Parcelable {

    private String albumName;
    private String name;
    private String image;
    private List<String> albumImages;
    private String id;
    private String trackPreview_url;

    public TrackItemList() {
        this.albumImages = new ArrayList<String>();
    }

    public TrackItemList(String albumName, String trackName, String trackImage, List<String> albumImages, String artistId, String trackPreview_url) {

        super();
        this.albumImages = new ArrayList<String>();

        this.albumName          = albumName;
        this.name               = trackName;
        this.image              = trackImage;
        this.albumImages        = albumImages;
        this.id                 = artistId;
        this.trackPreview_url   = trackPreview_url;
    }

    public TrackItemList(Parcel parcel) {

        this.albumImages = new ArrayList<String>();

        this.albumName          = parcel.readString();
        this.name               = parcel.readString();
        this.image              = parcel.readString();
        parcel.readStringList(this.albumImages);
        this.id                 = parcel.readString();
        this.trackPreview_url   = parcel.readString();
    }

    public String getId(){
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAlbumName(){
        return this.albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getName(){
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTrackPreview_url(){
        return this.trackPreview_url;
    }

    public void setPreviewUrl(String previewUrl) {
        this.trackPreview_url = previewUrl;
    }

    public String getImage(){
        return this.image;
    }

    public void setImage(String path) {
        this.image = path;
    }

    public void setAlbumImages(List<String> images) {
        Collections.copy(this.albumImages, images);
    }

    public List<String> getAlbumImages() {
        return albumImages;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(albumName);
        dest.writeString(name);
        dest.writeString(image);
        dest.writeStringList(albumImages);
        dest.writeString(id);
        dest.writeString(trackPreview_url);

    }

    public final static Parcelable.Creator CREATOR = new Parcelable.Creator<TrackItemList>() {

        @Override
        public TrackItemList createFromParcel(Parcel source) {
            return new TrackItemList(source);
        }

        @Override
        public TrackItemList[] newArray(int size) {
            return new TrackItemList[size];
        }
    };
}
