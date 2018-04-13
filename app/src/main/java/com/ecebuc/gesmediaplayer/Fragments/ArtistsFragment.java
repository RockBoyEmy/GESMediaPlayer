package com.ecebuc.gesmediaplayer.Fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ecebuc.gesmediaplayer.AudioUtils.ArtistAdapter;
import com.ecebuc.gesmediaplayer.Audios.Artist;
import com.ecebuc.gesmediaplayer.R;
import com.ecebuc.gesmediaplayer.Utils.RecyclerTouchListener;
import com.ecebuc.gesmediaplayer.Utils.SimpleDividerItemDecoration;

import java.util.ArrayList;

public class ArtistsFragment extends Fragment {

    private OnArtistFragmentInteractionListener artistFragmentCallback;

    private RecyclerView artistRecyclerView;
    private RecyclerView.Adapter artistRecyclerAdapter;
    private RecyclerView.LayoutManager recyclerLayoutManager;
    private ArrayList<Artist> artistList = new ArrayList<>();

    public ArtistsFragment() {
        // Required empty public constructor
    }
    public static ArtistsFragment newInstance(String param1, String param2) {
        ArtistsFragment fragment = new ArtistsFragment();
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnArtistFragmentInteractionListener) {
            artistFragmentCallback = (OnArtistFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnSongFragmentInteractionListener");
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //populating the list with the artists on device
        artistList = initArtistLoad();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_artists, container, false);

        artistRecyclerView = (RecyclerView) rootView.findViewById(R.id.artists_recycler_view);
        artistRecyclerView.setHasFixedSize(true);
        artistRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(getContext()));

        recyclerLayoutManager = new LinearLayoutManager(getActivity());
        artistRecyclerView.setLayoutManager(recyclerLayoutManager);

        artistRecyclerAdapter = new ArtistAdapter(artistList);
        artistRecyclerView.setAdapter(artistRecyclerAdapter);
        artistRecyclerView.setItemAnimator(new DefaultItemAnimator());
        artistRecyclerView.addOnItemTouchListener(
                new RecyclerTouchListener(getContext(), artistRecyclerView, new RecyclerTouchListener.ClickListener() {
                    @Override
                    public void onClick(View view, int position) {
                        Artist selectedArtist = artistList.get(position);
                        String artistId = selectedArtist.getArtistId();
                        artistFragmentCallback.startAlbumsFragmentFromId(artistId);
                        //Toast.makeText(getContext(), selectedArtist.getArtistName() + " is selected!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onLongClick(View view, int position) {
                        Toast.makeText(getContext(), "Long selected", Toast.LENGTH_SHORT).show();
                    }
                }));

        return rootView;
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        artistFragmentCallback.updateToolbarTitleForFragment("Artists");
    }
    @Override
    public void onDetach() {
        super.onDetach();
        artistFragmentCallback = null;
    }

    public interface OnArtistFragmentInteractionListener {
        void updateToolbarTitleForFragment(String fragmentTitle);
        void startAlbumsFragmentFromId(String artistId);
    }

    private ArrayList<Artist> initArtistLoad() {
        ContentResolver contentResolver = getActivity().getContentResolver();
        String artistId, artistName, artistAlbums, artistTracks;
        ArrayList<Artist> artistList = new ArrayList<>();
        final String[] ARTIST_SUMMARY_PROJECTION = {
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
                MediaStore.Audio.Artists.NUMBER_OF_TRACKS };

        Uri artistUri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
        String sortOrder = MediaStore.Audio.Artists.ARTIST + " ASC";
        Cursor cursor = contentResolver.query(
                artistUri,
                ARTIST_SUMMARY_PROJECTION,
                null, null,
                sortOrder);

        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                artistId = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists._ID));
                artistName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST));
                artistAlbums = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS));
                artistTracks = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS));

                // Save to artistList
                artistList.add(new Artist(artistId, artistName, artistAlbums, artistTracks));
            }
        }
        cursor.close();

        if(artistList.isEmpty()) {
            return null;
        } else {
            return artistList;
        }
    }
}
