package com.ecebuc.gesmediaplayer;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

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

        Button bubububu = (Button) findViewById(R.id.button2);
        bubububu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Test test", Snackbar.LENGTH_SHORT)
                        .show();
            }
        });

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
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
        int id = item.getItemId();

        switch(id){
            case R.id.nav_playing_now:
                break;
            case R.id.nav_all_songs:
                break;
            case R.id.nav_artists:
                break;
            case R.id.nav_albums:
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
}
