package com.example.snoozemusic;

public class Song implements Comparable<Song> {

	private long id;
	private String title;
	private String artist;

    @Override
    public int compareTo(Song song) {
        return this.title.compareTo(song.title);
    }

    public Song(long Id, String Title, String Artist) {
		id = Id;
		title = Title;
		artist = Artist;
	}
	
	public long getId() { return id; }
	public String getTitle() { return title; }
	public String getArtist() { return artist; }
	
}
