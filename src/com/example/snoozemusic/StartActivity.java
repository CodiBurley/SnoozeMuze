package com.example.snoozemusic;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class StartActivity extends Activity {

    private RelativeLayout start_screen;
    private Button but;
    private GestureDetector swipe_detect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_screen);
        swipe_detect = new GestureDetector(new OnSwipeListen());
        start_screen = (RelativeLayout) findViewById(R.id.start_screen);
        but = (Button) findViewById(R.id.button);
        but.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return swipe_detect.onTouchEvent(motionEvent);
            }
        });
    }

    public void swipeLeft() {
        Intent intent = new Intent(this, MusicActivity.class);
        startActivity(intent);
    }

    public void swipeRight() {
        Intent intent = new Intent(this, MusicActivity.class);
        startActivity(intent);
    }

    private class OnSwipeListen extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_MIN = 50;
        private static final int SWIPE_MAX = 200;
        private static final int SWIPE_VELO_THRESHOLD = 200;


        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            float diff_y = Math.abs(e1.getY() - e2.getY());
            float diff_x = e1.getX() - e2.getX();

            if (diff_y > SWIPE_MAX) {
                return false;
            } else if (diff_x > SWIPE_MIN && Math.abs(velocityX) > SWIPE_VELO_THRESHOLD) {
                swipeRight();
                return true;
            } else if (-diff_x > SWIPE_MIN && Math.abs(velocityX) > SWIPE_VELO_THRESHOLD) {
                swipeLeft();
                return true;
            } else {
                Toast t = Toast.makeText(StartActivity.this, "Neither", Toast.LENGTH_LONG);
                t.show();
            }

            return false;
        }

    }
}

