package com.example.snoozemusic;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;

import com.example.snoozemusic.MusicService.MusicBinder;

import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.widget.ListView;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.Toast;

public class MusicActivity extends Activity implements MediaPlayerControl {

    private ArrayList<Song> songList;
    private ListView songView;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound=false;
    private boolean paused = false, playback_paused = false;
    private MusicController controller;
    private MenuItem shuffle_toggle;
    private MenuItem shuffle_sleep;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.song_list);
        this.initVars();
        this.getSongList();
        SongAdapter songAd = new SongAdapter(songList, click, this);
        songView.setAdapter(songAd);
        setController();
    }

    private View.OnClickListener click = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

        }
    };

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @Override
    protected void onStart() {
        if(playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection,Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
        super.onStart();
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.start, menu);
        shuffle_toggle = menu.findItem(R.id.action_shuffle);
        shuffle_sleep = menu.findItem(R.id.action_sleep);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()) {
            case R.id.action_shuffle: //toggles shuffle and switches icons
                if(!musicSrv.toggleShuffle()) {
                    shuffle_toggle.setIcon(getResources().getDrawable(R.drawable.ic_action_shuffle_off));
                } else {
                    shuffle_toggle.setIcon(getResources().getDrawable(R.drawable.ic_action_shuffle));
                }
                break;
            case R.id.action_sleep: //toggles sleep mode and switches icons
                if(!musicSrv.toggleSleep()) {
                    Toast t = Toast.makeText(this,"sleep pressed", Toast.LENGTH_LONG);
                    t.show();
                    shuffle_sleep.setIcon(getResources().getDrawable(R.drawable.ic_action_sleep_off));
                } else {
                    shuffle_sleep.setIcon(getResources().getDrawable(R.drawable.ic_action_sleep_on));
                }
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicSrv=null;
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv=null;
        System.exit(0);
        super.onDestroy();
    }


    private void initVars() {
        songList = new ArrayList<Song>();
        songView = (ListView) findViewById(R.id.song_list);
    }

    private void getSongList() {

        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
    	
    	/*iterate through songs*/
        if(musicCursor!=null && musicCursor.moveToFirst()){
            //get columns
            int titleColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST);
            int timeColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DURATION);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                //gets rid of useless tones that aren't songs
                if(musicCursor.getLong(timeColumn) > 5000) {
                    songList.add(new Song(thisId, thisTitle, thisArtist));
                }
            } while (musicCursor.moveToNext());
        }
        Collections.sort(songList);
    }

    //onClick for item in list
    public void songPicked(View view){
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();
        setController();
        if(playback_paused) {
            playback_paused = false;
        }
        controller.setPressed(true);
        controller.show(0);

    }

    // Broadcast receiver to determine when music player has been prepared
    private BroadcastReceiver onPrepareReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            // When music player has been prepared, show controller
            controller.show(0);
        }
    };

    /***************Media Controller****************/
    public void setController() {
        if(controller == null) {
            this.controller = new MusicController(this);
        }

        controller.setPrevNextListeners(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        playNext();
                    }
                },
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        playPrev();
                    }
                });


        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
    }


    /*Methods used for the media controller, these methods
     *call methods from the musicService in order to control
     * the playback of songs.
     */
    @Override
    public void start() {
        musicSrv.go();
    }

    @Override
    public void pause() {
        musicSrv.pausePlayer();
        playback_paused = true;
    }

    public void playNext() {
        musicSrv.playNext();
        if(playback_paused) {
            setController();
            playback_paused = false;
        }
        controller.show(0);
    }

    public void playPrev() {
        musicSrv.playPrev();
        if(playback_paused) {
            setController();
            playback_paused = false;
        }
        controller.show(0);
    }

    @Override
    public int getDuration() {
        if(musicSrv != null && musicBound && musicSrv.isPlaying()) {
            return musicSrv.getDur();
        }
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if(musicSrv != null && musicBound && musicSrv.isPlaying()) {
            return musicSrv.getPosn();
        }
        return 0;
    }

    @Override
    public void seekTo(int i) {
        musicSrv.seek(i);
    }

    @Override
    public boolean isPlaying() {
        if(musicSrv != null && musicBound) {
            return musicSrv.isPlaying();
        }
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }



    //lifecycle overrides
    @Override
    protected void onResume() {
        super.onResume();
        if(paused && musicSrv.isPlaying()) {
            controller.show(0);
            paused = false;
        }
        // Set up receiver for media player onPrepared broadcast
        LocalBroadcastManager.getInstance(this).registerReceiver(onPrepareReceiver,
                new IntentFilter("MEDIA_PLAYER_PREPARED"));
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

}



