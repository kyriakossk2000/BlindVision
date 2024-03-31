package com.example.mobileapplication;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.List;

// source of code for drawing rectangles when a detection is made:
// https://github.com/matteomedioli/AndroidObjectDetection-OpenCV.git

public class DrawObjectsYolo {
    private final ActivityCameraYolo activityCameraYolo;
    private final Mat imageMatrix;
    private  Mat row;
    private Point labelDetected;
    private List<String> listOfWords;
    private List<String> classLabels;
    private final boolean audioOn;
    private double acc;

    public DrawObjectsYolo(ActivityCameraYolo activityCameraYolo, Mat imageMatrix, Mat row, Point labelDetected, List<String> listOfWords, List<String> classLabels, boolean audioOn, double roundedAcc) {
        this.activityCameraYolo = activityCameraYolo;
        this.imageMatrix = imageMatrix;
        this.row = row;
        this.labelDetected = labelDetected;
        this.listOfWords = listOfWords;
        this.classLabels = classLabels;
        this.audioOn = audioOn;
        this.acc = roundedAcc;

        int class_id = (int) labelDetected.x; // getting class id from label detected
        String label= classLabels.get(class_id);  // matching it with list actual label
        String accuracy = String.valueOf(acc);
        drawObjects(label,accuracy);
        listOfWords.add(label);
        activityCameraYolo.uniqueObjects.addAll(listOfWords);
        audioFeedback(label);
    }
    // old version of speech synthesis
    private void audioFeedback(String label) {
        if (audioOn) {
            if (listOfWords.size() == 1){
            //new SpeechSynthesis(activityCameraYolo,label);
            }
            else if (listOfWords.size() > 1){
                // avoiding repeating the same word if phone is pointing to the same object
                //if ((!listOfWords.get(listOfWords.size()-2).equals(listOfWords.get(listOfWords.size()-1))) && !listOfWords.get(listOfWords.size()-1).equals(listOfWords.get(listOfWords.size()-3))){
                //new SpeechSynthesis(activityCameraYolo,label);
                //}
            }
        }
    }
    // Reference: https://github.com/matteomedioli/AndroidObjectDetection-OpenCV.git
    private void drawObjects(String label, String acc){
        // getting bounding boxes for detected objects
        int left = (int) ((row.get(0, 0)[0] * imageMatrix.cols()) - (row.get(0, 2)[0] * imageMatrix.cols()) * 0.5); // width
        int top =(int)((row.get(0, 1)[0] * imageMatrix.rows()) - (row.get(0, 3)[0] * imageMatrix.rows()) * 0.5);   // height
        int right =(int)((row.get(0, 0)[0] * imageMatrix.cols()) + (row.get(0, 2)[0] * imageMatrix.cols()) * 0.5);
        int bottom =(int)((row.get(0, 1)[0] * imageMatrix.rows()) + (row.get(0, 3)[0] * imageMatrix.rows()) * 0.5);

        Point topLeftPoint = new Point(left,top);
        Point bottomRightPoint = new Point(right,bottom);
        Point textPos = new Point((left+right)/2,top);

        String labelWithAcc = label + " : " + acc;

        // draw rectangle boxes around objects detected
        Imgproc.rectangle(imageMatrix, topLeftPoint,bottomRightPoint , new Scalar(0,0,255), 3);
        // put text on rectangle boxes with object class
        Imgproc.putText(imageMatrix, labelWithAcc, textPos,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,2,new Scalar(255, 0, 0),2);
    }
}
