package com.example.ttins.spotifystreamer.app;

import android.app.Fragment;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.ttins.spotifystreamer.app.utils.TrackItemList;
import com.squareup.picasso.Picasso;

import java.io.IOException;


/**
 * A placeholder fragment containing a simple view.
 */
public class PlaybackActivityFragment extends Fragment {

    private final static String LOG_TAG = "PlaybackFragment";
    TextView mArtistTextView;
    TextView mAlbumTextView;
    TextView mSongTextView ;
    ImageView mAlbumImageView;
    SeekBar mTimeSeekBar;
    ImageButton mPrevTrackImageButton;
    ImageButton mPlayStopTrackImageButton;
    ImageButton mNextTrackImageButton;
    MediaPlayer mMediaPlayer;

    public PlaybackActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_playback, container, false);
        Bundle argsBundle = getArguments();

        if (null == argsBundle) {
            Log.d(LOG_TAG, "argsBundle is null");
        }

        TrackItemList trackItemList = argsBundle.getParcelable("ARG_TRACK");

        mArtistTextView = (TextView) rootView.findViewById(R.id.playback_artist_textview);
        mAlbumTextView = (TextView) rootView.findViewById(R.id.playback_album_textview);
        mSongTextView = (TextView) rootView.findViewById(R.id.playback_song_textview);
        mAlbumImageView = (ImageView) rootView.findViewById(R.id.playback_album_imageview);
        mTimeSeekBar = (SeekBar) rootView.findViewById(R.id.playback_time_seekbar);
        mPrevTrackImageButton = (ImageButton) rootView.findViewById(R.id.playback_prev_track_imagebutton);
        mPlayStopTrackImageButton = (ImageButton) rootView.findViewById(R.id.playback_playstop_track_imagebutton);
        mNextTrackImageButton = (ImageButton) rootView.findViewById(R.id.playback_next_track_imagebutton);

        if (trackItemList != null) {
            mAlbumTextView.setText(trackItemList.getAlbumName());
            Picasso.with(getActivity()).load(trackItemList.getAlbumImages().get(0)).into(mAlbumImageView);
            mSongTextView.setText(trackItemList.getName());

            String url = trackItemList.getTrackPreview_url();
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mMediaPlayer.setDataSource(url);
                mMediaPlayer.prepare(); // might take long! (for buffering, etc)
            }
            catch (IOException e) {
                Log.d(LOG_TAG, "No files found on trying to streaming audio clip");
            }
            mMediaPlayer.start();

        } else {
            Log.d(LOG_TAG, "TrackItemList is null");
        }

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();

        //TODO
        //Just stopping the media streaming onPause. This will have to be removed
        //once the mediaplayer will be managed by a Service.
        mMediaPlayer.stop();
        mMediaPlayer.release();
        mMediaPlayer=null;
    }
}
