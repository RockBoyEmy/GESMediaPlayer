package com.ecebuc.gesmediaplayer.AudioUtils;

import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ecebuc.gesmediaplayer.Audios.Audio;
import com.ecebuc.gesmediaplayer.R;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {

    // The dataset
    private ArrayList<Audio> songList;

    //Provide a reference to the views for each data item
    //Complex data items may need more than one view per item, and
    //you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView songRecyclerCoverView;
        public TextView songRecyclerTitleView, songRecyclerArtistView;

        public ViewHolder(View itemView) {
            super(itemView);
            this.songRecyclerCoverView = (ImageView) itemView.findViewById(R.id.song_item_albumCover);
            this.songRecyclerTitleView = (TextView) itemView.findViewById(R.id.song_item_songName);
            this.songRecyclerArtistView = (TextView) itemView.findViewById(R.id.song_item_artistName);
        }
    }

    // Constructor
    public SongAdapter(ArrayList<Audio> songList){
        this.songList = songList;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public SongAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItemView = layoutInflater.inflate(R.layout.song_list_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(listItemView);
        return viewHolder;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Audio currentSong = songList.get(position);
        String cover = currentSong.getAlbumArt();
        holder.songRecyclerTitleView.setText(currentSong.getTitle());
        holder.songRecyclerArtistView.setText(currentSong.getArtist());
        if(cover != null){
            Picasso.get().load(new File(cover)).resize(38, 38).centerCrop().into(holder.songRecyclerCoverView);
            //holder.songRecyclerCoverView.setImageBitmap(BitmapFactory.decodeFile(cover));
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return songList.size();
    }

}
