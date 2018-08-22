package com.xxun.xunlauncher.utils;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.xxun.xunlauncher.R;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void pay(View view) {
        PayDialogFragment payDialogFragment = new PayDialogFragment();
        payDialogFragment.show(getSupportFragmentManager(), "payFragment");
    }
}
