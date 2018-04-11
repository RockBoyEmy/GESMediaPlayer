package com.ecebuc.gesmediaplayer.Audios;

import java.io.Serializable;

public class Artist implements Serializable {
    private String artistId;
    private String artistName;
    private String artistTracksNo;
    private String artistAlbumsNo;

    public Artist(String artistId, String artistName, String artistAlbumsNo, String artistTracksNo) {
        this.artistId = artistId;
        this.artistName = artistName;
        this.artistAlbumsNo = artistAlbumsNo;
        this.artistTracksNo = artistTracksNo;
    }

    public String getArtistId() {
        return artistId;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getArtistTracksNo() {
        return artistTracksNo;
    }

    public String getArtistAlbumsNo() {
        return artistAlbumsNo;
    }

    public void setArtistId(String artistId) {
        this.artistId = artistId;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public void setArtistTracksNo(String artistTracksNo) {
        this.artistTracksNo = artistTracksNo;
    }

    public void setArtistAlbumsNo(String artistAlbumsNo) {
        this.artistAlbumsNo = artistAlbumsNo;
    }
}
