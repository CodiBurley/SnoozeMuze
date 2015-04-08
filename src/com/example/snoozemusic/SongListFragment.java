package com.example.snoozemusic;

import android.widget.MediaController;

//import android.app.Fragment;
import android.support.v4.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MediaController;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Codi Burley on 11/30/2014.
 */
public class SongListFragment extends Fragment implements MediaController.MediaPlayerControl {

    private MainActivity mainAct;
    private ArrayList<Song> songList;
    private SongAdapter songAd;
    private ListView songView;
    private LinearLayout listItem;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound=false;
    private boolean paused = false, playback_paused = false;
    private MusicController controller;
    private MenuItem shuffle_toggle;
    private MenuItem shuffle_sleep;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.song_list, container, false);
    }

    @Override
    public void onStart() {
        if(playIntent == null) {
            playIntent = new Intent(getActivity(), MusicService.class);
            getActivity().bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            getActivity().startService(playIntent);
        }
        this.initVars();
        //this.getSongList();
        this.setController();
        super.onStart();
    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
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

//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//
//        // Inflate the menu; this adds items to the action bar if it is present.
//        inflater.inflate(R.menu.start, menu);
//        shuffle_toggle = menu.findItem(R.id.action_shuffle);
//        shuffle_sleep = menu.findItem(R.id.action_sleep);
//        super.onCreateOptionsMenu(menu,inflater);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        switch(item.getItemId()) {
//            case R.id.action_shuffle: //toggles shuffle and switches icons
//                if(!musicSrv.toggleShuffle()) {
//                    shuffle_toggle.setIcon(getResources().getDrawable(R.drawable.ic_action_shuffle_off));
//                } else {
//                    shuffle_toggle.setIcon(getResources().getDrawable(R.drawable.ic_action_shuffle));
//                }
//                break;
//            case R.id.action_sleep: //toggles sleep mode and switches icons
//                if(!musicSrv.toggleSleep()) {
//                    shuffle_sleep.setIcon(getResources().getDrawable(R.drawable.ic_action_sleep_off));
//                } else {
//                    shuffle_sleep.setIcon(getResources().getDrawable(R.drawable.ic_action_sleep_on));
//                }
//                break;
//            case R.id.action_end:
//                getActivity().stopService(playIntent);
//                musicSrv=null;
//                System.exit(0);
//                break;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    private void initVars() {
        mainAct = (MainActivity) getActivity();
        if(mainAct.SongList != null) {
            songList = mainAct.SongList;
        } else {
            songList = new ArrayList<Song>();
            //getSongList();
        }
        songAd = new SongAdapter(songList, songPicked, getActivity());
        songView = (ListView) getView().findViewById(R.id.song_list);
        listItem = (LinearLayout) getView().findViewById(R.id.song_item);
        songView.setAdapter(songAd);

    }

    private void getSongList() {

        ContentResolver musicResolver = getActivity().getContentResolver();
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
    private View.OnClickListener songPicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
            musicSrv.playSong();
            //setController();
            if(playback_paused) {
                playback_paused = false;
            }
            controller.setPressed(true);
            controller.show(0);
        }
    };

    public void setController() {
        if(controller == null) {
            this.controller = new MusicController(getActivity());
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
        controller.setAnchorView(getView().findViewById(R.id.song_list));
        controller.setEnabled(true);
    }

    @Override
    public void start() {
        musicSrv.go();
    }

    @Override
    public void pause() {
        musicSrv.pausePlayer();
        playback_paused = true;
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

    @Override
    public boolean isPlaying() {
        if(musicSrv != null && musicBound) {
            return musicSrv.isPlaying();
        }
        return false;
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

    // Broadcast receiver to determine when music player has been prepared
    private BroadcastReceiver onPrepareReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            // When music player has been prepared, show controller
            controller.show(0);
        }
    };

    //lifecycle overrides
    @Override
    public void onResume() {
        super.onResume();
        if(paused && musicSrv.isPlaying()) {
            controller.show(0);
            paused = false;
        }

        //TODO switch songList here

        // Set up receiver for media player onPrepared broadcast
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(onPrepareReceiver,
                                          new IntentFilter("MEDIA_PLAYER_PREPARED"));
    }

    @Override
    public void onStop() {
        controller.hide();
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    public void onDestroy() {
        getActivity().stopService(playIntent);
        musicSrv=null;
        super.onDestroy();
    }
}
