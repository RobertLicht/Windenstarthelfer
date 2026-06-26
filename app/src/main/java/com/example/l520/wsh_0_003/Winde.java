package com.example.l520.wsh_0_003;

import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

public class Winde extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_winde);

        // Listen for clicks in action bar and control icon
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Hamburger icon

        // make fields in navigation drawer clickable
        NavigationView navigationView_winde = (NavigationView) findViewById(R.id.navigationView_winde);
        navigationView_winde.setNavigationItemSelectedListener(this);

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

            case R.id.nav_menu_winde_bearbeiten:
                Toast.makeText(Winde.this,"Option nicht verfügbar", Toast.LENGTH_SHORT).show();
                break;
        }

        switch (item.getItemId()) {

            case R.id.nav_menu_winde_verbindung:
                Toast.makeText(Winde.this,"Option nicht verfügbar", Toast.LENGTH_SHORT).show();
                break;
        }

        switch (item.getItemId()) {

            case R.id.nav_menu_winde_einstellungen:
                Toast.makeText(Winde.this,"Option nicht verfügbar", Toast.LENGTH_SHORT).show();
                break;
        }

        // hide navigation view drawer
        DrawerLayout dl_winde = (DrawerLayout) findViewById(R.id.drawerLayout);
        if (dl_winde.isDrawerOpen(GravityCompat.START)) {
            dl_winde.closeDrawer(GravityCompat.START);
        }

        return false;
    }
}
