package com.example.mobileapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity{

    private TextToSpeech welcomeMessage;
    private SpeechRecognizer speech;
    private Switch aSwitch;
    private boolean speechRec = false;
    private int modeOfDetection;
    // DETECTION_MODELS -> SSD MOBILENET MODEL, YOLO MODEL

    static {
        //checks if openCV has been loaded successfully --> report shown at Logcat
        String TAG = "OPENCV";
        if(OpenCVLoader.initDebug()){
            Log.d(TAG,"OpenCV loaded successfully");
        }else{
            Log.d(TAG,"OpenCV was not loaded");
        }
    }
    public String getAndroidVersion() {
        String release = Build.VERSION.RELEASE;
        int sdkVersion = Build.VERSION.SDK_INT;
        return "Android SDK: " + sdkVersion + " (" + release +")";
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("Version",getAndroidVersion());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ask to allow permission for the camera and microphone
        // help to implement permissions --> https://stackoverflow.com/questions/34342816/android-6-0-multiple-permissions
        String[] permissions = new String[2];
        permissions[0] = Manifest.permission.CAMERA;
        permissions[1] = Manifest.permission.RECORD_AUDIO;

        if(!getPermissions(this,permissions)){
            ActivityCompat.requestPermissions(this, permissions,0);
        }

        // welcome text-to-speech message
        ttsWelcome();

        Button button_default = findViewById(R.id.button_default);
        button_default.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                welcomeMessage.stop();
                if (speechRec) {
                    speech.destroy();
                }
                activityChangeDefault(button_default);

            }
        });
        Button button_object = findViewById(R.id.button_object);
        button_object.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                welcomeMessage.stop();
                if (speechRec) {
                    speech.destroy();
                }
                activityChangeObject(button_object);

            }
        });
        Button button_text = findViewById(R.id.button_text);
        button_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                welcomeMessage.stop();
                if (speechRec) {
                    speech.destroy();
                }
                activityChangeText(button_text);
            }
        });
    }

    private boolean getPermissions(Context context, String[] permissions) {
        if (context != null && permissions != null) {
            for(int i=0; i<permissions.length; i++){
                if (ActivityCompat.checkSelfPermission(context, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    // move to camera activity --> both text and object detection enable - Mode 1
    private void activityChangeDefault(View view){
        Intent intent;
        intent = new Intent(MainActivity.this, ActivityCamera.class);

        String key = "audio";
        aSwitch = findViewById(R.id.audioFeedback);

        intent.putExtra(key, aSwitch.isChecked());  //passing a boolean to enable or not the audio

        String key_Mode = "detection_mode";
        modeOfDetection = 1;
        intent.putExtra(key_Mode,modeOfDetection);

        startActivity(intent);
    }

    // only object detection enable - Mode 2
    private void activityChangeObject(View view){
        Intent intent;
        intent = new Intent(MainActivity.this, ActivityCamera.class);

        String key = "audio";
        aSwitch = findViewById(R.id.audioFeedback);

        intent.putExtra(key, aSwitch.isChecked());  //passing a boolean to enable or not the audio

        String key_Mode = "detection_mode";
        modeOfDetection = 2;
        intent.putExtra(key_Mode,modeOfDetection);

        startActivity(intent);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        welcomeMessage.stop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        ttsWelcome();
    }

    // only text detection enable - Mode 1
    private void activityChangeText(View view){
        Intent intent;
        intent = new Intent(MainActivity.this, ActivityCamera.class);

        String key = "audio";
        aSwitch = findViewById(R.id.audioFeedback);

        intent.putExtra(key, aSwitch.isChecked());  //passing a boolean to enable or not the audio

        String key_Mode = "detection_mode";
        modeOfDetection = 3;
        intent.putExtra(key_Mode,modeOfDetection);

        startActivity(intent);
    }

    /* Yolo not used anymore
    private void activityChangeYolo(View view){
        Intent intent;
        intent = new Intent(MainActivity.this, ActivityCameraYolo.class);

        String key = "audio";
        aSwitch = findViewById(R.id.audioFeedback);

        intent.putExtra(key, aSwitch.isChecked());  //passing a boolean to enable or not the audio
        startActivity(intent);
    }*/

    private void ttsWelcome(){
        String TAG = "TTS";
        welcomeMessage = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i == TextToSpeech.SUCCESS) {
                    welcomeMessage.setLanguage(Locale.ENGLISH);
                    welcomeAudio();

                } else {
                    Log.e(TAG, "TTS was not loaded");
                }
            }
        });
    }

    // welcome audio feedback
    private void welcomeAudio() {
        HashMap<String,String> map = new HashMap<>();
        String message = "Say 1 to choose Default mode, 2 to choose Scene Understanding mode, or 3 to choose Text mode. Shake Phone to return to Home Page.";
        float pitch = 1.0f;
        float speed = 1.3f;
        welcomeMessage.setPitch(pitch);
        welcomeMessage.setSpeechRate(speed);
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "SR-START");
        welcomeMessage.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
            }
            // when welcome start message stops call speech listener
            // help for utterance progress listener found on https://stackoverflow.com/questions/24956784/speech-recognition-intent-starts-before-tts-ends-speaking
            @Override
            public void onDone(String s) {

                Handler handler = new Handler(getApplicationContext().getMainLooper());

                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        speechRec = true;
                        speechListener(); //calling speech listener method
                    }
                };
                handler.post(runnable);
            }

            @Override
            public void onError(String s) {

            }
        });
        welcomeMessage.speak(message,TextToSpeech.QUEUE_FLUSH,map);

    }

    // Speech listener method
    private void speechListener(){
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speech.startListening(speechIntent);

        speech.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onResults(Bundle bundle) {
                StringBuilder message = new StringBuilder();
                ArrayList<String> sentence = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                for (int i = 0; i < sentence.size(); i++){
                    message.append(sentence.get(i));
                }
                Log.d("TTS", message.toString());
                String[] split = message.toString().split("\\s+");
                for (String word : split) {
                    if (word.equals("one") || word.equals("1")) {
                        speech.stopListening();
                        activityChangeDefault(findViewById(R.id.button_default));
                    }
                    else if (word.equals("two") || word.equals("too") || word.equals("2")){  // it might confuse too with two
                        speech.stopListening();
                        activityChangeObject(findViewById(R.id.button_object));
                    }
                    else if (word.equals("three") || word.equals("3")){
                        speech.stopListening();
                        activityChangeText(findViewById(R.id.button_text));
                    }
                    else{
                        speechListener();
                    }
                }
            }
            @Override
            public void onReadyForSpeech(Bundle bundle) {
            }
            @Override
            public void onBeginningOfSpeech() {
            }
            @Override
            public void onRmsChanged(float v) {
            }
            @Override
            public void onBufferReceived(byte[] bytes) {
            }
            @Override
            public void onEndOfSpeech() {
            }
            @Override
            public void onError(int i) {
            }
            @Override
            public void onPartialResults(Bundle bundle) {
            }
            @Override
            public void onEvent(int i, Bundle bundle) {
            }
        });

    }

}