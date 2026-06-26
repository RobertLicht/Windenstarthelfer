package com.example.l520.wsh_0_003;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import android.os.Handler;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    // Create output in Logfile in the terminal to follow action
    //final String TAG = this.getClass().getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // implement ImageButton objects listen for clicks
        ImageButton Flugzeug = (ImageButton) findViewById(R.id.imageButton_Flugzeug);
        Flugzeug.setOnClickListener(this);

        ImageButton Winde = (ImageButton) findViewById(R.id.imageButton_Winde);
        Winde.setOnClickListener(this);

        // Create toolbar and setup
        Toolbar wsh_toolbar = (Toolbar) findViewById(R.id.wsh_toolbar);
        setSupportActionBar(wsh_toolbar);

        getSupportActionBar().setTitle(R.string.wsh_tb_title);
        //getSupportActionBar().setSubtitle(R.string.wsh_tb_subtitle);
        getSupportActionBar().setIcon(R.drawable.ic_wsh_tower);
    }


    // add menu for the toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu,menu);

        return super.onCreateOptionsMenu(menu);
    }


    // action when entry of the menu is clicked
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            // case ueber
            case R.id.menu_1:
                //Toast.makeText(MainActivity.this,"Option nicht verfügbar" ,Toast.LENGTH_SHORT).show();
                Intent act_about = new Intent(MainActivity.this,AboutActivity.class);
                startActivity(act_about);
                break;
        }

        switch (item.getItemId()) {

            case R.id.menu_2:
                Toast.makeText(MainActivity.this,"Option nicht verfügbar" ,Toast.LENGTH_SHORT).show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void FlugzeugClicked(){
        //Toast.makeText(MainActivity.this, "Schaltfläche Flugzeug", Toast.LENGTH_SHORT).show();

        // wire up the imagebutton
        Intent act_flugzeug = new Intent(MainActivity.this,Flugzeug.class);
        startActivity(act_flugzeug);
    }

    public void WindeClicked(){
        //Toast.makeText(MainActivity.this, "Schaltfläche Winde", Toast.LENGTH_SHORT).show();

        // wire up the imagebutton and do intent
        Intent act_winde = new Intent(MainActivity.this,Winde.class);
        startActivity(act_winde);
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()){

            case R.id.imageButton_Flugzeug:
                    FlugzeugClicked();
                break;
            case R.id.imageButton_Winde:
                    WindeClicked();
                break;
        }

    }

    // Here the tap twice is done
    // Initialize variable twice
    boolean twice = false;
    @Override
    public void onBackPressed() {

        //Log.d(TAG, "click");

        // code to excit from the App, if value of twice is true
        if(twice == true){
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            System.exit(0);
        }
        twice = true;
        //Log.d(TAG, "twice: " + twice);

        //super.onBackPressed(); //handle response on the back button
        Toast.makeText(MainActivity.this, "Nochmal tippen um die App zu beenden",Toast.LENGTH_SHORT).show();
        // turn twice to false after time defined at the end
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                twice = false;
                //Log.d(TAG, "twice: " + twice);
            }
        },2500);// wait [ms]
    }

}
