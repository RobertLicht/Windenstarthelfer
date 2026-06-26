package com.example.l520.wsh_0_003;


import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;



public class Flugzeug extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    ActionBarDrawerToggle toggle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flugzeug);

        // Listen for clicks in action bar and control icon
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Hamburger icon

        // make fields in navigation drawer clickable
        NavigationView navigationView_flugzeug = (NavigationView) findViewById(R.id.navigationView_flugzeug);
        navigationView_flugzeug.setNavigationItemSelectedListener(this);

    }


    // capture clicks on the icon
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (toggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    // capture clicks on the navigation drawer
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {

            case R.id.nav_menu_flugzeug_bearbeiten:
                Toast.makeText(Flugzeug.this, "Option nicht verfügbar", Toast.LENGTH_LONG).show();
                break;
        }

        switch (item.getItemId()) {

            case R.id.nav_menu_flugzeug_verbindung:
                Toast.makeText(Flugzeug.this, "Option nicht verfügbar", Toast.LENGTH_LONG).show();
                break;
        }

        switch (item.getItemId()){

            case R.id.nav_menu_flugzeug_location:
                //Toast.makeText(Flugzeug.this, "Option bald verfügbar", Toast.LENGTH_SHORT).show();
                Intent act_location = new Intent(Flugzeug.this,LocationActivity.class);
                startActivity(act_location);
                break;
        }

        switch (item.getItemId()){

            case R.id.nav_menu_flugzeug_sensoren:
                //Toast.makeText(Flugzeug.this, "Sensoren", Toast.LENGTH_SHORT).show();
                Intent act_sensors = new Intent(Flugzeug.this,SensorActivity.class);
                startActivity(act_sensors);
                break;
        }

        switch (item.getItemId()) {

            case R.id.nav_menu_flugzeug_einstellungen:
                Toast.makeText(Flugzeug.this, "Option nicht verfügbar", Toast.LENGTH_LONG).show();
                break;
        }

        // hide navigation view drawer
        DrawerLayout dl_flugzeug = (DrawerLayout) findViewById(R.id.drawerLayout);
        if (dl_flugzeug.isDrawerOpen(GravityCompat.START)) {
            dl_flugzeug.closeDrawer(GravityCompat.START);
        }

        return false;
    }

}

