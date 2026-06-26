package com.example.l520.wsh_0_003;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

public class SplashScreenActivity extends AppCompatActivity {

    private static int SPLASH_TIME_OUT = 3000; // OneShotTimer [ms]

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        Animation anim = AnimationUtils.loadAnimation(this, R.anim.move_up);
        ImageView imgSplshScreen = (ImageView) findViewById(R.id.imgSplshScreen);
        imgSplshScreen.setAnimation(anim);


        Handler hander = new Handler();
        hander.postDelayed(new Runnable(){
            @Override
            public void run(){

                startActivity(new Intent(SplashScreenActivity.this, MainActivity.class));
                finish(); //no restart after user pushes back-button

            }
        },SPLASH_TIME_OUT);

    }
}
