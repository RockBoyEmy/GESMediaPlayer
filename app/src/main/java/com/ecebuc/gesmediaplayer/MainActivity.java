package com.ecebuc.gesmediaplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.v4.app.NavUtils;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;

import com.ecebuc.gesmediaplayer.Utils.OnSwipeListener;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
                                                                View.OnTouchListener{

    ImageView playPauseToggleButton, nextButton, previousButton;
    ImageView headerBackButton, smallArtCover, mainArtCover;
    TextView currentSongTitle, currentArtistName;

    GestureDetectorCompat gestureDetector;

    private final String MAIN_LOG = "MAIN ACTIVITY: ";

    private MediaBrowserCompat gesMediaBrowser;
    private MediaControllerCompat gesMediaController;
    private MediaControllerCompat.TransportControls gesPlaybackTransportControls;

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
                mainArtCover.setImageBitmap(metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART));
                smallArtCover.setImageBitmap(metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART));
                currentSongTitle.setText(metadata.getText(MediaMetadataCompat.METADATA_KEY_TITLE));
                currentArtistName.setText(metadata.getText(MediaMetadataCompat.METADATA_KEY_ARTIST));

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
            //mainArtCover.setImageBitmap(metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART));
            ImageViewAnimatedChange(getApplicationContext(), mainArtCover, metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART));
            smallArtCover.setImageBitmap(metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART));
            currentSongTitle.setText(metadata.getText(MediaMetadataCompat.METADATA_KEY_TITLE));
            currentArtistName.setText(metadata.getText(MediaMetadataCompat.METADATA_KEY_ARTIST));
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
                    playPauseToggleButton.setImageResource(R.drawable.ic_pause_black_24dp);
                    break;
                }
                case PlaybackStateCompat.STATE_PAUSED: {
                    playPauseToggleButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
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

        //register all buttons and views
        playPauseToggleButton = (ImageView) findViewById(R.id.home_playPause);
        nextButton = (ImageView)findViewById(R.id.home_next);
        previousButton = (ImageView) findViewById(R.id.home_prev);
        headerBackButton = (ImageView) findViewById(R.id.home_header_back);
        smallArtCover = (ImageView) findViewById(R.id.home_header_albumCoverSmall);
        mainArtCover = (ImageView) findViewById(R.id.main_albumCoverLarge);
        currentSongTitle = (TextView) findViewById(R.id.home_header_songTitle);
        currentArtistName = (TextView) findViewById(R.id.home_header_artistName);

        playPauseToggleButton.setOnClickListener(this);
        nextButton.setOnClickListener(this);
        previousButton.setOnClickListener(this);
        headerBackButton.setOnClickListener(this);

        //for implementing the swipe gestures on Main Activity
        gestureDetector = new GestureDetectorCompat(this, new OnSwipeListener(){
            @Override
            public boolean onSwipe(Direction direction) {
                if(direction == Direction.left){
                    gesPlaybackTransportControls.skipToNext();
                }

                if(direction == Direction.right){
                    gesPlaybackTransportControls.skipToPrevious();
                }
                /*if (direction==Direction.up){
                }
                if (direction==Direction.down){
                }*/
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e){
                if( gesMediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED ) {
                    gesPlaybackTransportControls.play();
                }
                else if( gesMediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING ) {
                    gesPlaybackTransportControls.pause();
                }
                //return true;
                return super.onDoubleTap(e);
            }
        });
        mainArtCover.setOnTouchListener(this);


        Log.d(MAIN_LOG, "exited onCreate");
    }
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(MAIN_LOG, "onStart entered");

        //connect mediaBrowser to service - it has to be started by this point
        /*if(!MediaPlaybackService.isServiceStarted){
            Log.e(MAIN_LOG, "onStart error: service is not yet started");
            return;
        }
        else{*/
            gesMediaBrowser = new MediaBrowserCompat(this,
                    new ComponentName(this, MediaPlaybackService.class),
                    mediaBrowserCallbacks, getIntent().getExtras());
            gesMediaBrowser.connect();
        //}

        Log.d(MAIN_LOG, "onStart exited");
    }
    @Override
    protected void onStop() {
        super.onStop();

        //disconnect the media browser from the service but keep playing in background
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
            case R.id.home_playPause:
                //has to be dealt with accordingly, based on the current state of mediaplayer
                if( gesMediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED ) {
                    gesPlaybackTransportControls.play();
                }
                else if( gesMediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING ) {
                        gesPlaybackTransportControls.pause();
                }
                break;

            case R.id.home_next:
                gesPlaybackTransportControls.skipToNext();
                break;

            case R.id.home_prev:
                gesPlaybackTransportControls.skipToPrevious();
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
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d(MAIN_LOG, "onTouch: ");
        gestureDetector.onTouchEvent(event);
        return true;
    }


    public static void ImageViewAnimatedChange(Context c, final ImageView v, final Bitmap new_image) {
        final Animation anim_out = AnimationUtils.loadAnimation(c, android.R.anim.fade_out);
        final Animation anim_in  = AnimationUtils.loadAnimation(c, android.R.anim.fade_in);
        anim_out.setAnimationListener(new Animation.AnimationListener()
        {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation)
            {
                v.setImageBitmap(new_image);
                anim_in.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override public void onAnimationRepeat(Animation animation) {}
                    @Override public void onAnimationEnd(Animation animation) {}
                });
                v.startAnimation(anim_in);
            }
        });
        v.startAnimation(anim_out);
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
