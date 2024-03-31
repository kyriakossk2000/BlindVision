package com.example.mobileapplication;

// source of code for making predictions:
// https://github.com/pramod722445/Object_Detection_App
// https://www.youtube.com/watch?v=4WSv4NLl9po&t=612s


import android.graphics.Bitmap;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognizer;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class SceneUnderstanding extends ActivityCamera {
    private final int MODEL_SIZE = 320; // EfficientDetLite0 model input size is 320 / MobileNetV1 model input size is 300
    private final Interpreter model;  // saves model for predictions
    private final List<String> classLabels; // saves all class labels
    private final Mat imageMatrix;   // saves image frame
    private final boolean audioOn; // used to determine if audio is on
    private final ActivityCamera activityCamera;
    private final List<String> listOfWords;
    private final List<String> listOfText;
    private final TextRecognizer textRecognizer;  // text recognition model for text understanding
    private DrawObjects drawObjects;

    public SceneUnderstanding(ActivityCamera activityCamera, Interpreter model, List<String> classLabels, Mat imageMatrix, boolean audioOn, List<String> listOfWords, TextRecognizer textRecognizer, List<String> listOfText) {
        this.model = model;
        this.classLabels = classLabels;
        this.imageMatrix = imageMatrix;
        this.audioOn = audioOn;
        this.activityCamera = activityCamera;
        this.listOfWords = listOfWords;
        this.textRecognizer = textRecognizer;
        this.listOfText = listOfText;
    }

    // Reference: https://github.com/pramod722445/Object_Detection_App
    public Mat imageScene() {
        rotateImage90(imageMatrix);
        // Returns a mutable bitmap with the specified width and height
        Bitmap map = Bitmap.createBitmap(imageMatrix.cols(), imageMatrix.rows(), Bitmap.Config.ARGB_8888);
        // creating bitmap from mat. Bitmap images can be processed
        Utils.matToBitmap(imageMatrix, map);

        // For Optical Character Recognition
        InputImage image = InputImage.fromBitmap(map, 0);
        if (activityCamera.modeOfDetection == 1 || activityCamera.modeOfDetection == 3) {
            // recognizing text every 2 seconds
            if (activityCamera.audioOn) {
                if (activityCamera.recognize_text) {
                    activityCamera.recognize_text = false;
                    new TextRecognition(this, imageMatrix, textRecognizer, image, listOfText);
                }
            } else {
                new TextRecognition(this, imageMatrix, textRecognizer, image, listOfText);
            }
        }
        // For Image Classification
        if (activityCamera.modeOfDetection == 1 || activityCamera.modeOfDetection == 2) {
            // classifying image every 2 seconds
            if (activityCamera.audioOn) {
                if (activityCamera.recognize_image) {
                    activityCamera.recognize_image = false;
                    new ImageClassifier(this, image, activityCamera.uniqueObjects);
                }
            } else {
                new ImageClassifier(this, image, activityCamera.uniqueObjects);
            }
        }

        map = image.getBitmapInternal();  // converting InputImage back to Bitmap
        Bitmap modelBitmap = scaledBitMap(map); // scaling bitmap to model input size

        ByteBuffer buffer;
        int[] pixelsArray = new int[(int) Math.pow(MODEL_SIZE, 2)];
        // returns a byte array with the input signature of the model 300 x 300 -> MobileNet, 320 X 320 -> EfficientDet
        // then adding it to an array to fed it to model as a flattened buffer of 270.000 byte values (300 x 300 x 3) or (320 x 320 x 3)
        buffer = bitmapToBuffer(modelBitmap, modelBitmap.getWidth(), modelBitmap.getHeight(), pixelsArray);

        Object[] input = new Object[1];
        input[0] = buffer;  // saving buffer in the input array that will be fed to the model

        // the model outputs four arrays, mapped to indices 0-4
        // Arrays 0,1,2 are for detected objects
        // MobileNet model can recognize up to 10 objects per frame
        // EfficientDet model can recognize up to 25 objects per frame
        Map<Integer, Object> output = new TreeMap<>(); //tree map to sort objects according to the best prediction --> it will display the object with best accuracy
        float[][][] boxes = new float[1][25][4];  // saving coordinates of box
        float[][] accuracy = new float[1][25];    // saving accuracy
        float[][] classes = new float[1][25];     // saving class of object
        output.put(0, boxes);  // locations -> multidimensional array of [25][4] values 0-1 representing bounding boxed (top,left,right,bottom)
        output.put(1, classes); // classes -> array of 25 integers each indicating the index of a class label from the labels file
        output.put(2, accuracy); // scores -> array of 25 objects with values from 0-1 representing probability that a class was detected

        // check if mode of detection requires object detection
        if (activityCamera.modeOfDetection == 1 || activityCamera.modeOfDetection == 2) {
            // check seen objects in a scene every 2 sec
            if (activityCamera.audioOn) {
                if (activityCamera.recognize_objects) {
                    this.model.runForMultipleInputsOutputs(input, output);
                    activityCamera.draw_output.putAll(output);  // saving the output map to a temp map so drawing objects a bit longer when audio is enable
                }
            } else {
                this.model.runForMultipleInputsOutputs(input, output);
            }
        }

        assert map != null;
        drawObjects = new DrawObjects(activityCamera, output, map.getWidth(), map.getHeight(), imageMatrix, classLabels, audioOn, listOfWords);

        // used to draw the boxes of the detected objects for longer time
        if (activityCamera.modeOfDetection == 1 || activityCamera.modeOfDetection == 2) {
            if (activityCamera.audioOn) {
                if (activityCamera.frameCounter <= 10) {  // keep object boxes for 10 frames
                    drawObjects.drawBoxesLonger(map.getWidth(), map.getHeight());
                }
            }
        }

        return rotateImageBack(imageMatrix);
    }

    // input size of model scaling
    private Bitmap scaledBitMap(Bitmap map) {
        // scaling bitmap to model size input
        return Bitmap.createScaledBitmap(map, MODEL_SIZE, MODEL_SIZE, false);
    }

    // rotating image by 90 degrees to do prediction on original captured frame
    private Mat rotateImage90(Mat sceneImage) {

        Core.flip(sceneImage.t(), sceneImage, 1);
        return sceneImage;
    }

    // rotating image back in normal to return it to onCameraFrame method and display it on the app
    private Mat rotateImageBack(Mat sceneImage) {

        Core.flip(sceneImage.t(), sceneImage, 0);
        return sceneImage;

    }

    // converting bitmap to byte buffer
    private ByteBuffer bitmapToBuffer(Bitmap map, int width, int height, int[] pixelsArray) {
        ByteBuffer buffer;
        buffer = ByteBuffer.allocateDirect((int) Math.pow(MODEL_SIZE, 2) * 3);
        buffer.order(ByteOrder.nativeOrder());
        map.getPixels(pixelsArray, 0, width, 0, 0, width, height);
        int count = 0;
        int i = 0;
        while (i < MODEL_SIZE * MODEL_SIZE) {
            buffer.put((byte) ((pixelsArray[count] >> 16) & 0xFF));
            buffer.put((byte) ((pixelsArray[count] >> 8) & 0xFF));
            buffer.put((byte) (pixelsArray[count] & 0xFF));
            count++;
            i++;
        }
        return buffer;
    }

}
