package com.ecebuc.gesmediaplayer.Audios;

import java.io.Serializable;

public class Album implements Serializable {
    private String albumId;
    private String albumName;
    private String albumArtist;
    private String albumArt;

    public Album(String albumId, String albumName, String albumArtist, String albumArt) {
        this.albumId = albumId;
        this.albumName = albumName;
        this.albumArtist = albumArtist;
        this.albumArt = albumArt;
    }

    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public void setAlbumArtist(String albumArtist) {
        this.albumArtist = albumArtist;
    }

    public void setAlbumArt(String albumArt) {
        this.albumArt = albumArt;
    }

    public String getAlbumId() {

        return albumId;
    }

    public String getAlbumName() {
        return albumName;
    }

    public String getAlbumArtist() {
        return albumArtist;
    }

    public String getAlbumArt() {
        return albumArt;
    }


}
