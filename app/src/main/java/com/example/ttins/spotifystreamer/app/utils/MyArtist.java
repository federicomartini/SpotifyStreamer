package com.example.ttins.spotifystreamer.app.utils;


public class MyArtist {

    public String image;
    public String name;
    public String id;
    public int followers;
    public int popularity;

    public MyArtist() {
        super();
    }

    public MyArtist(String artistName, String artistImage, String artistId, int artistFollowers,
                    int artistPopularity) {

        super();
        this.name   = artistName;
        this.image  = artistImage;
        this.id     = artistId;
        this.followers = artistFollowers;
        this.popularity = artistPopularity;
    }

}
