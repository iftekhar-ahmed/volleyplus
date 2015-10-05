package com.iftekhar.volleyplusdemo;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fm = getSupportFragmentManager();
        ClothListFragment fragment = ClothListFragment.findOrGetInstance(fm);
        fm.beginTransaction().replace(R.id.fragment_container, fragment, ClothListFragment.TAG).commit();
    }
}
