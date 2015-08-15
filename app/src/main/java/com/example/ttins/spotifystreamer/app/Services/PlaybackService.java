package com.example.ttins.spotifystreamer.app.Services;

import android.app.Notification;
import android.app.Notification.Action;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.example.ttins.spotifystreamer.app.PlaybackActivityFragment;
import com.example.ttins.spotifystreamer.app.R;
import com.example.ttins.spotifystreamer.app.utils.TrackItemList;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
    private static final String ACTION_ENABLE_NOTIFY = "com.example.ttins.spotifystreamer.app.services.PlaybackService.ACTION_ENABLE_NOTIFY";
    private static final String ACTION_DISABLE_NOTIFY = "com.example.ttins.spotifystreamer.app.services.PlaybackService.ACTION_DISABLE_NOTIFY";

    MediaPlayer mMediaPlayer;
    Thread mBackgroundThread;
    WifiManager.WifiLock mWifiLock;
    private final IBinder mBinder = new PlaybackBinder();
    private static String mUrl;
    private String mNewUrl;
    private boolean mIsPaused;
    private int mPausePosition;
    private int mPosition;
    private List<TrackItemList> mTrackItemList = new ArrayList<TrackItemList>();
    public OnPlaybackServiceListener mCallback;
    private boolean mIsCompleted;
    private boolean mIsStarted;
    int mNotificationId = 001;
    NotificationCompat.Builder mBuilder;
    NotificationManager mNotificationManager;
    RemoteViews mRemoteView;
    MediaSessionManager mManager;
    MediaSession mSession;
    MediaController mController;
    private Boolean mEnableNotify;


    public PlaybackService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mEnableNotify = sharedPref.getBoolean(getString(R.string.pref_checkbox_notification_key), false);

        mCallback = new PlaybackActivityFragment();
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mIsCompleted = true;
                mIsStarted = false;
                mIsPaused = false;
                mCallback.onMediaCompleted();
                Log.d(LOG_TAG, "Media completed");

                if(mEnableNotify) {
                    if (mBuilder == null) {
                        mRemoteView = new RemoteViews(getPackageName(), R.layout.notification_layout);
                        mBuilder = new NotificationCompat.Builder(getApplicationContext());
                        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    }

                    mBuilder.setContentTitle(mTrackItemList.get(mPosition).getName())
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setContentText(mTrackItemList.get(mPosition).getAlbumName())
                            .setSmallIcon(R.drawable.spotify_icon)
                            .setPriority(NotificationCompat.PRIORITY_HIGH);

                    mRemoteView.setTextViewText(R.id.track_title_notification_textview, mTrackItemList.get(mPosition).getName());
                    mRemoteView.setTextViewText(R.id.album_title_notification_textview, mTrackItemList.get(mPosition).getAlbumName());

                    Intent intentPrev = new Intent(getApplicationContext(), PlaybackService.class);
                    intentPrev.setAction(ACTION_PREV);
                    Intent intentPlayStop = new Intent(getApplicationContext(), PlaybackService.class);
                    intentPlayStop.setAction(ACTION_PLAY);
                    Intent intentNext = new Intent(getApplicationContext(), PlaybackService.class);
                    intentNext.setAction(ACTION_NEXT);

                    PendingIntent pendingPrevIntent = PendingIntent.getService(getApplicationContext(), 1, intentPrev, 0);
                    PendingIntent pendingPlayStopIntent = PendingIntent.getService(getApplicationContext(), 1, intentPlayStop, 0);
                    PendingIntent pendingNextIntent = PendingIntent.getService(getApplicationContext(), 1, intentNext, 0);

                    mRemoteView.setOnClickPendingIntent(R.id.prev_button_notification, pendingPrevIntent);
                    mRemoteView.setImageViewResource(R.id.playstop_button_notification, android.R.drawable.ic_media_play);
                    mRemoteView.setOnClickPendingIntent(R.id.playstop_button_notification, pendingPlayStopIntent);
                    mRemoteView.setOnClickPendingIntent(R.id.next_button_notification, pendingNextIntent);
                    try {
                        Void imageBmp = new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... params) {
                                mBuilder.setContent(mRemoteView);



                                try {
                                    mRemoteView.setImageViewBitmap(R.id.notification_image, Picasso.with(getApplicationContext()).load(mTrackItemList.get(mPosition).getImage()).get());
                                    mNotificationManager.notify(mNotificationId, mBuilder.build());



                                }
                                catch (IOException e) {
                                    Log.d(LOG_TAG, "Failed to load image into notification");
                                }

                                return null;
                            }

                        }.execute().get();
                    }
                    catch (InterruptedException e) {
                        Log.d(LOG_TAG, "Interrupted Exception on image notification load");
                    } catch (ExecutionException e) {
                        Log.d(LOG_TAG, "Execution Exception on image notification load");
                    }
                }


            }
        });

        if(mEnableNotify) {
            mRemoteView = new RemoteViews(getPackageName(), R.layout.notification_layout);
            mBuilder = new NotificationCompat.Builder(this);
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        }

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

        if (intent.getAction().equals(ACTION_ENABLE_NOTIFY)) {
            mEnableNotify=true;
            mRemoteView = new RemoteViews(getPackageName(), R.layout.notification_layout);
            mBuilder = new NotificationCompat.Builder(this);
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        } else if (intent.getAction().equals(ACTION_DISABLE_NOTIFY)) {
            mEnableNotify=false;
            if (mNotificationManager != null)
                mNotificationManager.cancel(mNotificationId);

            mRemoteView = null;
            mBuilder = null;
            mNotificationManager = null;
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

            if(mEnableNotify) {
                mBuilder.setContentTitle(mTrackItemList.get(mPosition).getName())
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setContentText(mTrackItemList.get(mPosition).getAlbumName())
                        .setSmallIcon(R.drawable.spotify_icon)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

                mRemoteView.setTextViewText(R.id.track_title_notification_textview, mTrackItemList.get(mPosition).getName());
                mRemoteView.setTextViewText(R.id.album_title_notification_textview, mTrackItemList.get(mPosition).getAlbumName());

                Intent intentPrev = new Intent(getApplicationContext(), PlaybackService.class);
                intentPrev.setAction(ACTION_PREV);
                Intent intentPlayStop = new Intent(getApplicationContext(), PlaybackService.class);
                intentPlayStop.setAction(ACTION_STOP);
                Intent intentNext = new Intent(getApplicationContext(), PlaybackService.class);
                intentNext.setAction(ACTION_NEXT);

                PendingIntent pendingPrevIntent = PendingIntent.getService(getApplicationContext(), 1, intentPrev, 0);
                PendingIntent pendingPlayStopIntent = PendingIntent.getService(getApplicationContext(), 1, intentPlayStop, 0);
                PendingIntent pendingNextIntent = PendingIntent.getService(getApplicationContext(), 1, intentNext, 0);

                mRemoteView.setOnClickPendingIntent(R.id.prev_button_notification, pendingPrevIntent);
                mRemoteView.setImageViewResource(R.id.playstop_button_notification, android.R.drawable.ic_media_pause);
                mRemoteView.setOnClickPendingIntent(R.id.playstop_button_notification, pendingPlayStopIntent);
                mRemoteView.setOnClickPendingIntent(R.id.next_button_notification, pendingNextIntent);
                try {
                    Void imageBmp = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            mBuilder.setContent(mRemoteView);



                            try {
                                mRemoteView.setImageViewBitmap(R.id.notification_image, Picasso.with(getApplicationContext()).load(mTrackItemList.get(mPosition).getImage()).get());
                                mNotificationManager.notify(mNotificationId, mBuilder.build());



                            }
                            catch (IOException e) {
                                Log.d(LOG_TAG, "Failed to load image into notification");
                            }

                            return null;
                        }

                    }.execute().get();
                }
                catch (InterruptedException e) {
                    Log.d(LOG_TAG, "Interrupted Exception on image notification load");
                } catch (ExecutionException e) {
                    Log.d(LOG_TAG, "Execution Exception on image notification load");
                }

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

            if(mEnableNotify) {
                mBuilder.setContentTitle(mTrackItemList.get(mPosition).getName())
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setContentText(mTrackItemList.get(mPosition).getAlbumName())
                        .setSmallIcon(R.drawable.spotify_icon)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

                mRemoteView.setTextViewText(R.id.track_title_notification_textview, mTrackItemList.get(mPosition).getName());
                mRemoteView.setTextViewText(R.id.album_title_notification_textview, mTrackItemList.get(mPosition).getAlbumName());

                Intent intentPrev = new Intent(getApplicationContext(), PlaybackService.class);
                intentPrev.setAction(ACTION_PREV);
                Intent intentPlayStop = new Intent(getApplicationContext(), PlaybackService.class);
                intentPlayStop.setAction(ACTION_STOP);
                Intent intentNext = new Intent(getApplicationContext(), PlaybackService.class);
                intentNext.setAction(ACTION_NEXT);

                PendingIntent pendingPrevIntent = PendingIntent.getService(getApplicationContext(), 1, intentPrev, 0);
                PendingIntent pendingPlayStopIntent = PendingIntent.getService(getApplicationContext(), 1, intentPlayStop, 0);
                PendingIntent pendingNextIntent = PendingIntent.getService(getApplicationContext(), 1, intentNext, 0);

                mRemoteView.setOnClickPendingIntent(R.id.prev_button_notification, pendingPrevIntent);
                mRemoteView.setImageViewResource(R.id.playstop_button_notification, android.R.drawable.ic_media_pause);
                mRemoteView.setOnClickPendingIntent(R.id.playstop_button_notification, pendingPlayStopIntent);
                mRemoteView.setOnClickPendingIntent(R.id.next_button_notification, pendingNextIntent);


                try {
                    Void imageBmp = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            mBuilder.setContent(mRemoteView);



                            try {
                                mRemoteView.setImageViewBitmap(R.id.notification_image, Picasso.with(getApplicationContext()).load(mTrackItemList.get(mPosition).getImage()).get());
                                mNotificationManager.notify(mNotificationId, mBuilder.build());



                            }
                            catch (IOException e) {
                                Log.d(LOG_TAG, "Failed to load image into notification");
                            }

                            return null;
                        }

                    }.execute().get();
                }
                catch (InterruptedException e) {
                    Log.d(LOG_TAG, "Interrupted Exception on image notification load");
                } catch (ExecutionException e) {
                    Log.d(LOG_TAG, "Execution Exception on image notification load");
                }
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

                if(mEnableNotify) {
                    mBuilder.setContentTitle(mTrackItemList.get(mPosition).getName())
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setContentText(mTrackItemList.get(mPosition).getAlbumName())
                            .setSmallIcon(R.drawable.spotify_icon)
                            .setPriority(NotificationCompat.PRIORITY_HIGH);

                    mRemoteView.setTextViewText(R.id.track_title_notification_textview, mTrackItemList.get(mPosition).getName());
                    mRemoteView.setTextViewText(R.id.album_title_notification_textview, mTrackItemList.get(mPosition).getAlbumName());

                    Intent intentPrev = new Intent(getApplicationContext(), PlaybackService.class);
                    intentPrev.setAction(ACTION_PREV);
                    Intent intentPlayStop = new Intent(getApplicationContext(), PlaybackService.class);
                    intentPlayStop.setAction(ACTION_STOP);
                    Intent intentNext = new Intent(getApplicationContext(), PlaybackService.class);
                    intentNext.setAction(ACTION_NEXT);

                    PendingIntent pendingPrevIntent = PendingIntent.getService(getApplicationContext(), 1, intentPrev, 0);
                    PendingIntent pendingPlayStopIntent = PendingIntent.getService(getApplicationContext(), 1, intentPlayStop, 0);
                    PendingIntent pendingNextIntent = PendingIntent.getService(getApplicationContext(), 1, intentNext, 0);

                    mRemoteView.setOnClickPendingIntent(R.id.prev_button_notification, pendingPrevIntent);
                    mRemoteView.setImageViewResource(R.id.playstop_button_notification, android.R.drawable.ic_media_pause);
                    mRemoteView.setOnClickPendingIntent(R.id.playstop_button_notification, pendingPlayStopIntent);
                    mRemoteView.setOnClickPendingIntent(R.id.next_button_notification, pendingNextIntent);


                    try {
                        Void imageBmp = new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... params) {
                                mBuilder.setContent(mRemoteView);



                                try {
                                    mRemoteView.setImageViewBitmap(R.id.notification_image, Picasso.with(getApplicationContext()).load(mTrackItemList.get(mPosition).getImage()).get());
                                    mNotificationManager.notify(mNotificationId, mBuilder.build());



                                }
                                catch (IOException e) {
                                    Log.d(LOG_TAG, "Failed to load image into notification");
                                }

                                return null;
                            }

                        }.execute().get();
                    }
                    catch (InterruptedException e) {
                        Log.d(LOG_TAG, "Interrupted Exception on image notification load");
                    } catch (ExecutionException e) {
                        Log.d(LOG_TAG, "Execution Exception on image notification load");
                    }

                }


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

                if(mEnableNotify) {
                    mBuilder.setContentTitle(mTrackItemList.get(mPosition).getName())
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setContentText(mTrackItemList.get(mPosition).getAlbumName())
                            .setSmallIcon(R.drawable.spotify_icon)
                            .setPriority(NotificationCompat.PRIORITY_HIGH);

                    mRemoteView.setTextViewText(R.id.track_title_notification_textview, mTrackItemList.get(mPosition).getName());
                    mRemoteView.setTextViewText(R.id.album_title_notification_textview, mTrackItemList.get(mPosition).getAlbumName());

                    Intent intentPrev = new Intent(getApplicationContext(), PlaybackService.class);
                    intentPrev.setAction(ACTION_PREV);
                    Intent intentPlayStop = new Intent(getApplicationContext(), PlaybackService.class);
                    intentPlayStop.setAction(ACTION_PLAY);
                    Intent intentNext = new Intent(getApplicationContext(), PlaybackService.class);
                    intentNext.setAction(ACTION_NEXT);

                    PendingIntent pendingPrevIntent = PendingIntent.getService(getApplicationContext(), 1, intentPrev, 0);
                    PendingIntent pendingPlayStopIntent = PendingIntent.getService(getApplicationContext(), 1, intentPlayStop, 0);
                    PendingIntent pendingNextIntent = PendingIntent.getService(getApplicationContext(), 1, intentNext, 0);

                    mRemoteView.setOnClickPendingIntent(R.id.prev_button_notification, pendingPrevIntent);
                    mRemoteView.setImageViewResource(R.id.playstop_button_notification, android.R.drawable.ic_media_play);
                    mRemoteView.setOnClickPendingIntent(R.id.playstop_button_notification, pendingPlayStopIntent);
                    mRemoteView.setOnClickPendingIntent(R.id.next_button_notification, pendingNextIntent);


                    try {
                        Void imageBmp = new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... params) {
                                mBuilder.setContent(mRemoteView);



                                try {
                                    mRemoteView.setImageViewBitmap(R.id.notification_image, Picasso.with(getApplicationContext()).load(mTrackItemList.get(mPosition).getImage()).get());
                                    mNotificationManager.notify(mNotificationId, mBuilder.build());



                                }
                                catch (IOException e) {
                                    Log.d(LOG_TAG, "Failed to load image into notification");
                                }

                                return null;
                            }

                        }.execute().get();
                    }
                    catch (InterruptedException e) {
                        Log.d(LOG_TAG, "Interrupted Exception on image notification load");
                    } catch (ExecutionException e) {
                        Log.d(LOG_TAG, "Execution Exception on image notification load");
                    }
                }

            }

        } else if (intent.getAction().equals(ACTION_SEEK)) {
            mMediaPlayer.seekTo(intent.getIntExtra("INTENT_SEEKTO", 0));
        } else {

        }

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

    public String getPreviewUrl() {
        try {
            if (mUrl != null)
                return mUrl;
        } catch (NullPointerException e) {
            return null;
        } catch (IndexOutOfBoundsException e) {
            return null;
        }

        return null;
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
