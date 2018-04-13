package com.ecebuc.gesmediaplayer.Fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ecebuc.gesmediaplayer.AudioUtils.SongAdapter;
import com.ecebuc.gesmediaplayer.AudioUtils.StorageUtils;
import com.ecebuc.gesmediaplayer.Audios.Audio;
import com.ecebuc.gesmediaplayer.R;
import com.ecebuc.gesmediaplayer.Utils.SimpleDividerItemDecoration;
import com.ecebuc.gesmediaplayer.Utils.RecyclerTouchListener;

import java.util.ArrayList;

public class SongsFragment extends Fragment {

    private OnSongFragmentInteractionListener songFragmentCallback;

    private RecyclerView songRecyclerView;
    private RecyclerView.Adapter songRecyclerAdapter;
    private RecyclerView.LayoutManager recyclerLayoutManager;
    private ArrayList<Audio> songList = new ArrayList<>();

    private final String IS_FRAGMENT_TYPE_DEFAULT_TAG = "isFragmentDefaultTag";
    private final String fromAlbumIDParamTAG = "Album ID";
    private boolean paramIsFragmentDefault;
    private String paramAlbumID;
    private String fragmentTitle;

    public SongsFragment() {
        // Required empty public constructor
    }
    public static SongsFragment newInstance(/*String param1, String param2*/) {
        SongsFragment fragment = new SongsFragment();
        /*Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);*/
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSongFragmentInteractionListener) {
            songFragmentCallback = (OnSongFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnSongFragmentInteractionListener");
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        paramIsFragmentDefault = args.getBoolean(IS_FRAGMENT_TYPE_DEFAULT_TAG, true);

        if(paramIsFragmentDefault){
            //default behavior - display all albums
            //get the songs list for the recyclerview adapter
            StorageUtils storageUtils = new StorageUtils(getActivity());
            songList = storageUtils.loadAudio();
            //initSongList();
            fragmentTitle = "All Songs";
        } else {
            //fragment was created with arguments - handle accordingly
            paramAlbumID = args.getString(fromAlbumIDParamTAG);
            if(paramAlbumID != null){
                initSongListFromId(paramAlbumID);
            } else {
                Log.e("SongsFrag onCreate: ", "error - paramAlbumID is null");
            }
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_songs, container, false);

        //Recycler view setup for songs display
        songRecyclerView = (RecyclerView) rootView.findViewById(R.id.songs_recycler_view);
        songRecyclerView.setHasFixedSize(true);
        songRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(getContext()));

        recyclerLayoutManager = new LinearLayoutManager(getActivity());
        songRecyclerView.setLayoutManager(recyclerLayoutManager);

        songRecyclerAdapter = new SongAdapter(songList);
        songRecyclerView.setAdapter(songRecyclerAdapter);
        songRecyclerView.setItemAnimator(new DefaultItemAnimator());
        songRecyclerView.addOnItemTouchListener(
                new RecyclerTouchListener(getContext(), songRecyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Audio selectedSong = songList.get(position);
                Toast.makeText(getContext(), selectedSong.getTitle() + " is selected!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLongClick(View view, int position) {
                Toast.makeText(getContext(), "Long selected", Toast.LENGTH_SHORT).show();
            }
        }));

        return rootView;
    }
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        songFragmentCallback.updateToolbarTitleForFragment(fragmentTitle);
    }
    @Override
    public void onDetach() {
        super.onDetach();
        songFragmentCallback = null;
    }

    public interface OnSongFragmentInteractionListener {
        void updateToolbarTitleForFragment(String fragmentTitle);
    }

    private void initSongListFromId(String id){
        ContentResolver contentResolver = getActivity().getContentResolver();
        String data, title, album, artist, albumArt, albumId;

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        selection += " and album_id = " + id;
        String sortOrder = MediaStore.Audio.AudioColumns.TITLE + " COLLATE LOCALIZED ASC";

        String[] ALBUM_SUMMARY_PROJECTION = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID
        };

        Cursor cursor = contentResolver.query(uri,
                            ALBUM_SUMMARY_PROJECTION,
                            selection, null,
                            sortOrder);

        Cursor albumArtCursor = contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[] {MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                MediaStore.Audio.Albums._ID + "=?",
                new String[] {String.valueOf(id)},
                null);
        albumArtCursor.moveToFirst();
        albumArt = albumArtCursor.getString(albumArtCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
        albumArtCursor.close();

        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

                // Save to audioList
                songList.add(new Audio(data, title, album, artist, albumArt));
            }
            //set the fragment name to the artist of which we are displaying the albums
            cursor.moveToPrevious();
            fragmentTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
        }
        cursor.close();
    }
}
