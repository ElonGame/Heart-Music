package com.musicplayer.heartmusic;

/*
 * This is demo code to accompany the Mobiletuts+ series:
 * Android SDK: Creating a Music Player
 * 
 * Sue Smith - February 2014
 */

public class Song {
	
	private long id;
	private String title;
	private String artist;
    private int bpm;

	public Song(long songID, String songTitle, String songArtist, int songBpm){
		id=songID;
		title=songTitle;
		artist=songArtist;
        bpm=songBpm;
	}
	
	public long getID(){return id;}
	public String getTitle(){return title;}
	public String getArtist(){return artist;}
    public int getBpm(){return bpm;}
    public void setBpm(int songBpm){
        bpm=songBpm;
    }
}
