package com.example.mobileapplication;

import android.content.res.AssetFileDescriptor;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ModelLoader extends ActivityCamera{
    public Interpreter modelInterpreter;
    private GpuDelegate gpu;  // used to delegate phone's GPU --> better performance
    public ModelLoader(AssetFileDescriptor fileDescriptor) throws IOException {
        try {
            Interpreter.Options power = new Interpreter.Options();
            gpu = new GpuDelegate();
            power.addDelegate(gpu);
            power.setNumThreads(4);
            modelInterpreter= new Interpreter(modelLoad(fileDescriptor),power);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    // source: https://www.youtube.com/watch?v=RhjBDxpAOIc&t=76s
    private MappedByteBuffer modelLoad(AssetFileDescriptor fileDescriptor) throws IOException{
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long start = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return  fileChannel.map(FileChannel.MapMode.READ_ONLY,start,declaredLength);
    }

}
