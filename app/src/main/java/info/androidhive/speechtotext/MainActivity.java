package info.androidhive.speechtotext;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class MainActivity extends Activity {

    private TextView speechInput;
    private TextView translatedText;
    private ImageButton btnSpeak;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private TextToSpeech t1;
    private String restUrl = "TRANSLATE API";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        speechInput = (TextView) findViewById(R.id.speechInput);
        translatedText = (TextView) findViewById(R.id.translatedText);
        btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);

        // hide the action bar
        getActionBar().hide();

        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.ENGLISH);
                }
            }
        });

        btnSpeak.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });

    }

    public static String POST(String url, String t) {
        InputStream inputStream = null;
        String translate = "";
        try {

            URL u = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) u.openConnection();
            connection.setRequestMethod("POST");
            String json = "";

            // 3. build jsonObject
            JSONObject jsonObject = new JSONObject();
            jsonObject.accumulate("text", t);

            // 4. convert JSONObject to JSON to String
            json = jsonObject.toString();
            System.out.println("JSON: " + json);
            try {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-type", "application/json");

                OutputStream os = connection.getOutputStream();
                OutputStreamWriter wr = new OutputStreamWriter(os);
                wr.write(json);
                wr.flush();

                InputStream in = new BufferedInputStream(connection.getInputStream());

                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
                String line = null;
                StringBuilder sb = new StringBuilder(256);
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();
                JSONObject jo = new JSONObject(sb.toString());
                translate = (String) jo.get("text");
                System.out.println("" + sb.toString());
            } finally {
                connection.disconnect();
            }


        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return translate;
    }


    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        WeakReference<Activity> mWeakActivity;

        public HttpAsyncTask(Activity activity) {
            mWeakActivity = new WeakReference<Activity>(activity);
        }

        @Override
        protected String doInBackground(String... params) {

            return POST(params[0], params[1]);
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getBaseContext(), "Data Sent!", Toast.LENGTH_LONG).show();
            Activity activity = mWeakActivity.get();
            System.out.println("result: " + result);
            if (activity != null) {
                t1.speak(result, TextToSpeech.QUEUE_FLUSH, null);
                translatedText.setText(result);
            }
        }
    }

    /**
     * Showing google speech input dialog
     */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Receiving speech input
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String toSpeak = result.get(0);
                    speechInput.setText(result.get(0));
                    new HttpAsyncTask(this).execute(restUrl, toSpeak);
                }
                break;
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}
