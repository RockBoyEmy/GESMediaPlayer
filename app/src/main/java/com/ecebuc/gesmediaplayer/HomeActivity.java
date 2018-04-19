package com.ecebuc.gesmediaplayer;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.ecebuc.gesmediaplayer.Fragments.AlbumsFragment;
import com.ecebuc.gesmediaplayer.Fragments.ArtistsFragment;
import com.ecebuc.gesmediaplayer.Fragments.SongsFragment;


public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
                    SongsFragment.OnSongFragmentInteractionListener,
                    AlbumsFragment.OnAlbumFragmentInteractionListener,
                    ArtistsFragment.OnArtistFragmentInteractionListener {

    private final int REQUEST_CODE_GESPLAYER_EXTERNAL_STORAGE = 101;
    final String IS_FRAGMENT_TYPE_DEFAULT_TAG = "isFragmentDefaultTag";
    final String HOME_LOG = "HOME ACTIVITY: ";

    private MediaBrowserCompat gesMediaBrowser;
    private MediaControllerCompat gesMediaController;
    private MediaControllerCompat.TransportControls gesPlaybackTransportControls;

    private MediaBrowserCompat.ConnectionCallback mediaBrowserCallbacks = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            super.onConnected();
            try {
                Log.d(HOME_LOG, "entered onConnected");
                //create the media controller and register the callbacks to stay in sync
                gesMediaController = new MediaControllerCompat(getApplicationContext(), gesMediaBrowser.getSessionToken());
                gesMediaController.registerCallback(mediaControllerCallbacks);

                //save the controller and define the easy access transport controls in the object
                MediaControllerCompat.setMediaController(HomeActivity.this, gesMediaController);
                gesPlaybackTransportControls = gesMediaController.getTransportControls();
                //gesPlaybackTransportControls.playFromMediaId(String.valueOf(R.raw.warner_tautz_off_broadway), null);

                //Display initial state
                MediaMetadataCompat metadata = gesMediaController.getMetadata();
                PlaybackStateCompat pbState = gesMediaController.getPlaybackState();

            } catch( RemoteException e ) {
                e.getMessage();
                Log.d(HOME_LOG, "onConnected: some exception was thrown, see what can you find out");
            }
            Log.d(HOME_LOG, "left onConnect");
        }
        @Override
        public void onConnectionSuspended() {
            // The Service has crashed. Disable transport controls until it automatically reconnects
            Log.d(HOME_LOG, "onConnectionSuspend: the service has crashed");
            gesPlaybackTransportControls = null;
        }
        @Override
        public void onConnectionFailed() {
            // The Service has refused our connection
            Log.d(HOME_LOG, "onConnectionFail: the service hasn't been able to connect");
        }
    };
    private MediaControllerCompat.Callback mediaControllerCallbacks = new MediaControllerCompat.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
        }
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            if( state == null ) {
                Log.d(HOME_LOG, "onPlaybackChange: the state is null");
                return;
            }

            switch( state.getState() ) {
                case PlaybackStateCompat.STATE_PLAYING: {
                    break;
                }
                case PlaybackStateCompat.STATE_PAUSED: {
                    break;
                }
            }
        }
        @Override
        public void onSessionDestroyed(){
            // Override to handle the session being destroyed
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Log.d(HOME_LOG, "onCreate entered");
        // Check that the activity is using the correct layout version for fragments
        if (findViewById(R.id.home_fragment_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            //request permissions for external storage
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission have not been granted
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_GESPLAYER_EXTERNAL_STORAGE);
            } else {
                Log.d(HOME_LOG, "onCreate - permissions already granted");

                //TODO =========================================================
                //TODO: whatever you add in onCreate, remember about the permissions function too
                //TODO =========================================================

                //initiate device scan for audio files and create a list for them
                //loadAudio();
                Bundle args = new Bundle();

                SongsFragment firstFragment = new SongsFragment();
                firstFragment.setArguments(args);
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.home_fragment_container, firstFragment).commit();
            }
        }


        //Custom toolbar setup
        Toolbar activity_home_toolbar = (Toolbar) findViewById(R.id.toolbar_home);
        setSupportActionBar(activity_home_toolbar);

        //Navigation drawer and animation setup for the activity
        DrawerLayout nav_drawer = (DrawerLayout) findViewById(R.id.nav_drawer_layout);
        ActionBarDrawerToggle navToggleActionButton = new ActionBarDrawerToggle(this,
                nav_drawer,
                activity_home_toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        nav_drawer.addDrawerListener(navToggleActionButton);
        navToggleActionButton.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_drawer_view);
        navigationView.setNavigationItemSelectedListener(this);

        Log.d(HOME_LOG, "onCreate exited");
    }
    @Override
    public void onStart(){
        super.onStart();
        Log.d(HOME_LOG, "onStart entered");

        //bind to the service again if it was created and started and the user is returning to app
        if(!MediaPlaybackService.isServiceStarted)
        {
            gesMediaBrowser = new MediaBrowserCompat(this,
                    new ComponentName(this, MediaPlaybackService.class),
                    mediaBrowserCallbacks, getIntent().getExtras());
            gesMediaBrowser.connect();
        }

        Log.d(HOME_LOG, "onStart exited");
    }
    @Override
    public void onStop(){
        super.onStop();
        Log.d(HOME_LOG, "onStop entered");

        //disconnect the media browser from the service but keep playing in background
        if (gesMediaController != null) {
            gesMediaController.unregisterCallback(mediaControllerCallbacks);
            Log.d(HOME_LOG, "control callback unregistered");
        }
        gesMediaBrowser.disconnect();
        if(gesMediaBrowser.isConnected()){
            Log.d(HOME_LOG, "mediaBrowser is somehow still connected");
        }

        Log.d(HOME_LOG, "onStop exited");
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(HOME_LOG, "entered onDestroy");
        gesPlaybackTransportControls.stop();

        gesMediaBrowser.disconnect();
        gesMediaBrowser = null;
        Log.d(HOME_LOG, "exited onDestroy");
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.nav_drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        else{
            //go back through fragments stack, but stop when reached the main 3 frags
            int count = getSupportFragmentManager().getBackStackEntryCount();
            Log.d(HOME_LOG, "onBackPressed: count = " + Integer.toString(count));
            count--;
            if(count >= 0){
                super.onBackPressed();
            }
            else{
                this.moveTaskToBack(true);
            }
        }
    }

    //------------------------------Toolbar & NavDrawer options & misc----------------------------//
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home_toolbar_popup, menu);

        MenuItem searchItem = menu.findItem(R.id.toolbar_action_search); //the search button itself
        SearchView searchView = (SearchView) searchItem.getActionView(); //the textView of the search

        /* Can now use the view to extract the content (search words) from
         * it and perform the searching operation*/

        // Configure the search info and add any event listeners...

        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.toolbar_action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        FragmentTransaction fragmentTransaction;
        Bundle fragmentArgs = new Bundle();
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_playing_now:
                // TODO: Check if any song is playing, if not, don't start, display message
                // Navigate back to the Main activity
                Intent mainActivityIntent = new Intent(this, MainActivity.class);
                mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(mainActivityIntent);
                break;
            case R.id.nav_all_songs:
                // Create the songs fragment
                SongsFragment songsFragment = new SongsFragment();
                //fragmentArgs.putBoolean(IS_FRAGMENT_TYPE_DEFAULT_TAG, true);
                songsFragment.setArguments(fragmentArgs);

                getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.home_fragment_container, songsFragment);
                //fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                break;

            case R.id.nav_artists:
                ArtistsFragment artistsFragment = new ArtistsFragment();
                //fragmentArgs.putBoolean(IS_FRAGMENT_TYPE_DEFAULT_TAG, true);
                artistsFragment.setArguments(fragmentArgs);

                //clear back stack and start fresh
                getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.home_fragment_container, artistsFragment);
                //fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                break;
            case R.id.nav_albums:
                AlbumsFragment albumsFragment = new AlbumsFragment();
                //fragmentArgs.putBoolean(IS_FRAGMENT_TYPE_DEFAULT_TAG, true);
                albumsFragment.setArguments(fragmentArgs);

                getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.home_fragment_container, albumsFragment);
                //fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                break;

            case R.id.nav_settings:
                break;
            case R.id.nav_share:
                break;
        }

        DrawerLayout nav_drawer = (DrawerLayout) findViewById(R.id.nav_drawer_layout);
        nav_drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    //-------------------------------Fragment Interfaces Functions--------------------------------//
    @Override
    public void updateToolbarTitleForFragment(String fragmentTitle) {
        try {
            getSupportActionBar().setTitle(fragmentTitle);
        } catch (Exception e){
            e.printStackTrace();
            Log.d("updateToolbarTitle: ", "setTitle: " + e);
        }
    }
    @Override
    public void startAlbumsFragmentFromId(String artistId){
        Bundle fragmentArgs = new Bundle();
        fragmentArgs.putString("Artist ID", artistId);
        fragmentArgs.putBoolean(IS_FRAGMENT_TYPE_DEFAULT_TAG, false);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        AlbumsFragment albumFromIdFragment = new AlbumsFragment();
        albumFromIdFragment.setArguments(fragmentArgs);
        fragmentTransaction.replace(R.id.home_fragment_container, albumFromIdFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
    @Override
    public void startSongsFragmentFromId(String albumId){
        Bundle fragmentArgs = new Bundle();
        fragmentArgs.putString("Album ID", albumId);
        fragmentArgs.putBoolean(IS_FRAGMENT_TYPE_DEFAULT_TAG, false);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        SongsFragment songFromIdFragment = new SongsFragment();
        songFromIdFragment.setArguments(fragmentArgs);
        fragmentTransaction.replace(R.id.home_fragment_container, songFromIdFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
    @Override
    public void playSelectedSong(int position){
        if(!MediaPlaybackService.isServiceStarted){
            startService(new Intent(this, MediaPlaybackService.class));
        }
        String stubSongId = "stub";
        Bundle extras = new Bundle();
        extras.putInt("songPosition", position);
        gesPlaybackTransportControls.playFromMediaId(stubSongId, extras);
    }


    //----------------------------------------Permissions-----------------------------------------//
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_GESPLAYER_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("onRequestPermissions: ", "permissions granted for external storage");

                    //initiate connection to the MediaPlaybackService through MediaBrowser
                    gesMediaBrowser = new MediaBrowserCompat(this,
                            new ComponentName(this, MediaPlaybackService.class),
                            mediaBrowserCallbacks, getIntent().getExtras());
                    gesMediaBrowser.connect();

                    //initiate device scan for audio files and create a list for them
                    Bundle args = new Bundle();

                    SongsFragment firstFragment = new SongsFragment();
                    firstFragment.setArguments(args);
                    getSupportFragmentManager().beginTransaction()
                            .add(R.id.home_fragment_container, firstFragment).commit();

                } else {
                    //close the app if permissions aren't granted
                    finish();
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }
}
