package com.example.mobileapplication;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

// source of code for making predictions:
// https://github.com/matteomedioli/AndroidObjectDetection-OpenCV.git

public class SceneUnderstandingYolo{
    private final Net net;  // saves model for predictions
    private final List<String> classLabels; // saves all class labels
    private final Mat imageMatrix;   // saves image frame
    private final boolean audioOn; // used to determine if audio is on
    private final ActivityCameraYolo activityCameraYolo;
    private final List<String> listOfWords;
    private final int INPUT_SIZE = 224;  // input size for Yolov4 tiny model -> was reduced for better performance (original is 416 x 416)
    private final double THRESHOLD = 0.5; // required accuracy

    public SceneUnderstandingYolo(ActivityCameraYolo activityCameraYolo, Net net, List<String> classLabels, Mat rgba, boolean audioOn, List<String> listOfWords) {
        this.activityCameraYolo = activityCameraYolo;
        this.net = net;
        this.classLabels = classLabels;
        this.imageMatrix = rgba;
        this.audioOn = audioOn;
        this.listOfWords = listOfWords;
    }

    // Reference: https://github.com/matteomedioli/AndroidObjectDetection-OpenCV.git
    public Mat imageScene() {
        rotateImage90(imageMatrix);

        // converting frame colour from 4 channels to 3
        Imgproc.cvtColor(imageMatrix, imageMatrix, Imgproc.COLOR_RGBA2RGB);

        //Mean subtraction, Scaling, and optionally channel swapping
        Mat blob = Dnn.blobFromImage(imageMatrix, 1.0 / 255.0, new Size(INPUT_SIZE,INPUT_SIZE), new Scalar(127.5), true, false);

        // sets the new input value for the network
        net.setInput(blob);

        List<Mat> result = new ArrayList<>();
        // gives us the index of the output layers of the network
        List<String> outNames = net.getUnconnectedOutLayersNames();

        // runs a forward pass to compute the network output
        // save them to a list of mats containing all information (positions and labels)
        net.forward(result, outNames);

        double accuracy;
        int i = 0;
        // iterating in the list of returned mats
        while (i < result.size()) {
            Mat out = result.get(i);
            int j = 0;
            while (j < out.rows()) {
                Mat row = out.row(j);
                Mat scores = row.colRange(5, out.cols());
                Core.MinMaxLocResult locResult = Core.minMaxLoc(scores); // finds global minimum and maximum in an array
                accuracy = locResult.maxVal;  // get the best accuracy
                Point labelDetected = locResult.maxLoc;  // get the "id" of the location -> will be matched to labels
                // if needed threshold is satisfied by the confidence of detection
                if (accuracy > THRESHOLD) {
                    double roundedAcc = (Math.round(accuracy * 100.0) / 100.0);
                    activityCameraYolo.accuracyList.add(roundedAcc);
                    new DrawObjectsYolo(activityCameraYolo, imageMatrix, row, labelDetected, listOfWords,classLabels,audioOn,roundedAcc);
                }
                j++;
            }
            i++;
        }
        return rotateImageBack(imageMatrix);
    }
    // rotating image by 90 degrees to do prediction on original captured frame
    private Mat rotateImage90(Mat sceneImage){

        Core.flip(sceneImage.t(),sceneImage,1);
        return sceneImage;
    }

    // rotating image back in normal to return it to onCameraFrame method and display it on the app
    private Mat rotateImageBack(Mat sceneImage){

        Core.flip(sceneImage.t(),sceneImage,0);
        return sceneImage;
    }
}
