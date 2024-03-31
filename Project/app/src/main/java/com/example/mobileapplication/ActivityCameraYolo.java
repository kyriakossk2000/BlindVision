package com.example.mobileapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class ActivityCameraYolo extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private CameraBridgeViewBase openCVcamera;
    private Bundle audio;
    private List<String> classLabels = new ArrayList<>();
    private InputStream labels;
    private final String MODELTAG = "ModelLoad";
    private final String FPS = "FPS";
    private boolean audioOn;
    private SceneUnderstandingYolo sceneUnderstanding;
    private List<String> listOfWords;
    private ModelLoaderYolo modelLoaderYolo;
    public Set<String> uniqueObjects = new HashSet<String>();
    private long time = System.currentTimeMillis();
    private int fpsCounter;
    public List<Double> accuracyList = new ArrayList<>();
    private double timePassed;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        listOfWords = new ArrayList<>();


        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String joined = "";
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    joined = String.join(",", uniqueObjects);
                }
                // checking if audio should be fed to user
                if (audioOn) {
                    new SpeechSynthesis(ActivityCameraYolo.this, joined);
                }
                listOfWords.clear();
                uniqueObjects.clear();
                calcMeanAP();
            }
        };
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(runnable, 0, 3500, TimeUnit.MILLISECONDS);

        // loads YOLO model to the app and saves labels in a list
        loadModel();

        // indicates if audio Feedback will be enable
        audioFeedbackCheck();

        openCVcamera = findViewById(R.id.camera_preview);
        openCVcamera.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
        openCVcamera.setCvCameraViewListener(this);
        openCVcamera.enableFpsMeter();   // Log -> FPSMETER -> AVG 4.75FPS

        Button button_back = findViewById(R.id.button_home);
        button_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onDestroy();
                openCVcamera.disableView();
                finish();
                activityChange(button_back);
            }
        });
    }

    // yolov3 tiny and yolov2 tiny models taken from:
    // https://pjreddie.com/darknet/yolo/
    // yolov4 tiny model taken from
    // https://github.com/kiyoshiiriemon/yolov4_darknet
    // dataset used to train the model: COCO dataset
    private void loadModel(){
        try {

            modelLoaderYolo = new ModelLoaderYolo(this);

            labels = this.getAssets().open("labelsYolo.txt");

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(labels));
            String label;

            while ((label = bufferedReader.readLine()) != null) {
                classLabels.add(label);   //read label file and save each label in a list
            }
            bufferedReader.close();

            Log.d(MODELTAG, "Model Loaded Successfully");

        }
        catch (IOException e) {
            Log.d(MODELTAG, "Model Not Loaded");
            e.printStackTrace();
        }
    }



    // indicates if audio Feedback will be enable
    private void audioFeedbackCheck(){
        audio = getIntent().getExtras();
        if (audio!=null) {
            audioOn = audio.getBoolean("audio");
            Log.d(MODELTAG, ""+ audioOn);
        }
    }

    private void activityChange(View view){
        Intent intent = new Intent(ActivityCameraYolo.this,MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        // calculating fps
        if (System.currentTimeMillis() - time > 1000){
            time = System.currentTimeMillis();
            Log.d(FPS, ""+fpsCounter);
            fpsCounter = 0;
        }
        sceneUnderstanding = new SceneUnderstandingYolo(ActivityCameraYolo.this, modelLoaderYolo.net,classLabels, inputFrame.rgba(), audioOn,listOfWords);
        fpsCounter++;
        return sceneUnderstanding.imageScene();
    }

    // calculating average accuracy of model
    private void calcMeanAP(){
        timePassed += 3.5;
        double mean;
        double sum = 0;
        for (double i : accuracyList){
            sum += i;
        }
        mean = sum / accuracyList.size();
        Log.e("mAP2", "Mean Accuracy: "+mean + " Objects detected: "+accuracyList.size() + " Time passed" + timePassed);
    }
    // testing results --> Mean Accuracy: 0.68, Objects detected: 1113, Time passed: 4.5 minutes
}
