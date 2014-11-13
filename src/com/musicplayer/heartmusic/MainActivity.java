package com.musicplayer.heartmusic;

import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import com.musicplayer.heartmusic.MusicService.MusicBinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ListIterator;

public class MainActivity extends Activity {
    dbHelper helper;
    SQLiteDatabase db;

    //song list variables
    private ArrayList<Song> songList;
    private ListView songView;

    //service
    private MusicService musicSrv;
    private Intent playIntent;
    //binding
    private boolean musicBound = false;

    //activity and playback pause flags
    private boolean paused = false, playbackPaused = false;

    private static MainActivity listActivity = new MainActivity();

    public static MainActivity getInstance() {
        return listActivity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        listActivity = this;

        setContentView(R.layout.activity_main);

        helper = new dbHelper(this);

        try {
            db = helper.getWritableDatabase();
        } catch (SQLiteException ex) {
            db = helper.getReadableDatabase();
        }

        //retrieve list view
        songView = (ListView) findViewById(R.id.song_list);
        //instantiate list
        songList = new ArrayList<Song>();
        //get songs from device
        loadSongList();

        loadBpmInfo();

        //sort alphabetically by title
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
        //create and set adapter
        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);
    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection() {

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

    //start and bind the service when the activity starts
    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    //user song select
    public void songPicked(View view) {
        Intent intentPlaybackActivity = new Intent(MainActivity.this, PlaybackActivity.class);

        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();

        if (playbackPaused) {
            playbackPaused=false;
        }

        startActivity(intentPlaybackActivity);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //menu item selected
        switch (item.getItemId()) {
        case R.id.action_shuffle:
            musicSrv.setShuffle();
            break;
        case R.id.action_bpm:
            musicSrv.setBpmMode();
            break;
        case R.id.action_playlist:
            Intent intentPlaybackActivity = new Intent(MainActivity.this, PlaybackActivity.class);
            startActivity(intentPlaybackActivity);
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    //method to retrieve song info from device
    public void loadSongList() {
        //query external audio
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        //iterate over results if valid
        if(musicCursor!=null && musicCursor.moveToFirst()){
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            int filePathColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.DATA);

            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisFilePath = musicCursor.getString(filePathColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist, thisFilePath, 0));
            } 
            while (musicCursor.moveToNext());
        }
    }

    public void loadBpmInfo() {
        ListIterator<Song> songs = songList.listIterator();

        while (songs.hasNext()) {
            Song currSong = songs.next();
            long id = currSong.getID();
            int bpm;

            String[] selectionArgs = { new Long(id).toString() };
            Cursor bpmCursor = db.rawQuery("SELECT * FROM BPM WHERE id=?", selectionArgs);
            int bpmColumn = bpmCursor.getColumnIndex("bpm");

            if (bpmCursor.getCount() != 0) {
                bpmCursor.moveToFirst();
                bpm = bpmCursor.getInt(bpmColumn);
            } else {
                bpm = calculateBPM(currSong);

                ContentValues values = new ContentValues();

                values.put("id", id);
                values.put("title", currSong.getTitle());
                values.put("artist", currSong.getArtist());
                values.put("path", currSong.getFilePath());
                values.put("bpm", bpm);

                db.insert("BPM", null, values);
            }

            currSong.setBpm(bpm);
            songs.set(currSong);
        }
    }

    public int calculateBPM(Song song) {
        BeatDetector music = new BeatDetector();
        music.loadWavFile(song.getFilePath());
        music.calcBands();
        music.diffBands();
        return music.getBPM();
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (paused) {
            paused = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        unbindService(musicConnection);
        musicSrv = null;
        super.onDestroy();
    }

    public MusicService getMusicSrv() {
        return musicSrv;
    }
    public ArrayList<Song> getSongList() { return songList; }
}
