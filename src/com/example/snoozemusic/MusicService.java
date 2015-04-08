package com.example.snoozemusic;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import java.util.ArrayList;
import android.content.ContentUris;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import java.util.Random;
import android.app.Notification;
import android.app.PendingIntent;
import android.widget.Toast;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private MediaPlayer player;
    private ArrayList<Song> songs;
    private int songPosn;
    private String songTitle="";
    private static final int NOTIFY_ID=1;
    private boolean shuffle = false;
    private boolean sleep_mode = false;
    private AudioManager volume_control;

    private final IBinder musicBind = new MusicBinder();

    @Override
    public IBinder onBind(Intent arg0) {
        return musicBind;
    }

    public void onCreate() {
        //creates service
        super.onCreate();
        //initialize variables
        songPosn = 0;
        player = new MediaPlayer();
        //initializing variables for volume control
        volume_control = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        initMusicPlayer();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        player.stop();
        player.release();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        int current_volume = volume_control.getStreamVolume(AudioManager.STREAM_MUSIC);
        boolean finished = false;
        if(mp.getCurrentPosition() > 0) {
            mp.reset();
            if(sleep_mode) {
                volume_control.setStreamVolume(AudioManager.STREAM_MUSIC, current_volume - 1, 0);
                if(volume_control.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
                    finished = true; //if volume reaches zero
                }
            }
            if(finished){
                player.stop();
            } else {
                this.playNext();
            }
        }

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //start playback
        mp.start();

        //For Navigating back to app after exiting
        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.android_music_player_play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(songTitle);
        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);

        // Broadcast intent to activity to let it know the media player has been prepared
        Intent on_prep_intent = new Intent("MEDIA_PLAYER_PREPARED");
        LocalBroadcastManager.getInstance(this).sendBroadcast(on_prep_intent);

    }

    public void initMusicPlayer() {
        //set player properties
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    public void setList(ArrayList<Song> theSongs) {
        songs=theSongs;
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void playSong(){
        player.reset();

        //get song
        Song playSong = songs.get(songPosn);
        //get title
        songTitle = playSong.getTitle();
        //get id
        long currSong = playSong.getId();
        //set uri
        Uri trackUri = ContentUris.withAppendedId(android.provider
                        .MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);
        try{
            player.setDataSource(getApplicationContext(), trackUri);
        } catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
        player.prepareAsync();
    }

    public void setSong(int songIndex){
        songPosn=songIndex;
    }

    public int getPosn(){
        return player.getCurrentPosition();
    }

    public int getDur(){
        return player.getDuration();
    }

    public boolean isPlaying(){
        return player.isPlaying();
    }

    public void pausePlayer(){
        player.pause();
    }

    public void seek(int posn){
        player.seekTo(posn);
    }

    public void go(){
        player.start();
    }

    public void playPrev(){
        songPosn--;
        if(songPosn == 0) {
            songPosn=songs.size()-1;
        }
        playSong();
    }

    public void playNext() {
        if(!shuffle) {
            songPosn++;
            if (songPosn == songs.size() - 1) {
                songPosn = 0;
            }
            playSong();
        } else {
            Random rand = new Random();
            int currentPosn = songPosn;
            while(currentPosn == songPosn) {
                songPosn = rand.nextInt(songs.size());
            }
            playSong();
        }
    }

    public boolean toggleShuffle() {
        shuffle = !shuffle;
        return shuffle;
    }

    public boolean toggleSleep() {
        sleep_mode = !sleep_mode;
        return sleep_mode;
    }

}
