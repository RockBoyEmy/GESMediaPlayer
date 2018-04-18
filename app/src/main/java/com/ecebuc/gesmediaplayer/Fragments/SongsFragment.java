package com.ecebuc.gesmediaplayer.Fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
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
    private SongLoaderAsync songLoader;

    private final String IS_FRAGMENT_TYPE_DEFAULT_TAG = "isFragmentDefaultTag";
    private String fragmentTitle;

    private final String fromAlbumIDParamTAG = "Album ID";
    private boolean paramIsFragmentDefault;
    private String paramAlbumID;

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
            //default behavior - display all device songs
            //will be done in onCreateView
            fragmentTitle = "All Songs";
        } else {
            //fragment was probably created from an album - handle accordingly
            paramAlbumID = args.getString(fromAlbumIDParamTAG);
            if(paramAlbumID == null){
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

        //here goes the AsyncTask
        if(paramIsFragmentDefault){
            //fragment is new, load all songs asynchronously
            songLoader = new SongLoaderAsync(songRecyclerView.getAdapter());
            initSongAsyncTask(songLoader);
        }
        else {
            //songList has songs in it, means fragment comes from an album
            initSongListFromId(paramAlbumID);
        }

        songRecyclerView.setItemAnimator(new DefaultItemAnimator());
        songRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), songRecyclerView, new RecyclerTouchListener.ClickListener() {
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
    public void onDestroy(){
        super.onDestroy();

        if(songLoader != null && !songLoader.isCancelled()){
            songLoader.cancel(true);
        }
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
        String sortOrder = MediaStore.Audio.AudioColumns.TRACK + " ASC";

        String[] SONG_SUMMARY_PROJECTION = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID
        };

        Cursor cursor = contentResolver.query(uri,
                            SONG_SUMMARY_PROJECTION,
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
                songRecyclerView.getAdapter().notifyItemInserted(cursor.getPosition());
            }
            //set the fragment name to the artist of which we are displaying the albums
            cursor.moveToPrevious();
            fragmentTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
        }
        cursor.close();
    }
    private void initSongAsyncTask(SongLoaderAsync songLoader){
        ContentResolver contentResolver = getActivity().getContentResolver();
        final String[] SONG_SUMMARY_PROJECTION = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID
        };

        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        final Cursor cursor = contentResolver.query(
                songUri,
                SONG_SUMMARY_PROJECTION,
                selection, null,
                sortOrder);

        //begin song load asynchronously
        songLoader.execute(cursor);
    }

    private class SongLoaderAsync extends AsyncTask<Cursor, Audio, Void> {
        RecyclerView.Adapter recyclerAdapter;
        ContentResolver contentResolver;
        String data, title, album, artist, albumArt, albumId;
        int position;

        public SongLoaderAsync(RecyclerView.Adapter adapter) {
            recyclerAdapter = adapter;
            contentResolver = getActivity().getContentResolver();
            position = -1;
        }

        @Override
        protected Void doInBackground(Cursor... passedCursor) {

            if (passedCursor[0] != null && passedCursor[0].getCount() > 0) {
                while (passedCursor[0].moveToNext()) {
                    albumId = passedCursor[0].getString(passedCursor[0].getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                    data = passedCursor[0].getString(passedCursor[0].getColumnIndex(MediaStore.Audio.Media.DATA));
                    title = passedCursor[0].getString(passedCursor[0].getColumnIndex(MediaStore.Audio.Media.TITLE));
                    album = passedCursor[0].getString(passedCursor[0].getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    artist = passedCursor[0].getString(passedCursor[0].getColumnIndex(MediaStore.Audio.Media.ARTIST));

                    Cursor albumArtCursor = contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                            new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                            MediaStore.Audio.Albums._ID + "=?",
                            new String[]{String.valueOf(albumId)},
                            null);
                    albumArtCursor.moveToFirst();
                    albumArt = albumArtCursor.getString(albumArtCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));

                    position++;
                    publishProgress(new Audio(data, title, album, artist, albumArt));

                    //periodically check if a cancel command was given, to exit rapidly
                    if(isCancelled())break;
                }
            }
            passedCursor[0].close();
            return null;
        }
        @Override
        protected void onProgressUpdate(Audio... newSong) {
            //super.onProgressUpdate(newSong);
            songList.add(newSong[0]);
            recyclerAdapter.notifyItemInserted(position);

        }
        @Override
        protected void onPostExecute(Void aVoid) {
            //super.onPostExecute(aVoid);
        }
    }
}
