package com.example.ttins.spotifystreamer.app.Services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.example.ttins.spotifystreamer.app.PlaybackActivityFragment;
import com.example.ttins.spotifystreamer.app.utils.TrackItemList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.Image;

public class PlaybackService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, AudioManager.OnAudioFocusChangeListener {

    private final static String LOG_TAG = "PlaybackService";
    public static final String ACTION_INIT = "com.example.ttins.spotifystreamer.app.services.PlaybackService.ACTION_INIT";
    public static final String ACTION_PLAY = "com.example.ttins.spotifystreamer.app.services.PlaybackService.ACTION_PLAY";
    public static final String ACTION_PLAY_RESUME = "com.example.ttins.spotifystreamer.app.services.PlaybackService.ACTION_PLAY_RESUME";
    public static final String ACTION_PAUSE = "com.example.ttins.spotifystreamer.app.services.PlaybackService.ACTION_PAUSE";
    public static final String ACTION_SEEK = "com.example.ttins.spotifystreamer.app.services.PlaybackService.ACTION_SEEK";
    public static final String ACTION_STOP = "com.example.ttins.spotifystreamer.app.services.PlaybackService.ACTION_STOP";
    public static final String ACTION_PREV = "com.example.ttins.spotifystreamer.app.services.PlaybackService.ACTION_PREV";
    public static final String ACTION_NEXT = "com.example.ttins.spotifystreamer.app.services.PlaybackService.ACTION_NEXT";
    MediaPlayer mMediaPlayer;
    Thread mBackgroundThread;
    WifiManager.WifiLock mWifiLock;
    private final IBinder mBinder = new PlaybackBinder();
    private String mUrl;
    private String mNewUrl;
    private boolean mIsPaused;
    private int mPausePosition;
    private int mPosition;
    private List<TrackItemList> mTrackItemList = new ArrayList<TrackItemList>();
    public OnPlaybackServiceListener mCallback;
    private boolean mIsCompleted;
    private boolean mIsStarted;


    public PlaybackService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mCallback = new PlaybackActivityFragment();
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mIsCompleted=true;
                mIsStarted=false;
                mIsPaused=false;
                mCallback.onMediaCompleted();
                Log.d(LOG_TAG, "Media completed");
            }
        });

        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

        mWifiLock.acquire();
        mMediaPlayer.setOnPreparedListener(this); //Has to be called to instruct the Listener

        Log.d(LOG_TAG, "DEBUG :: PlaybackService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (null == intent || null == intent.getAction()) {
            return START_STICKY;
        }

        if (intent.getAction().equals(ACTION_INIT)) {

            mPosition = intent.getIntExtra("INTENT_TRACK_POSITION", 0);
            mTrackItemList = intent.getParcelableArrayListExtra("INTENT_TOP_TEN_LIST");

            if (!mTrackItemList.get(mPosition).getTrackPreview_url().equals(mUrl) ) {
                mUrl = mTrackItemList.get(mPosition).getTrackPreview_url();
                mMediaPlayer.stop();
                mMediaPlayer.reset();
                mIsStarted=false;
                mIsPaused=false;
                mIsCompleted=false;
                try {
                    mMediaPlayer.setDataSource(mUrl);
                }
                catch (IOException e) {
                    Log.d(LOG_TAG, "No files found on trying to streaming audio clip");
                }
            }

        } else if (intent.getAction().equals(ACTION_NEXT)) {
            mPosition = mPosition + 1;
            mPosition = (mPosition > (mTrackItemList.size() - 1)) ? 0 : mPosition;
            mUrl = mTrackItemList.get(mPosition).getTrackPreview_url();
            mMediaPlayer.stop();
            mIsStarted = false;
            mMediaPlayer.reset();
            mIsCompleted=false;
            try {
                mMediaPlayer.setDataSource(mUrl);
                mMediaPlayer.prepareAsync();
            }
            catch (IOException e) {
                Log.d(LOG_TAG, "No files found on trying to streaming audio clip");
            }

        } else if (intent.getAction().equals(ACTION_PREV)) {
            mPosition = mPosition - 1;
            mPosition = (mPosition < 0) ? (mTrackItemList.size()-1) : mPosition;
            mUrl = mTrackItemList.get(mPosition).getTrackPreview_url();
            mMediaPlayer.stop();
            mIsStarted = false;
            mMediaPlayer.reset();
            mIsCompleted=false;
            try {
                mMediaPlayer.setDataSource(mUrl);
                mMediaPlayer.prepareAsync();
            }
            catch (IOException e) {
                Log.d(LOG_TAG, "No files found on trying to streaming audio clip");
            }

        } else if (intent.getAction().equals(ACTION_PLAY)) {
                if (mIsPaused)
                    playMusicInBackground(mMediaPlayer);
                else if (mIsCompleted) {
                    mMediaPlayer.seekTo(0);
                    mMediaPlayer.start();
                    mIsCompleted=false;
                    mIsStarted = true;
                    mIsPaused = false;
                } else if (mIsStarted) {
                    //Do nothing
                }
                else
                    mMediaPlayer.prepareAsync();

        } else if (intent.getAction().equals(ACTION_PLAY_RESUME)) {
            Intent i = new Intent("DURATION_UPDATED");
            i.putExtra("duration", mMediaPlayer.getDuration());

            sendBroadcast(i);

        } else if (intent.getAction().equals(ACTION_PAUSE)) {

        }  else if (intent.getAction().equals(ACTION_STOP)) {
            if (null != mMediaPlayer && mMediaPlayer.isPlaying() && !mIsPaused) {
                Log.d(LOG_TAG, "DEBUG :: ACTION_STOP received");
                mMediaPlayer.pause();
                mIsStarted = false;
                mPausePosition = mMediaPlayer.getCurrentPosition();
                mIsPaused = true;
            }

        } else if (intent.getAction().equals(ACTION_SEEK)) {
            mMediaPlayer.seekTo(intent.getIntExtra("INTENT_SEEKTO", 0));
        } else {

        }

        /*
        if (    intent.getAction().equals(ACTION_PLAY) ||
                intent.getAction().equals(ACTION_NEXT) ||
                intent.getAction().equals(ACTION_PREV)) {


            if (mNewUrl.equals(mUrl)) {
                if (mIsPaused) {
                    mMediaPlayer.seekTo(mPausePosition);
                    playMusicInBackground(mMediaPlayer);
                    Log.d(LOG_TAG, "DEBUG :: Pause. Preparing through seekTo " + mPausePosition + " sec");
                } else if (mIsCompleted) {
                    Log.d(LOG_TAG, "DEBUG :: Completed. Preparing through seekTo " + mMediaPlayer.getCurrentPosition() + " sec");
                    mMediaPlayer.start();
                    mCallback.onMediaPlaying();
                    mIsStarted = true;
                    mIsCompleted=false;
                }

                return START_STICKY;
            } else {
                mUrl = mNewUrl;
                mMediaPlayer.stop();
                mIsStarted = false;
                mMediaPlayer.reset();
                mIsCompleted=false;
            }

            Log.d(LOG_TAG, "Preview URL: " + mUrl);

            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);

            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // could not get audio focus.
            }

            try {
                mMediaPlayer.setDataSource(mUrl);
                mMediaPlayer.prepareAsync();
            }
            catch (IOException e) {
                Log.d(LOG_TAG, "No files found on trying to streaming audio clip");
            }

        } else if (intent.getAction().equals(ACTION_STOP)) {
            if (null != mMediaPlayer && mMediaPlayer.isPlaying() && !mIsPaused) {
                Log.d(LOG_TAG, "DEBUG :: ACTION_STOP received");
                mMediaPlayer.pause();
                mIsStarted = false;
                mPausePosition = mMediaPlayer.getCurrentPosition();
                mIsPaused = true;
            }
        }
        */

        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    public void playMusicInBackground(MediaPlayer mediaPlayer) {
        mIsPaused = false;
        mediaPlayer.start();
        mCallback.onMediaPlaying();

        mCallback.onTrackPlaying(mTrackItemList.get(mPosition).getAlbumName(), mTrackItemList.get(mPosition).getAlbumImages().get(0), mTrackItemList.get(mPosition).getName(), mTrackItemList.get(mPosition).getArtists(), mMediaPlayer.getDuration());
        mIsStarted = true;
        /*mBackgroundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "Starting Media Playback");
                mMediaPlayer.start();
            }
        });*/
    }

    public int getDuration() {
        return mMediaPlayer.getDuration();
    }

    public String getCurrentAlbumName() {
        return mTrackItemList.get(mPosition).getAlbumName();
    }

    public String getAlbumImageUrl () {
        return mTrackItemList.get(mPosition).getAlbumImages().get(0);
    }

    public String getTrackName() {
        return mTrackItemList.get(mPosition).getName();
    }

    public List<String> getArtists()    {
        return mTrackItemList.get(mPosition).getArtists();
    }

    public int getPlaybackPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    public String getTrackItemList() {
        return mUrl;
    }

    public List<TrackItemList> getTopTenList() {
        return mTrackItemList;
    }

    public int getTrackListPosition() {
        return mPosition;
    }

    public boolean isMediaPlayerReady() {

        return (mMediaPlayer != null);
    }

    public boolean isMediaPlayerPaused() {
        return mIsPaused;
    }

    public boolean isLooping () {
        return mMediaPlayer.isLooping();
    }

    public boolean isMediaStarted() { return mIsStarted; }

    public void setPlaybackPosition(int timePosition) {
        Log.d(LOG_TAG, "set playback position to " + timePosition + "sec");
        mMediaPlayer.seekTo(timePosition);
        if (mIsPaused) mPausePosition = timePosition;
    }

    public void onPrepared(MediaPlayer mediaPlayer) {
        //mCallback.onTrackDataAvailable(mediaPlayer.getDuration());
        Intent i = new Intent("DURATION_UPDATED");
        i.putExtra("duration", mediaPlayer.getDuration());

        sendBroadcast(i);
        playMusicInBackground(mediaPlayer);
    }

    public boolean isMediaCompleted() {
        return mIsCompleted;
    }

    public void getPlaybackServiceIntent() {
        Intent i = new Intent("DURATION_UPDATED");
        i.putExtra("duration", mMediaPlayer.getDuration());

        sendBroadcast(i);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mMediaPlayer.stop();
        mIsStarted = false;
        mIsPaused = false;
        mMediaPlayer.release();
        mMediaPlayer=null;
        mWifiLock.release();

    }

    public void initMediaPlayer() {

        mMediaPlayer.setOnErrorListener(this);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(LOG_TAG, "MediaPlayer Error: What: " + what + " Extra: " + extra);
        return true;
    }

    public class PlaybackBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mMediaPlayer == null) initMediaPlayer();
                else if (!mMediaPlayer.isPlaying()) {
                    mMediaPlayer.start();
                    mCallback.onTrackPlaying(mTrackItemList.get(mPosition).getAlbumName(), mTrackItemList.get(mPosition).getAlbumImages().get(0), mTrackItemList.get(mPosition).getName(), mTrackItemList.get(mPosition).getArtists(), mMediaPlayer.getDuration());
                    mCallback.onMediaPlaying();
                }
                mMediaPlayer.setVolume(1.0f, 1.0f);
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mMediaPlayer.isPlaying()) mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mMediaPlayer.isPlaying()) mMediaPlayer.pause();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mMediaPlayer.isPlaying()) mMediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    public interface OnPlaybackServiceListener {
        void onMediaCompleted();
        void onMediaPlaying();
        void onTrackPlaying(String albumName, String albumImageUrl, String trackName, List<String> artistNames, int duration);
    }




}
