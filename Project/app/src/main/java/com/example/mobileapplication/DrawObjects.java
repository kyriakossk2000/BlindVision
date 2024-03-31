package com.example.mobileapplication;

// source of code for drawing rectangles when a detection is made:
// https://github.com/pramod722445/Object_Detection_App
// https://www.youtube.com/watch?v=2VitVfsqvso&list=PL0aoTDj9NwggFB0oZx_nwZXchnWQKsxcp&index=7

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class DrawObjects{
    private Map<Integer,Object> output;
    private final int height;
    private final int width;
    private final Mat imageMatrix;
    private final List<String> classLabels;
    private final double THRESHOLD = 0.6;
    private final boolean audioOn;
    private final int OBJECTS_TO_DETECT = 25;  // recognizing up to 10 objects for MobileNet / up to 25 objects for EfficientDet
    private final ActivityCamera activityCamera;
    private final List<String> listOfWords;

    public DrawObjects(ActivityCamera activityCamera, Map<Integer, Object> output, int width, int height, Mat imageMatrix, List<String> classLabels, boolean audioOn, List<String> listOfWords) {
        this.height = height;
        this.width = width;
        this.output = output;
        this.imageMatrix = imageMatrix;
        this.classLabels = classLabels;
        this.audioOn = audioOn;
        this.activityCamera = activityCamera;
        this.listOfWords = listOfWords;
        drawObjects();
    }

    // Reference: https://github.com/pramod722445/Object_Detection_App
    private void drawObjects() {
        int i = 0;
        // i can be adjusted to recognize up to 25 objects (EfficientDet) or 10 objects (MobileNet) per frame
        while (i < OBJECTS_TO_DETECT){
            float label = (float) Array.get(Array.get(output.get(1), 0), i);

            //float audioLabel = (float) Array.get(Array.get(output.get(1), 0), 0);  // need audio for one of all the objects detected
            //String labelForAudio = classLabels.get((int) audioLabel);   // first seen - first out

            float threshold = (float) Array.get(Array.get(output.get(2), 0), i);
            if (threshold > THRESHOLD) {  // checking if confidence is more than required set threshold
                String labelName = classLabels.get((int) label);
                float roundedAcc = (float) (Math.round(threshold * 100.0) / 100.0);  // getting accuracy
                activityCamera.accuracyList.add(roundedAcc);
                String acc = String.valueOf(roundedAcc);
                listOfWords.add(labelName.toLowerCase());
                activityCamera.uniqueObjects.addAll(listOfWords);
                detectionMade(labelName,i,acc);

                // old version of speech synthesis
                // checking if audio should be fed to user
                if (audioOn) {
                    if (listOfWords.size() == 1){
                    //new SpeechSynthesis(activityCamera,joined);
                    }
                    else if (listOfWords.size() > 1){
                        // avoiding repeating the same word if phone is pointing to the same object
                        //if ((!listOfWords.get(listOfWords.size()-2).equals(listOfWords.get(listOfWords.size()-1))) && !listOfWords.get(listOfWords.size()-1).equals(listOfWords.get(listOfWords.size()-3))){
                        //new SpeechSynthesis(activityCamera,labelForAudio);
                        //}
                    }
                }
            }
            i++;
        }
    }

    // Reference: https://github.com/pramod722445/Object_Detection_App
    private void detectionMade(String labelName, int object, String acc){
        Object box = Array.get(Objects.requireNonNull(Array.get(output.get(0), 0)),object); // get bounding boxes for each detected object

        float top = (float) Array.get(box,0) * height;
        float left = (float) Array.get(box,1) * width;
        float right = (float) Array.get(box,3) * width;
        float bottom = (float) Array.get(box,2) * height;


        Point topLeftPoint = new Point(left,top);
        Point bottomRightPoint = new Point(right,bottom);
        Scalar boxColour = new Scalar(0,0,255);
        Scalar textColour = new Scalar(255,0,0);
        Point textPos = new Point((left+right)/2,top);

        // draw rectangle boxes around objects detected
        Imgproc.rectangle(imageMatrix,topLeftPoint, bottomRightPoint, boxColour,2);
        String labelWithAcc = labelName + " : " + acc;
        // put text on rectangle boxes with object class
        Imgproc.putText(imageMatrix, labelWithAcc, textPos,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,2,textColour,2);
        activityCamera.recognize_objects = false;
    }


    // used to draw object for longer time when audio is enabled
    // Reference: https://github.com/pramod722445/Object_Detection_App
    public void drawBoxesLonger(int width, int height) {
        if (!activityCamera.draw_output.isEmpty()) {
            int i = 0;
            // i can be adjusted to recognize up to 25 objects (EfficientDet) or 10 objects (MobileNet) per frame
            while (i < 25) {
                float label = (float) Array.get(Array.get(activityCamera.draw_output.get(1), 0), i);

                float threshold = (float) Array.get(Array.get(activityCamera.draw_output.get(2), 0), i);
                if (threshold > 0.6) {  // checking if confidence is more than required set threshold
                    String labelName = classLabels.get((int) label);
                    float roundedAcc = (float) (Math.round(threshold * 100.0) / 100.0);  // getting accuracy
                    String acc = String.valueOf(roundedAcc);
                    detectionMadeLong(labelName, i, acc, width, height);
                }
                i++;
            }
        }
    }
    // Reference: https://github.com/pramod722445/Object_Detection_App
    private void detectionMadeLong(String labelName, int object, String acc, int width, int height){
        Object box = Array.get(Objects.requireNonNull(Array.get(activityCamera.draw_output.get(0), 0)),object); // get bounding boxes for each detected object
        float top = (float) Array.get(box,0) * height;
        float left = (float) Array.get(box,1) * width;
        float right = (float) Array.get(box,3) * width;
        float bottom = (float) Array.get(box,2) * height;


        Point topLeftPoint = new Point(left,top);
        Point bottomRightPoint = new Point(right,bottom);
        Scalar boxColour = new Scalar(0,0,255);
        Scalar textColour = new Scalar(255,0,0);
        Point textPos = new Point((left+right)/2,top);

        // draw rectangle boxes around objects detected
        Imgproc.rectangle(imageMatrix, topLeftPoint, bottomRightPoint, boxColour, 2);
        String labelWithAcc = labelName + " : " + acc;
        // put text on rectangle boxes with object class
        Imgproc.putText(imageMatrix, labelWithAcc, textPos, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, 2, textColour, 2);
    }


}
