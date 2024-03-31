package com.example.mobileapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.RequiresApi;

import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ActivityCamera extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private CameraBridgeViewBase openCVcamera;
    private Bundle home_Data;
    private ModelLoader model;
    private AssetFileDescriptor modelFile;
    public TextToSpeech textToSpeech;
    private final List<String> classLabels = new ArrayList<>();
    private InputStream labels;
    private final String MODELTAG = "ModelLoad";
    private final String FPS = "FPS";
    public boolean audioOn;
    public int modeOfDetection; // used to indicate mode of detection
    private SceneUnderstanding sceneUnderstanding;
    public List<String> listOfWords = new ArrayList<>();
    public List<String> listOfText = new ArrayList<>();
    public Set<String> uniqueObjects = new HashSet<String>();
    private int fpsCounter;
    private long time = System.currentTimeMillis();
    public List<Float> accuracyList = new ArrayList<>();
    private double timePassed;
    private TextRecognizer textRecognizer;
    public boolean recognize_text = true;
    public boolean recognize_objects = true;
    public boolean recognize_image = true;
    public boolean audioFeedEnded = true;
    public HashMap<String,String> map = new HashMap<>();
    private float accelerate, accelerateEnd, shaking;
    private static int countShaking;
    public int frameCounter = 0;
    public Map<Integer, Object> draw_output = new TreeMap<>();

    //Callback to allow OpenCV Java calls
    //code source: https://docs.nvidia.com/gameworks/content/technologies/mobile/opencv_tutorial_camera_preview.htm
    private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            String TAG = "OpenCV_CallBack";
            if(status == LoaderCallbackInterface.SUCCESS){
                Log.i(TAG,"OpenCV loaded successfully");
                openCVcamera.enableView();
            }else{
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    public void onResume(){
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION,this,loaderCallback);
    }
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        textToSpeech.stop();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        // determines the mode of detection
        modeOfDetection();
        // indicates if audio Feedback will be enable
        audioFeedbackCheck();
        // initializing text-to-speech
        ttsAudio();
        // activating shaking sensor
        activateShakingSensor();


        Runnable runnable = () -> {
            Log.e("Talking", "-->" + audioFeedEnded);
            // checks if audio feedback is ended so it can recognize again --> recognizing things also every 2 second
            if (audioFeedEnded) {
                recognize_text = true;
                recognize_objects = true;
                recognize_image = true;
                StringBuilder joinedOCR = new StringBuilder();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    int count = 0;
                    // saving up to 4 blocks of text
                    for (String i : listOfText) {
                        if (count < 4) {
                            joinedOCR.append(i);
                            count++;
                            continue;
                        }
                        break;
                    }
                    Log.e("OCRAudio", "" + joinedOCR);
                }

                // initializing Speech Synthesis
                new SpeechSynthesis(ActivityCamera.this, uniqueObjects, joinedOCR);

                listOfWords.clear();
                uniqueObjects.clear();
                listOfText.clear();
                draw_output.clear();
                frameCounter = 0;
                calcMeanAP();
            }
        };
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(runnable, 0, 2000, TimeUnit.MILLISECONDS);

        // loads TLite model to the app and saves labels in a list
        loadModel();

        // optical character recognition model using ML Kit API by Google
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS); // creating an instance of TextRecognizer

        openCVcamera = findViewById(R.id.camera_preview);
        openCVcamera.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
        openCVcamera.setCvCameraViewListener(this);
        openCVcamera.enableFpsMeter();

        Button button_back = findViewById(R.id.button_home);
        button_back.setOnClickListener(view -> {
            onDestroy();
            openCVcamera.disableView();
            finish();
            activityChange();
        });
    }


    // method to initialize text to speech
    private void ttsAudio(){
        if (textToSpeech==null){
            textToSpeech = new TextToSpeech(this, i -> {
                if (i == TextToSpeech.SUCCESS) {
                    if (textToSpeech!=null) {
                        textToSpeech.setLanguage(Locale.ENGLISH);
                    }
                } else {
                    Log.e("TTS", "TTS was not loaded");
                }
            });
        }
    }

    // load TLite model to the app and adds labels in a list
    // MobileNetV1 model and dataset from https://github.com/tensorflow/examples/tree/master/lite/examples/object_detection/android
    // dataset used to train the model: COCO dataset
    // EfficientDetLite0 model and dataset from https://tfhub.dev/tensorflow/lite-model/efficientdet/lite0/detection/default/1
    // dataset used to train the model: COCO dataset
    private void loadModel(){
        try {
            modelFile = this.getAssets().openFd("efficientdet_lite0.tflite");
            labels = this.getAssets().open("classes.txt");

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(labels));
            String label;

            while ((label = bufferedReader.readLine()) != null) {
                classLabels.add(label);   //read label file and save each label in a list
            }
            bufferedReader.close();

            model = new ModelLoader(modelFile);
            Log.d(MODELTAG, "Model Loaded Successfully");

        }
        catch (IOException e) {
            Log.d(MODELTAG, "Model Not Loaded");
            e.printStackTrace();
        }
    }
    // determines the mode of detection
    private void modeOfDetection(){
        home_Data = getIntent().getExtras();
        if (home_Data !=null) {
            modeOfDetection = home_Data.getInt("detection_mode");
            Log.d("ModeDetection", ""+ modeOfDetection);
        }
    }

    // indicates if audio Feedback will be enable
    private void audioFeedbackCheck(){
        home_Data = getIntent().getExtras();
        if (home_Data !=null) {
            audioOn = home_Data.getBoolean("audio");
            Log.d("AudioOn", ""+ audioOn);
        }
    }

    private void activityChange(){
        // stopping audio feedback
        if(textToSpeech!=null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        Intent intent = new Intent(ActivityCamera.this,MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onCameraViewStarted ( int width, int height){
    }

    @Override
    public void onCameraViewStopped () {
    }

    @Override
    public Mat onCameraFrame (CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        // calculating fps manually
        if (System.currentTimeMillis() - time > 1000) {
            time = System.currentTimeMillis();
            Log.e(FPS, "" + fpsCounter);  // showing frames per second
            fpsCounter = 0;
        }
        fpsCounter++;
        frameCounter++;  // counting frames
        sceneUnderstanding = new SceneUnderstanding(ActivityCamera.this, model.modelInterpreter, classLabels, inputFrame.rgba(), audioOn, listOfWords, textRecognizer, listOfText);
        return sceneUnderstanding.imageScene();
    }
    // activating shaking sensor to go back to home screen
    // Reference for shaking sensor: Code from https://stackoverflow.com/questions/59906444/how-to-detect-shake-and-toast-a-message-after-shaking-the-phone-3-times-in-andro
    private void activateShakingSensor() {
        final SensorEventListener shakingListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];
                accelerate = accelerateEnd;
                accelerate = (float) Math.sqrt((double)((x*x) + (y*y) + (z*z)));
                float deference = accelerate - accelerateEnd;
                shaking = shaking*0.9f + deference;
                if (shaking>60){
                    countShaking++;
                }
                if (countShaking >= 20){
                    countShaking = 0;
                    onDestroy();
                    openCVcamera.disableView();
                    finish();

                    activityChange();
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(shakingListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
        accelerate = SensorManager.GRAVITY_EARTH;
        accelerateEnd = SensorManager.GRAVITY_EARTH;
        shaking = 0.00f;
    }


    // calculating average accuracy of model
    private void calcMeanAP () {
        timePassed += 3.5;
        double mean;
        double sum = 0;
    /*for (double i : accuracyList){
        sum += i;
    }*/
        // mean = sum / accuracyList.size();
        // Log.e("mAP1", "Mean Accuracy: "+mean + " Objects detected: "+accuracyList.size() + " Time passed" + timePassed);
    }

}
