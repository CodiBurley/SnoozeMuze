package com.example.snoozemusic;

import android.content.SharedPreferences;
import java.util.ArrayList;

/**
 * Created by Codi on 12/6/2014.
 */
public class Playlist {

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor playlist_editor;
    private String name; //name that ID's the playlist
    private ArrayList<Song> songList;
    private boolean newList = false;

    public Playlist(String _name, SharedPreferences _pref, ArrayList<Song> _list) {
        name = _name;
        sharedPref = _pref;
        playlist_editor = sharedPref.edit();
        songList = _list;

        /*if this playlist does not exist*/
        if(!sharedPref.getBoolean("created",false)) {
            this.newList = true;
            playlist_editor.putString("name", name);
            playlist_editor.putBoolean("created",true);
            this.initPrefs();
        }
    }

    public void addSong(Song add_song) {
        /*Go through all prefs, and switch boolean value
         *to true of the song that is being added
         */
        for(Song song: songList) {
            if(add_song.getTitle() == song.getTitle()) {
                playlist_editor.putBoolean(add_song.getTitle(),true);
            }
        }
    }

    /*puts all Songs in songList into a boolean key value pair, all
     *initialized to false, with the song title as the key
     */
    public void initPrefs() {
        for(Song song: songList) {
            playlist_editor.putBoolean(song.getTitle(), false);
        }
    }

    public ArrayList<Song> getPlayList() {
        ArrayList<Song> output_list = new ArrayList<Song>();
        for(Song song: songList) {
            if(sharedPref.getBoolean(song.getTitle(), false)) {
                output_list.add(song);
            }
        }
        return output_list;
    }

}
