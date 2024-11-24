package com.example.laba4;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String API_KEY = "dcbb690643d50831f735d15336758d3b";
    private static final String CITY_NAME = "Voronezh";
    private static final int POLLING_INTERVAL = 20000; // 60 seconds
    private Handler handler;
    private TextView weatherTextView;
    private TextView statusTextView;
    private DatabaseHelper databaseHelper; // Переменная для базы данных

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler(Looper.getMainLooper());
        weatherTextView = findViewById(R.id.weatherTextView);
        statusTextView = findViewById(R.id.statusTextView);
        databaseHelper = new DatabaseHelper(this); // Инициализация базы данных

        Button refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(v -> fetchWeatherData());

        Button navigateButton = findViewById(R.id.navigateButton);
        navigateButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SongListActivity.class);
            startActivity(intent);
        });

        if (!isNetworkAvailable()) {
            statusTextView.setText("Нет подключения к интернету. Автономный режим.");
            Toast.makeText(this, "Нет подключения к интернету. Автономный режим.",
                    Toast.LENGTH_LONG).show();
        } else {
            statusTextView.setText("Подключено к интернету");
            fetchWeatherData(); // Получаем данные о погоде сразу
            startPolling(); // Начинаем опрос для обновлений
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void startPolling() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isNetworkAvailable()) {
                    fetchWeatherData(); // Периодически получаем данные о погоде
                }
                handler.postDelayed(this, POLLING_INTERVAL);
            }
        }, 0); // Начинаем немедленно
    }

    private void fetchWeatherData() {
        if (isNetworkAvailable()) {
            new FetchWeatherTask().execute();
        } else {
            statusTextView.setText("Нет подключения к интернету.");
        }
    }

    private class FetchWeatherTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            OkHttpClient client = new OkHttpClient();
            String url = "https://api.openweathermap.org/data/2.5/weather?q=" + CITY_NAME + "&appid=" + API_KEY + "&units=metric";

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    return response.body().string();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String jsonData) {
            if (jsonData != null) {
                try {
                    JSONObject json = new JSONObject(jsonData);
                    String weatherDescription = json.getJSONArray("weather").getJSONObject(0).getString("description");
                    double temperature = json.getJSONObject("main").getDouble("temp");
                    weatherTextView.setText("Погода в Воронеже: " + weatherDescription + ", Температура: " + temperature + "°C");

                    // Сохраняем данные о погоде в базе данных
                    databaseHelper.addWeather(CITY_NAME, temperature, weatherDescription);
                } catch (JSONException e) {
                    e.printStackTrace();
                    weatherTextView.setText("Ошибка получения данных");
                }
            } else {
                weatherTextView.setText("Не удалось получить данные");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        databaseHelper.close(); // Закрываем базу данных при уничтожении активности
    }
}