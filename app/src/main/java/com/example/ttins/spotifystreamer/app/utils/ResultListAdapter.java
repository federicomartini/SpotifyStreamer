package com.example.ttins.spotifystreamer.app.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatRatingBar;
import android.widget.ArrayAdapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.example.ttins.spotifystreamer.app.R;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;


public class ResultListAdapter
        extends ArrayAdapter<MyArtist> {

    private int mResource;
    private Context mContext;
    ArtistHolder holder = null;

    private final static int STARS_STEP = 20;
    private final static int MAX_POPULARITY = 100;

    public ResultListAdapter(Context context, int resource, List<MyArtist> objects) {
        super(context, resource, objects);
        mResource = resource;
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View itemView = convertView;

        if (null == itemView) {
            LayoutInflater inflater = ((Activity)mContext).getLayoutInflater();
            itemView = inflater.inflate(mResource, parent, false);

            holder = new ArtistHolder();
            holder.textArtist=(TextView)itemView.findViewById(R.id.list_item_artist_textview);
            holder.textFollowers=(TextView)itemView.findViewById(R.id.list_item_artist_followers_textview);
            //holder.textPopularity=(TextView)itemView.findViewById(R.id.list_item_artist_popularity_textview);
            holder.imageArtist=(ImageView)itemView.findViewById(R.id.list_item_artist_imageView);
            holder.ratingBar=(RatingBar)itemView.findViewById(R.id.rating_bar);

            itemView.setTag(holder);
        }
        else {

            holder = (ArtistHolder)itemView.getTag();
        }

        MyArtist artist = getItem(position);
        holder.textArtist.setText(artist.name);
        holder.textFollowers.setText(String.valueOf(artist.followers));
        holder.ratingBar.setRating(getPopularityRating((float) artist.popularity, holder.ratingBar.getNumStars()));

        if (null != artist.image) {

            Picasso.with(mContext).load(artist.image).into(holder);
        }
        else {

            Picasso.with(mContext).load(R.drawable.unknown_artist).into(holder);

        }



        return itemView;

    }

    private float getPopularityRating(float rate, int numStars) {

        float rating= (float) 0.0;
        int step = STARS_STEP;

        if (rate == MAX_POPULARITY)
            return ((float) numStars);

        for (int i=0;i<numStars;i++) {

            if(rate < (step *(i+1))) {
                rating=i;
                break;
            }

        }

        return rating;
    }


    static class ArtistHolder implements Target{

        ImageView imageArtist;
        TextView textArtist;
        TextView textFollowers;
        RatingBar ratingBar;
        RoundImage mRoundImage;

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            mRoundImage = new RoundImage(bitmap);
            imageArtist.setImageDrawable(mRoundImage);

        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }

    }


}
