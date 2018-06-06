package com.aware.app.myoubiss18;

import android.content.ContentValues;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

public class DataCollection extends AppCompatActivity {
private Spinner genderSpinner;
private Button startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_collection);
        genderSpinner  = (Spinner) findViewById(R.id.gender);
// Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.gender, android.R.layout.simple_spinner_item);
// Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
        genderSpinner.setAdapter(adapter);
        startButton= findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JsonObject data=new JsonObject();

               data.addProperty ("participant_id",  ((EditText) findViewById(R.id.participant_id)).getText().toString());
               data.addProperty("trial",  ((EditText) findViewById(R.id.trial)).getText().toString());
               String gender=((Spinner) findViewById(R.id.gender)).getSelectedItem().toString();
              // if (gender.equals("Male") ) {gender="1";} else {gender="2";}
               data.addProperty("gender", gender );
               data.addProperty("weight",  ((EditText) findViewById(R.id.weight)).getText().toString());
                data.addProperty("drink_start_time",  ((EditText) findViewById(R.id.drink_start_time)).getText().toString());
                data.addProperty("number_of_bottles",  ((EditText) findViewById(R.id.no_of_drinks)).getText().toString());
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(System.currentTimeMillis());
                data.addProperty("trial_time", cal.get(Calendar.HOUR_OF_DAY));
                data.addProperty("trial_start_time",System.currentTimeMillis() );

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);

                intent.putExtra("EXTRA_DATA", data.toString());
                startActivity(intent);

            }
        });
    }






    }



