package com.example.newmp3playerproject;

import java.io.Serializable;

public class MusicData implements Serializable {
    private String id;
    private String albumId;
    private String title;
    private String artist;
    private String star;

    public MusicData(String id, String albumId, String title, String artist, String star) {
        this.id = id;
        this.albumId = albumId;
        this.title = title;
        this.artist = artist;
        this.star = star;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAlbumId() {
        return albumId;
    }

    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getStar() {
        return star;
    }

    public void setStar(String star) {
        this.star = star;
    }

    @Override
    public String toString() {
        return "id= " + id + ", albumId= " + albumId + ", title= " + title + ", artist= " + artist + ", star= " + star;
    }
}
