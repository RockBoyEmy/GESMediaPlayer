package com.ecebuc.gesmediaplayer;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        //setting up the fragments stuff
        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.home_fragment_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Create a new Fragment to be placed in the activity layout
            SongsFragment firstFragment = new SongsFragment();

            // In case this activity was started with special instructions from an
            // Intent, pass the Intent's extras to the fragment as arguments
            //firstFragment.setArguments(getIntent().getExtras());

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.home_fragment_container, firstFragment).commit();
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
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.nav_drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

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
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_playing_now:
                break;
            case R.id.nav_all_songs:
                // Create the songs fragment
                SongsFragment songsFragment = new SongsFragment();
                fragmentTransaction = getSupportFragmentManager().beginTransaction();

                // Replace whatever is in the fragment_container view with this fragment,
                // and add the transaction to the back stack so the user can navigate back
                fragmentTransaction.replace(R.id.home_fragment_container, songsFragment);
                fragmentTransaction.addToBackStack(null);

                // Commit the transaction
                fragmentTransaction.commit();
                break;

            case R.id.nav_artists:
                ArtistsFragment artistsFragment = new ArtistsFragment();
                fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.home_fragment_container, artistsFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                break;
            case R.id.nav_albums:
                AlbumsFragment albumsFragment = new AlbumsFragment();
                fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.home_fragment_container, albumsFragment);
                fragmentTransaction.addToBackStack(null);
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

    @Override
    public void updateToolbarTitleForFragment(String fragmentTitle) {
        try {
            getSupportActionBar().setTitle(fragmentTitle);
        } catch (Exception e){
            e.printStackTrace();
            Log.d("updateToolbarSong: ", "setTitle: " + e);
        }
    }
}
