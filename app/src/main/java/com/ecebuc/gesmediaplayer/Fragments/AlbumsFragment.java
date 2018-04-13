package com.ecebuc.gesmediaplayer.Fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ecebuc.gesmediaplayer.AudioUtils.AlbumAdapter;
import com.ecebuc.gesmediaplayer.Audios.Album;
import com.ecebuc.gesmediaplayer.R;
import com.ecebuc.gesmediaplayer.Utils.RecyclerTouchListener;

import java.util.ArrayList;

public class AlbumsFragment extends Fragment {

    private OnAlbumFragmentInteractionListener albumFragmentCallback;

    private RecyclerView albumRecyclerView;
    private RecyclerView.Adapter albumRecyclerAdapter;
    private RecyclerView.LayoutManager recyclerLayoutManager;
    private ArrayList<Album> albumList = new ArrayList<>();

    private final String IS_FRAGMENT_TYPE_DEFAULT_TAG = "isFragmentDefaultTag";
    private final String fromArtistIDParamTAG = "Artist ID";
    private boolean paramIsFragmentDefault;
    private String paramArtistID;
    private String fragmentTitle;

    public AlbumsFragment() {
        // Required empty public constructor
    }
    public static AlbumsFragment newInstance(String param1, String param2) {
        AlbumsFragment fragment = new AlbumsFragment();
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnAlbumFragmentInteractionListener) {
            albumFragmentCallback = (OnAlbumFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnAlbumFragmentInteractionListener");
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        paramIsFragmentDefault = args.getBoolean(IS_FRAGMENT_TYPE_DEFAULT_TAG, true);

        if(paramIsFragmentDefault){
            //default behavior - display all albums
            initAlbumList();
            fragmentTitle = "Albums";
        } else {
            //fragment was created with arguments - handle accordingly
            paramArtistID = args.getString(fromArtistIDParamTAG);
            if(paramArtistID != null){
                initAlbumListFromId(paramArtistID);
            } else {
                Log.e("AlbumFrag onCreate: ", "error - paramArtistID is null");
            }
        }
        //populating the list with the albums on device
        //albumList = initAlbumList();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_albums, container, false);

        albumRecyclerView = (RecyclerView) rootView.findViewById(R.id.albums_recycler_view);
        albumRecyclerView.setHasFixedSize(true);

        recyclerLayoutManager = new GridLayoutManager(getActivity(), 2);
        albumRecyclerView.setLayoutManager(recyclerLayoutManager);

        albumRecyclerAdapter = new AlbumAdapter(albumList);
        albumRecyclerView.setAdapter(albumRecyclerAdapter);
        albumRecyclerView.setItemAnimator(new DefaultItemAnimator());
        albumRecyclerView.addOnItemTouchListener(
                new RecyclerTouchListener(getContext(), albumRecyclerView, new RecyclerTouchListener.ClickListener() {
                    @Override
                    public void onClick(View view, int position) {
                        Album selectedAlbum = albumList.get(position);
                        String albumId = selectedAlbum.getAlbumId();
                        albumFragmentCallback.startSongsFragmentFromId(albumId);
                        //Toast.makeText(getContext(), selectedAlbum.getAlbumName() + " is selected!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onLongClick(View view, int position) {
                        Toast.makeText(getContext(), "Long selected", Toast.LENGTH_SHORT).show();
                        //could implement the popup menu in here
                    }
                }));

        return rootView;
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        albumFragmentCallback.updateToolbarTitleForFragment(fragmentTitle);
    }
    @Override
    public void onDetach() {
        super.onDetach();
        albumFragmentCallback = null;
    }

    public interface OnAlbumFragmentInteractionListener {
        void updateToolbarTitleForFragment(String fragmentTitle);
        void startSongsFragmentFromId(String albumId);
    }

    private void /*ArrayList<Album>*/ initAlbumList() {
        //this function populates the global albumList directly through the parallel thread
        ContentResolver contentResolver = getActivity().getContentResolver();
        Handler albumLoadHandler;
        Runnable albumLoadRunnable;
        final int TIMES_TO_NEXT_ADAPTER_NOTIFY = 5;
        //ArrayList<Album> albumList = new ArrayList<>();

        final String[] ALBUM_SUMMARY_PROJECTION = {
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.ALBUM_ART};

        Uri albumUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        String sortOrder = MediaStore.Audio.Albums.ALBUM + " ASC";
        final Cursor cursor = contentResolver.query(
                albumUri,
                ALBUM_SUMMARY_PROJECTION,
                null, null,
                sortOrder);

        Log.d("initAlbumList: ", "Album query performed");

        albumLoadHandler = new Handler();
        albumLoadRunnable = new Runnable() {
            @Override
            public void run() {
                String albumId, albumTitle, albumArtist, albumArt;
                Cursor runnableCursor = cursor;
                int notifyCounter = TIMES_TO_NEXT_ADAPTER_NOTIFY;

                if (runnableCursor != null && runnableCursor.getCount() > 0) {
                    while (runnableCursor.moveToNext()) {
                        albumId = runnableCursor.getString(runnableCursor.getColumnIndex(MediaStore.Audio.Albums._ID));
                        albumTitle = runnableCursor.getString(runnableCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
                        albumArtist = runnableCursor.getString(runnableCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
                        albumArt = runnableCursor.getString(runnableCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));

                        // Save to albumList
                        albumList.add(new Album(albumId, albumTitle, albumArtist, albumArt));
                        Log.d("initAlbumLoad: ", "added " + albumTitle + " to the list");
                        Log.d("initAlbumLoad: ", "notifyCounter is: " + notifyCounter);
                        notifyCounter--;

                        //if dataset was updated the set amount of times, notify adapter
                        if (notifyCounter == 0) {
                            albumRecyclerAdapter.notifyDataSetChanged();
                            Log.d("initAlbumLoad: ", "adapter notified");
                            notifyCounter = 5;
                        }
                    }

                    //when all device files have been loaded, notify adapter one last time
                    albumRecyclerAdapter.notifyDataSetChanged();
                    Log.d("initAlbumLoad: ", "adapter notified the last time");
                }
                runnableCursor.close();
                if (albumList.isEmpty()) {
                    Toast.makeText(getActivity(), "There are no albums on device", Toast.LENGTH_SHORT).show();
                }
            }
        };
        albumLoadHandler.post(albumLoadRunnable);
    }
    private void initAlbumListFromId(String id){
        ContentResolver contentResolver = getActivity().getContentResolver();
        String albumId, albumTitle, albumArtist, albumArt;
        long artistId = Long.parseLong(id);

        final String[] ALBUM_SUMMARY_PROJECTION = {
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.ALBUM_ART};

        Uri albumUri = MediaStore.Audio.Artists.Albums.getContentUri("external", artistId);
        String sortOrder = MediaStore.Audio.Albums.ALBUM + " ASC";
        final Cursor cursor = contentResolver.query(
                albumUri,
                ALBUM_SUMMARY_PROJECTION,
                null, null,
                sortOrder);


        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                albumId = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));
                albumTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
                albumArtist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
                albumArt = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));

                // Save to albumList
                albumList.add(new Album(albumId, albumTitle, albumArtist, albumArt));
            }
            //set the fragment name to the artist of which we are displaying the albums
            cursor.moveToPrevious();
            fragmentTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
        }
        cursor.close();
    }
}







/*public class AlbumsFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private OnAlbumFragmentInteractionListener albumFragmentCallback;
    private final static int LOADER_ID = 0;
    private RecyclerView albumRecyclerView;
    private AlbumAdapter albumRecyclerAdapter;
    private RecyclerView.LayoutManager recyclerLayoutManager;


    public AlbumsFragment() {
    }
    public static AlbumsFragment newInstance(String paramArtistID, String param2) {
        AlbumsFragment fragment = new AlbumsFragment();
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnAlbumFragmentInteractionListener) {
            mListener = (OnAlbumFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnAlbumFragmentInteractionListener");
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //populating the list with the albums on device
        //albumList = initAlbumList();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_albums, container, false);

        //Recycler view setup for songs display
        albumRecyclerView = (RecyclerView) rootView.findViewById(R.id.albums_recycler_view);
        albumRecyclerView.setHasFixedSize(true);

        recyclerLayoutManager = new GridLayoutManager(getActivity(), 2);
        albumRecyclerView.setLayoutManager(recyclerLayoutManager);

        albumRecyclerAdapter = new AlbumAdapter(getActivity(), null);
        albumRecyclerView.setAdapter(albumRecyclerAdapter);
        albumRecyclerView.setItemAnimator(new DefaultItemAnimator());

        return rootView;
    }
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final String[] ALBUM_SUMMARY_PROJECTION = {
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.ALBUM_ART };
        Uri albumUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        String sortOrder = MediaStore.Audio.Albums.ALBUM + " ASC";

        return new CursorLoader(getActivity(),
                albumUri,
                ALBUM_SUMMARY_PROJECTION,
                null, null,
                sortOrder);
    }
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        //albumRecyclerAdapter.setData(data);
        albumRecyclerAdapter.swapCursor(data);
    }
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //albumRecyclerAdapter.setData(null);
        albumRecyclerAdapter.swapCursor(null);
    }

    public void onDetach() {
        super.onDetach();
        albumFragmentCallback = null;
    }

    public interface OnAlbumFragmentInteractionListener {
        void updateToolbarTitleForFragment(String fragmentTitle);
    }
}*/
