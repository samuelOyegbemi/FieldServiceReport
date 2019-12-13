package com.floromsolutions.fieldservicereport;

import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;

public class SplashScreenActivity extends FSActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int SPLASH_DELAY = 3000;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);

        new Handler().postDelayed(new Runnable(){

            @Override
            public void run() {
                // TODO Auto-generated method stub
                Intent homeIntent = new Intent(SplashScreenActivity.this, MainActivity.class);
                SplashScreenActivity.this.startActivity(homeIntent);
                SplashScreenActivity.this.finish();
            }
        }, SPLASH_DELAY);
    }
}
