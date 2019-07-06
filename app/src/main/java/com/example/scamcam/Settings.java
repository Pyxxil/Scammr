package com.example.scamcam;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Settings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNav);
        bottomNavigationView.setOnNavigationItemSelectedListener(item1 -> {
            switch (item1.getItemId()) {
                case R.id.action_home:
                    homePage();
                    break;
                case R.id.action_report:
                    reportPage();
                    break;
                case R.id.action_settings:
                    break;
            }
            return true;
        });
    }

    void homePage() {
        Intent home = new Intent(this, MainActivity.class);
        startActivity(home);
    }

    void reportPage() {
        Intent report = new Intent(this, Report.class);
        startActivity(report);
    }
}