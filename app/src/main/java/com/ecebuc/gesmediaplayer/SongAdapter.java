package com.ecebuc.gesmediaplayer;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {

    // The dataset
    private ArrayList<Audio> songList;

    //Provide a reference to the views for each data item
    //Complex data items may need more than one view per item, and
    //you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView recyclerTitleView, recyclerArtistView;

        public ViewHolder(View itemView) {
            super(itemView);
            this.recyclerTitleView = (TextView) itemView.findViewById(R.id.recycler_item_songName);
            this.recyclerArtistView = (TextView) itemView.findViewById(R.id.recycler_item_artistName);
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
        holder.recyclerTitleView.setText(currentSong.getTitle());
        holder.recyclerArtistView.setText(currentSong.getArtist());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return songList.size();
    }

}
