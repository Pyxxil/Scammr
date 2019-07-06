package com.example.scamcam;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Report extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNav);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_home:
                    homePage();
                    break;
                case R.id.action_report:
                    break;
                case R.id.action_settings:
                    settingsPage();
                    break;
            }
            return true;
        });
    }

    void homePage() {
        Intent home = new Intent(this, MainActivity.class);
        startActivity(home);
    }

    void settingsPage() {
        Intent setting = new Intent(this, Settings.class);
        startActivity(setting);
    }
}