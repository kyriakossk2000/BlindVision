package com.example.mobileapplication;

import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
// yolov3 tiny and yolov2 tiny models taken from:
// https://pjreddie.com/darknet/yolo/
// yolov4 tiny model taken from
// https://github.com/kiyoshiiriemon/yolov4_darknet
// source of code for loading model:
// https://github.com/matteomedioli/AndroidObjectDetection-OpenCV.git
public class ModelLoaderYolo {

    private final ActivityCameraYolo activityCameraYolo;
    public Net net;

    public ModelLoaderYolo(ActivityCameraYolo activityCameraYolo) throws IOException {
        this.activityCameraYolo = activityCameraYolo;
        String fileCong = "yolov4-tiny.cfg";
        String fileWeights = "yolov4-tiny.weights";

        byte[] dataConf = getData(fileCong);
        byte[] dataWeight = getData(fileWeights);

        String yoloConfig = getPath(fileCong,dataConf);
        String yoloWeight = getPath(fileWeights, dataWeight);

        // fed to Darknet the configuration and weights files
        // reads a network model stored in Darknet model files
        net = Dnn.readNetFromDarknet(yoloConfig,yoloWeight);
    }
    private byte[] getData(String file) throws IOException {
        BufferedInputStream inputStream = new BufferedInputStream(activityCameraYolo.getAssets().open(file));
        byte[] dataConf = new byte[inputStream.available()];
        inputStream.read(dataConf);
        inputStream.close();
        return dataConf;
    }

    private String getPath(String file, byte[] data) throws IOException {
        File fileOut = new File(activityCameraYolo.getFilesDir(), file);
        FileOutputStream out = new FileOutputStream(fileOut);
        out.write(data);
        out.close();
        return fileOut.getAbsolutePath();
    }
}
