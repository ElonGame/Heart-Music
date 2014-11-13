package com.musicplayer.heartmusic;

import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Random;

public class PlaybackActivity extends Activity implements MediaPlayerControl{
    //controller
    private MusicController controller;
    private Intent playIntent;
    private MainActivity ListActivity = MainActivity.getInstance();

    private MusicService musicSrv;
    private boolean musicBound = true;
    private ArrayList<Song> songList;

    //activity and playback pause flags
    private boolean paused = false, playbackPaused = false;


    private static PlaybackActivity playbackActivity = new PlaybackActivity();

    public static PlaybackActivity getInstance() {
        return playbackActivity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playbackActivity = this;

        setContentView(R.layout.play);

        musicSrv = ListActivity.getMusicSrv();

        setSongInfo();

        setController();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        controller.show(0);
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
                Intent intentListActivity = new Intent(PlaybackActivity.this, MainActivity.class);
                startActivity(intentListActivity);
                break;
        }
        return super.onOptionsItemSelected(item);
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
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (musicSrv != null && musicBound && musicSrv.isPng())
            return musicSrv.getPosn();
        else return 0;
    }

    @Override
    public int getDuration() {
        if (musicSrv != null && musicBound && musicSrv.isPng())
            return musicSrv.getDur();
        else return 0;
    }

    @Override
    public boolean isPlaying() {
        if (musicSrv != null && musicBound)
            return musicSrv.isPng();
        return false;
    }

    @Override
    public void pause() {
        playbackPaused = true;
        musicSrv.pausePlayer();
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    @Override
    public void start() {
        musicSrv.go();
    }

    public void setSongInfo() {
        TextView songView = (TextView) findViewById(R.id.current_song);
        TextView artistView = (TextView) findViewById(R.id.current_artist);
        TextView heartRateView = (TextView) findViewById(R.id.heart_rate);
        TextView bpmView = (TextView) findViewById(R.id.bpm);

        songList = ListActivity.getSongList();
        //get songs from device
        Song currSong = songList.get(musicSrv.getSongPosn());

        songView.setText(currSong.getTitle());
        artistView.setText(currSong.getArtist());
        bpmView.setText(new Integer(currSong.getBpm()).toString());

        int heartRate = new Random().nextInt(songList.size()) + 60;
        musicSrv.setHeartRate(heartRate);

        if (musicSrv.isBpmMode()) {
            heartRateView.setText(new Integer(heartRate).toString());
        } else {
            heartRateView.setText("");
        }
    }

    //set the controller up
    private void setController() {
        controller = new MusicController(this);
        //set previous and next button listeners
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });
        //set and show
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.mediaController));
        controller.setEnabled(true);
    }

    private void playNext() {
        musicSrv.playNext();
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        setSongInfo();
    }

    private void playPrev() {
        musicSrv.playPrev();
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        setSongInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        controller.hide();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (paused) {
            setController();
            paused = false;
        }
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        controller.hide();

        Log.e("Test","Back Button Clicked");

        finish();
    }

}
