package com.example.ttins.spotifystreamer.app;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ttins.spotifystreamer.app.Services.PlaybackService;
import com.example.ttins.spotifystreamer.app.utils.TrackItemList;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Image;


/**
 * A placeholder fragment containing a simple view.
 */
public class PlaybackActivityFragment extends DialogFragment implements PlaybackService.OnPlaybackServiceListener{

    private final static String LOG_TAG = "PlaybackFragment";
    public static final String FRAG_ARG_NOW_PLAYING = "ARG_NOW_PLAYING";
    public static final String ARG_TRACK = "ARG_TRACK";
    public static final String ARG_TRACK_POSITION = "ARG_TRACK_POSITION";
    public static final String ARG_TOP_TEN_LIST = "ARG_TOP_TEN_LIST";
    public static final String TRACK_ITEM_LIST = "TRACK_ITEM_LIST";
    public static final String PLAY_POSITION = "PLAY_POSITION";
    public static final String TRACK_LIST = "TRACK_LIST";
    public static final String INTENT_SEEKTO = "INTENT_SEEKTO";
    public static final String DURATION_UPDATED = "DURATION_UPDATED";


    private static final String INIT_TIME = "0:00";
    RelativeLayout mLayoutContainer;
    static TextView mArtistTextView;
    static TextView mAlbumTextView;
    static TextView mSongTextView ;
    static ImageView mAlbumImageView;
    static SeekBar mTimeSeekBar;
    TextView mStartTimeTextView;
    TextView mStopTimeTextView;
    ImageButton mPrevTrackImageButton;
    static ImageButton mPlayStopTrackImageButton;
    ImageButton mNextTrackImageButton;
    private int mDuration;
    private PlaybackService mPlaybackService;
    boolean mBound;
    private int mPosition;
    private List<TrackItemList> mTracks = new ArrayList<TrackItemList>();
    private TrackItemList mTrackItemList;
    private OnFragmentClickListener mCallback;
    private Handler durationHandler = new Handler();
    private boolean mNowPlayingRequest;

    private BroadcastReceiver mUiUpdated= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            mDuration = intent.getExtras().getInt("duration");

            String sDuration = String.format("%d:%d",
                    TimeUnit.MILLISECONDS.toMinutes(mDuration),
                    TimeUnit.MILLISECONDS.toSeconds(mDuration) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(mDuration))
            );

            mStopTimeTextView.setText(sDuration);

            try {
                mTimeSeekBar.setMax(mDuration);
                updateSeekBarAndText(mTimeSeekBar, mStartTimeTextView, mDuration, mPlaybackService.getPlaybackPosition());
                //mTimeSeekBar.setProgress(mPlaybackService.getPlaybackPosition());
            }
            catch (NullPointerException e) {
                Log.d(LOG_TAG, "SeekBar is null on broadcast Ui update");
            }

            try {
                if (mPlaybackService.isMediaCompleted() || mPlaybackService.isMediaPlayerPaused()) {
                    mPlayStopTrackImageButton.setImageResource(android.R.drawable.ic_media_play);
                }
            } catch (NullPointerException e) {
                Log.d(LOG_TAG, "mPlaybackService is null");
            }
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaybackService.PlaybackBinder binder = (PlaybackService.PlaybackBinder) service;
            mPlaybackService = binder.getService();
            mBound=true;

            /* Change SeekBar position Listener */
            onSeekBarChangeListener(mTimeSeekBar, mPlaybackService);
            /* Play/Stop button click Listener */
            onPlayStopButtonClickListener(mPlayStopTrackImageButton, mPlaybackService);

            if (mPlaybackService.isMediaPlayerReady())
                mPlaybackService.getPlaybackServiceIntent();

            if(mNowPlayingRequest) {
                mNowPlayingRequest = false;
                if (mPlaybackService.isMediaStarted() || mPlaybackService.isMediaPlayerPaused() || mPlaybackService.isMediaCompleted()) {
                    sendActionToService(getActivity(), PlaybackService.ACTION_PLAY_RESUME);

                    if (mPlaybackService.getTrackItemList() == null) {
                        mSongTextView.setText("No Song to play");
                    } else {
                        updateSeekBarAndText(mTimeSeekBar, mStartTimeTextView, mDuration, mPlaybackService.getPlaybackPosition());
                        updateTrackInfo(mPlaybackService, mAlbumTextView, mAlbumImageView, mSongTextView);
                    }
                } else {
                    mCallback.onFragmentDismiss();
                    durationHandler.removeCallbacks(updateSeekBarTime);
                    dismiss();
                }
            } else {
                updateSeekBarAndText(mTimeSeekBar, mStartTimeTextView, mDuration, mPlaybackService.getPlaybackPosition());
                updateTrackInfo(mPlaybackService, mAlbumTextView, mAlbumImageView, mSongTextView);
            }

            Log.d(LOG_TAG, "Bound to Playback service");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound=false;
            Log.d(LOG_TAG, "Unbound to Playback service");
        }
    };


    public PlaybackActivityFragment() {
    }

    private void updateTrackInfo (PlaybackService playbackService, TextView albumTextView, ImageView albumImageView, TextView songTextView) {

        albumTextView.setText(playbackService.getCurrentAlbumName());
        Picasso.with(getActivity()).load(playbackService.getAlbumImageUrl()).into(albumImageView);
        songTextView.setText(playbackService.getTrackName());
    }

    @Override
    public void onStart() {
        super.onStart();

        if (null == mPlaybackService) {
            Intent intent = new Intent(getActivity(), PlaybackService.class);
            if (!getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
                Log.d(LOG_TAG, "Bind to Service failed");
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setRetainInstance(true);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_playback_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(LOG_TAG, "onCreateView()");

        List<String> artists = new ArrayList<String>();
        View rootView = inflater.inflate(R.layout.fragment_playback, container, false);
        Bundle argsBundle = getArguments();

        if (null == argsBundle) {
            Log.d(LOG_TAG, "argsBundle is null!");
            if (null == savedInstanceState) {
                dismiss();
                return null;
            }
        } else if (argsBundle.getBoolean("ARG_NOW_PLAYING")) {
            mNowPlayingRequest = true;
        }

        if(getDialog() != null) {
            Dialog dialog = getDialog();
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        ImageButton imageButtonShare = (ImageButton) rootView.findViewById(R.id.share_imagebutton);

        /* Handling Click on Share Button */
        onShareButtonClickHandler(imageButtonShare);

        if(null == savedInstanceState) {
            mTrackItemList = argsBundle.getParcelable(ARG_TRACK);
            mPosition = argsBundle.getInt(ARG_TRACK_POSITION);
            mTracks = argsBundle.getParcelableArrayList(ARG_TOP_TEN_LIST);
        } else {
            mTrackItemList = savedInstanceState.getParcelable(TRACK_ITEM_LIST);
            mPosition = savedInstanceState.getInt(PLAY_POSITION);
            mTracks = savedInstanceState.getParcelableArrayList(TRACK_LIST);
        }

        mLayoutContainer = (RelativeLayout) rootView.findViewById(R.id.dialog_fragment_container);
        mArtistTextView = (TextView) rootView.findViewById(R.id.playback_artist_textview);
        mAlbumTextView = (TextView) rootView.findViewById(R.id.playback_album_textview);
        mSongTextView = (TextView) rootView.findViewById(R.id.playback_song_textview);
        mAlbumImageView = (ImageView) rootView.findViewById(R.id.playback_album_imageview);
        mTimeSeekBar = (SeekBar) rootView.findViewById(R.id.playback_time_seekbar);
        mStartTimeTextView = (TextView) rootView.findViewById(R.id.start_time_textview);
        mStopTimeTextView = (TextView) rootView.findViewById(R.id.stop_time_textview);
        mPrevTrackImageButton = (ImageButton) rootView.findViewById(R.id.playback_prev_track_imagebutton);
        mPlayStopTrackImageButton = (ImageButton) rootView.findViewById(R.id.playback_playstop_track_imagebutton);
        mNextTrackImageButton = (ImageButton) rootView.findViewById(R.id.playback_next_track_imagebutton);


        if (null != mTrackItemList) {
            artists = mTrackItemList.getArtists();
            String artistsStringList = "";

            for (String artistName: artists) {
                artistsStringList = artistsStringList + artistName + "";
            }

            mArtistTextView.setText(artistsStringList);

            if (mPlaybackService != null) {
                updateSeekBarAndText(mTimeSeekBar, mStartTimeTextView, mDuration, mPlaybackService.getPlaybackPosition());
                updateTrackInfo(mPlaybackService, mAlbumTextView, mAlbumImageView, mSongTextView);
            } else {
                mStartTimeTextView.setText(INIT_TIME);
                mStopTimeTextView.setText(INIT_TIME);
                Log.d(LOG_TAG, "mPlaybackService is null");
            }
        } else {
            Log.d(LOG_TAG, "mTrackItemList is null");
        }

        /* Change SeekBar position Listener */
        onSeekBarChangeListener(mTimeSeekBar, mPlaybackService);
        /* Play/Stop button click Listener */
        onPlayStopButtonClickListener(mPlayStopTrackImageButton, mPlaybackService);
        /* Prev button click Listener */
        onClickImageButtonToIntent(mPrevTrackImageButton, PlaybackService.ACTION_PREV);
        /* Next button click Listener */
        onClickImageButtonToIntent(mNextTrackImageButton, PlaybackService.ACTION_NEXT);

        return rootView;
    }

    private void sendActionToService(Context context, String action) {
        Intent serviceIntent = new Intent(action);
        serviceIntent.setClass(context, PlaybackService.class);
        getActivity().startService(serviceIntent);
    }

    private void onClickImageButtonToIntent(ImageButton imageButton, String action) {
        final String localAction = action;

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendActionToService(getActivity(), localAction);
            }
        });
    }

    private void onPlayStopButtonClickListener(ImageButton playStopImageButton, PlaybackService playbackService) {
        final PlaybackService localPlaybackService = playbackService;
        final ImageButton localImageButton = playStopImageButton;

        if (null == localPlaybackService)
            return;

        localImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String stringAction;

                if (localPlaybackService.isMediaStarted()) {
                    stringAction = PlaybackService.ACTION_STOP;
                    Log.d(LOG_TAG, "ACTION_STOP received");
                    localImageButton.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    stringAction = PlaybackService.ACTION_PLAY;
                    Log.d(LOG_TAG, "ACTION_PLAY received");
                    localImageButton.setImageResource(android.R.drawable.ic_media_pause);
                }

                sendActionToService(getActivity(), stringAction);
            }
        });
    }

    private void onSeekBarChangeListener(SeekBar seekBar, PlaybackService playbackService) {
        final PlaybackService localPlaybackService = playbackService;

        if (null == localPlaybackService)
            return;

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                try {
                    if (localPlaybackService.isMediaPlayerReady() && fromUser) {
                        localPlaybackService.setPlaybackPosition(progress);

                        if(localPlaybackService.isMediaCompleted()) {

                            Intent seekToIntent = new Intent(PlaybackService.ACTION_SEEK);
                            seekToIntent.putExtra(INTENT_SEEKTO, progress);
                            Intent explicitIntent = createExplicitFromImplicitIntent(getActivity(), seekToIntent);
                            getActivity().startService(explicitIntent);
                        }
                    }
                } catch (NullPointerException e) {
                    Log.d(LOG_TAG, "PlaybackService Null");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void updateSeekBarAndText(SeekBar seekBar, TextView startTextView, int duration, int currentPosition) {
        if (seekBar != null) {
            seekBar.setMax(duration);
            seekBar.setProgress(currentPosition);
        } else {
            Log.d(LOG_TAG, "SeekBar is null. Can't update it.");
        }

        if (startTextView != null) {
            String sCurrent = String.format("%d:%d",
                    TimeUnit.MILLISECONDS.toMinutes(currentPosition),
                    TimeUnit.MILLISECONDS.toSeconds(currentPosition) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(currentPosition))
            );

            startTextView.setText(sCurrent);

        } else {
            Log.d(LOG_TAG, "TextView for current Position is null. Can't update it.");
        }
    }

    private void onShareButtonClickHandler(ImageButton imageButton) {

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlaybackService.getPreviewUrl() != null && mPlaybackService.getPreviewUrl().length() != 0) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, mPlaybackService.getPreviewUrl());
                    sendIntent.setType("text/plain");
                    startActivity(Intent.createChooser(sendIntent, "SendTo"));
                } else {
                    Toast.makeText(getActivity(), "No Url found", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public static Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }

        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putParcelable(TRACK_ITEM_LIST, mTrackItemList);
        savedInstanceState.putInt(PLAY_POSITION, mPosition);
        savedInstanceState.putParcelableArrayList(TRACK_LIST, (ArrayList<TrackItemList>) mTracks);
    }

    @Override
    public void onResume() {
        getActivity().registerReceiver(mUiUpdated, new IntentFilter(DURATION_UPDATED));

        super.onResume();

        if (getDialog() != null) {
            int height = (int) getResources().getDimension(R.dimen.fragment_container_height);
            int width = (int) getResources().getDimension(R.dimen.fragment_container_width);
            mLayoutContainer.getLayoutParams().height = height;
            mLayoutContainer.getLayoutParams().width = width;

            height = (int) getResources().getDimension(R.dimen.dialog_container_height);
            width = (int) getResources().getDimension(R.dimen.dialog_container_width);
            getDialog().getWindow().setLayout(width, height);
        }

        if(mPlaybackService != null)
            updateSeekBarAndText(mTimeSeekBar, mStartTimeTextView, mDuration, mPlaybackService.getPlaybackPosition());

        durationHandler.postDelayed(updateSeekBarTime, 100);

    }

    private Runnable updateSeekBarTime = new Runnable() {
        @Override
        public void run() {

            try {
                if (null != mPlaybackService ) {
                    if (!mPlaybackService.isMediaCompleted() && !mPlaybackService.isMediaPlayerPaused()) {
                        updateSeekBarAndText(mTimeSeekBar, mStartTimeTextView, mDuration, mPlaybackService.getPlaybackPosition());
                    }
                }
            } catch (NullPointerException e) {
                Log.d(LOG_TAG, "mediaPlayer not ready (null)");
            }

            durationHandler.postDelayed(this, 100);
        }
    };

    @Override
    public void onPause() {
        try {
            if (mUiUpdated != null)
                getActivity().unregisterReceiver(mUiUpdated);
            Log.d(LOG_TAG, "onPause(): unregister Receiver");
        }
        catch (IllegalArgumentException e) {
            Log.d(LOG_TAG, "Receiver not registered to Broadcast event");
        }

        durationHandler.removeCallbacks(updateSeekBarTime);
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallback = (OnFragmentClickListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentClickListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public interface OnFragmentClickListener {
        void onFragmentDismiss();
    }

    @Override
    public void onMediaCompleted() {
        if (mPlayStopTrackImageButton != null) {
            mPlayStopTrackImageButton.setImageResource(android.R.drawable.ic_media_play);
            Log.d(LOG_TAG, "onMediaCompleted called");
        } else {
            Log.d(LOG_TAG, "onMediaCompleted found null button");
        }
    }

    @Override
    public void onMediaPlaying() {
        if (mPlayStopTrackImageButton != null) {
            mPlayStopTrackImageButton.setImageResource(android.R.drawable.ic_media_pause);
            Log.d(LOG_TAG, "onMediaPlaying called");
        } else {
            Log.d(LOG_TAG, "onMediaPlaying found null button");
        }
    }

    @Override
    public void onMediaPause() {
        if (mPlayStopTrackImageButton != null) {
            mPlayStopTrackImageButton.setImageResource(android.R.drawable.ic_media_play);
            Log.d(LOG_TAG, "onMediaPause called");
        } else {
            Log.d(LOG_TAG, "onMediaPause found null button");
        }
    }

    @Override
    public void onTrackPlaying(String albumName, String albumImageUrl, String trackName, List<String> artistNames, int duration) {
        try {
            Log.d(LOG_TAG, "onTrackPlaying received");
            mAlbumTextView.setText(albumName);
            Picasso.with(getActivity()).load(albumImageUrl).into(mAlbumImageView);
            mSongTextView.setText(trackName);
            mTimeSeekBar.setMax(duration);
        } catch (NullPointerException e) {
            Log.d(LOG_TAG, "onTrackPlaying() failed cause of NullPointerException");
        }

    }

}
