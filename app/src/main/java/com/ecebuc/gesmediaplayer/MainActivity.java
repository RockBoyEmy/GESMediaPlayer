package com.ecebuc.gesmediaplayer;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.ecebuc.gesmediaplayer.AudioUtils.StorageUtils;
import com.ecebuc.gesmediaplayer.Audios.Audio;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button playPauseToggleButton, loadNavActivityButton;
    ImageView headerBackButtons;

    private final int REQUEST_CODE_GESPLAYER_EXTERNAL_STORAGE = 101;
    private final String MAIN_LOG = "MAIN ACTIVITY: ";
    private static final int STATE_PAUSED = 0;
    private static final int STATE_PLAYING = 1;
    private static int currentPlaybackState;

    private ArrayList<Audio> audioFilesOnDevice;
    private int audioIndex;

    private MediaBrowserCompat gesMediaBrowser;
    private MediaControllerCompat gesMediaController;
    private MediaControllerCompat.TransportControls gesPlaybackTransportControls;



    //-------------------------------Session and Controller Callbacks-----------------------------//

    private MediaBrowserCompat.ConnectionCallback mediaBrowserCallbacks = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            super.onConnected();
            try {
                Log.d(MAIN_LOG, "entered onConnected");
                //create the media controller and register the callbacks to stay in sync
                gesMediaController = new MediaControllerCompat(MainActivity.this, gesMediaBrowser.getSessionToken());
                gesMediaController.registerCallback(mediaControllerCallbacks);

                //save the controller and define the easy access transport controls in the object
                MediaControllerCompat.setMediaController(MainActivity.this, gesMediaController);
                gesPlaybackTransportControls = gesMediaController.getTransportControls();
                //gesPlaybackTransportControls.playFromMediaId(String.valueOf(R.raw.warner_tautz_off_broadway), null);

                //Display initial state
                MediaMetadataCompat metadata = gesMediaController.getMetadata();
                PlaybackStateCompat pbState = gesMediaController.getPlaybackState();

            } catch( RemoteException e ) {
                e.getMessage();
                Log.d(MAIN_LOG, "onConnected some exception was thrown, see what can you find out");
            }
            Log.d(MAIN_LOG, "left onConnected");
        }
        @Override
        public void onConnectionSuspended() {
            // The Service has crashed. Disable transport controls until it automatically reconnects
            Log.d(MAIN_LOG, "onConnectionSuspended the service has crashed");
            gesPlaybackTransportControls = null;
        }
        @Override
        public void onConnectionFailed() {
            // The Service has refused our connection
            Log.d(MAIN_LOG, "onConnectionFailed: the service hasn't been able to connect");
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
                Log.d(MAIN_LOG, "onPlaybackStateChanged: the state is null");
                return;
            }

            switch( state.getState() ) {
                case PlaybackStateCompat.STATE_PLAYING: {
                    currentPlaybackState = STATE_PLAYING;
                    break;
                }
                case PlaybackStateCompat.STATE_PAUSED: {
                    currentPlaybackState = STATE_PAUSED;
                    break;
                }
            }
        }
        @Override
        public void onSessionDestroyed(){
            // Override to handle the session being destroyed
        }
    };



    //-----------------------------------Activity lifecycle methods-------------------------------//

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(MAIN_LOG, "onCreate entered");

        //grab the buttons for media playback control
        playPauseToggleButton = (Button) findViewById(R.id.playPause_btn);
        loadNavActivityButton = (Button) findViewById(R.id.loadNavActivity_btn);
        headerBackButtons = (ImageView) findViewById(R.id.home_header_back);

        //initialize the songs list
        audioFilesOnDevice = new ArrayList<Audio>();
        audioIndex = -1;


        //request permissions for external storage
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission have not been granted
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_GESPLAYER_EXTERNAL_STORAGE);
        } else {
            Log.d(MAIN_LOG, "onCreate permissions already granted");

            //initiate device scan for audio files and create a list for them
            //loadAudio();
        }

        playPauseToggleButton.setOnClickListener(this);
        loadNavActivityButton.setOnClickListener(this);
        headerBackButtons.setOnClickListener(this);
        Log.d(MAIN_LOG, "exited onCreate");
    }
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(MAIN_LOG, "onStart entered");

        //bind to the service again if it was created and started and the user is returning to app
        if(!MediaPlaybackService.isServiceStarted)
        {
            gesMediaBrowser = new MediaBrowserCompat(this,
                    new ComponentName(this, MediaPlaybackService.class),
                    mediaBrowserCallbacks, getIntent().getExtras());
            gesMediaBrowser.connect();
        }
        Log.d(MAIN_LOG, "onStart exited");
    }
    @Override
    protected void onStop() {
        super.onStop();
        /* disconnect the media browser from the service and unregister
        * the media controller callback but do not stop background music from playing
        * this way we can use other apps but still listen to the audio playback */
        Log.d(MAIN_LOG, "onStop entered");
        if (gesMediaController != null) {
            gesMediaController.unregisterCallback(mediaControllerCallbacks);
            Log.d(MAIN_LOG, "control callback unregistered");
        }
        gesMediaBrowser.disconnect();
        if(gesMediaBrowser.isConnected()){
            Log.d(MAIN_LOG, "mediaBrowser is somehow still connected");
        }
        Log.d(MAIN_LOG, "onStop exited");
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(MAIN_LOG, "entered onDestroy");
        //gesPlaybackTransportControls.stop();

        gesMediaBrowser.disconnect();
        //gesMediaBrowser = null;
        Log.d(MAIN_LOG, "exited onDestroy");
    }
    @Override
    public void onBackPressed(){
        Intent goBackIntent = NavUtils.getParentActivityIntent(this);
        goBackIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        NavUtils.navigateUpTo(this, goBackIntent);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.playPause_btn:
                //has to be dealt with accordingly, based on the current state of mediaplayer
                if( currentPlaybackState == STATE_PAUSED ) {
                    gesPlaybackTransportControls.play();
                    currentPlaybackState = STATE_PLAYING;
                } else {
                    if( gesMediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING ) {
                        gesPlaybackTransportControls.pause();
                    }
                    currentPlaybackState = STATE_PAUSED;
                }
                break;

            case R.id.loadNavActivity_btn:
                Intent loadIntent = new Intent(this, HomeActivity.class);
                startActivity(loadIntent);
                break;

            case R.id.home_header_back:
                // Same as for onBackPressed()
                Intent goBackIntent = NavUtils.getParentActivityIntent(this);
                goBackIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                NavUtils.navigateUpTo(this, goBackIntent);
                break;

            //space for future cases here
            //
            //
        }
    }



    //-----------------------------------Audio file load methods----------------------------------//

    /*private void loadAudio() {
        Log.d("loadAudio: ", "entered loadAudio");
        ContentResolver contentResolver = getContentResolver();
        String data, title, album, artist, albumArt;

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);



        //TODO: see this whole thing better with the album art



        if (cursor != null && cursor.getCount() > 0) {
            //audioFilesOnDevice = new ArrayList<>();
            while (cursor.moveToNext()) {
                String albumId = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                 data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                 title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                 album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                 artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                // albumArt = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));

                Cursor albumArtCursor = getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        new String[] {MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                        MediaStore.Audio.Albums._ID+ "=?",
                        new String[] {String.valueOf(albumId)},
                        null);
                albumArtCursor.moveToFirst();
                albumArt = albumArtCursor.getString(albumArtCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));

                // Save to audioList
                audioFilesOnDevice.add(new Audio(data, title, album, artist, albumArt));
            }
        }
        cursor.close();

        if(!audioFilesOnDevice.isEmpty()) {
            StorageUtils storage = new StorageUtils(getApplicationContext());
            storage.storeAudio(audioFilesOnDevice);
        }

        Log.d("loadAudio: ", "exited loadAudio");
    }*/



    //------------------------------------------Permissions---------------------------------------//
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_GESPLAYER_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("onRequestPermissions: ", "permissions granted for external storage");
                    //loadAudio();

                    //initiate connection to the MediaPlaybackService through MediaBrowser
                    gesMediaBrowser = new MediaBrowserCompat(this,
                            new ComponentName(this, MediaPlaybackService.class),
                            mediaBrowserCallbacks, getIntent().getExtras());
                    gesMediaBrowser.connect();

                } else {
                    //close the app if permissions aren't granted
                    finish();
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }


    /*Not sure about the following if needed or not in this implementation of the application
     * it will stay commented-out for now, implement in case of weird behavior and app crashes
     * apparently this is how it will fix it, but check with old version, with binders*/
    //@Override
    //public void onSaveInstanceState(Bundle savedInstanceState) {
    //    savedInstanceState.putBoolean("ServiceState", serviceBound);
    //   super.onSaveInstanceState(savedInstanceState);
    //}
    //@Override
    //public void onRestoreInstanceState(Bundle savedInstanceState) {
    //    super.onRestoreInstanceState(savedInstanceState);
    //    serviceBound = savedInstanceState.getBoolean("ServiceState");
    //}
}
