package com.example.snoozemusic;

import android.app.ActionBar;
import android.app.Activity;
//import android.app.Fragment;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentManager;
//import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Codi Burley on 11/30/2014.
 */
public class MainActivity extends FragmentActivity {

    public ArrayList<Song> SongList = new ArrayList<Song>();
    public ArrayList<String> ListTitles = new ArrayList<String>();
    public SharedPreferences Prefs;
    private Intent playIntent;
    ViewPager musicPager;
    TabsAdapter tabsAdapter;
    private MusicService musicSrv;
    private MenuItem shuffle_toggle;
    private MenuItem shuffle_sleep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getSongList();
        musicPager = new ViewPager(this);
        musicPager.setId(R.id.pager);
        setContentView(musicPager);

        final ActionBar bar = getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
        tabsAdapter = new TabsAdapter(this, musicPager);
        tabsAdapter.addTab(bar.newTab().setText("Songs"),
                SongListFragment.class, null);
        tabsAdapter.addTab(bar.newTab().setText("Playlist"),
                SongListFragment.class, null);
        if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        }
        //setContentView(R.layout.song_container);
    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            //get service
            musicSrv = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @Override
    protected void onResume() {
        super.onResume();
    }

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
                    shuffle_sleep.setIcon(getResources().getDrawable(R.drawable.ic_action_sleep_off));
                } else {
                    shuffle_sleep.setIcon(getResources().getDrawable(R.drawable.ic_action_sleep_on));
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class TabsAdapter extends FragmentPagerAdapter
            implements ActionBar.TabListener, ViewPager.OnPageChangeListener {
        private final Context mContext;
        private final ActionBar mActionBar;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

        static final class TabInfo {
            private final Class<?> clss;
            private final Bundle args;

            TabInfo(Class<?> _class, Bundle _args) {
                clss = _class;
                args = _args;
            }
        }

        public TabsAdapter(FragmentActivity activity, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mActionBar = activity.getActionBar();
            mViewPager = pager;
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {
            TabInfo info = new TabInfo(clss, args);
            tab.setTag(info);
            tab.setTabListener(this);
            mTabs.add(info);
            mActionBar.addTab(tab);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            TabInfo info = mTabs.get(position);
            return Fragment.instantiate(mContext, info.clss.getName(), info.args);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            mActionBar.setSelectedNavigationItem(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            Object tag = tab.getTag();
            for (int i = 0; i < mTabs.size(); i++) {
                if (mTabs.get(i) == tag) {
                    mViewPager.setCurrentItem(i);
                }
            }
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }
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
                    SongList.add(new Song(thisId, thisTitle, thisArtist));
                }
            } while (musicCursor.moveToNext());
        }
        Collections.sort(SongList);
    }

    private void getListTitles() {

    }


}


