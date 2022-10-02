package com.example.museum;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.Toolbar;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    Button camera,Settings,home;
    private Spinner sp;
    private SwitchCompat switchCompat;
     int check = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

     sp = findViewById(R.id.languages);
     switchCompat = findViewById(R.id.readme);


     switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                     check = 1;
                } else {

                }
            }
        });




     sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
         @Override
         public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
           int s = sp.getSelectedItemPosition();
           String l=lang(s);

         }

         @Override
         public void onNothingSelected(AdapterView<?> parent) {

         }
     });

        int ind = sp.getSelectedItemPosition();
        String lan = lang(ind);
        Locale locale = new Locale(lan);
        Locale.setDefault(locale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
    }

    @Override
    public void onBackPressed()
    {
        // code here to show dialog
        super.onBackPressed();  // optional depending on your needs
        int ind = sp.getSelectedItemPosition();
        String lan = lang(ind);
        if(lan.equals("Arabic")){
            Intent intent = new Intent(getApplicationContext(),ArabicMainActivity.class);
            intent.putExtra("checked",check);
            startActivity(intent);
        }
        else{
            Intent intent =new Intent(getApplicationContext(),MainActivity.class);
            intent.putExtra("checked",check);
            startActivity(intent);
        }
    }

    public void Home(View view) {

        home = findViewById(R.id.home);
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int ind = sp.getSelectedItemPosition();
                String lan = lang(ind);
//                Toast.makeText(getApplicationContext(),lan,Toast.LENGTH_LONG).show();
              if(lan.equals("Arabic")){
                  Intent intent = new Intent(getApplicationContext(),ArabicMainActivity.class);
                  intent.putExtra("checked",check);
                  startActivity(intent);
              }
              else{
                  Intent intent =new Intent(getApplicationContext(),MainActivity.class);
                  intent.putExtra("checked",check);
                  startActivity(intent);
                }
            }
        });
    }

    public String lang(int ind){
        if (ind == 1){
            return "Arabic";
        }
       return "english";
    }
}