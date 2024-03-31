package com.example.mobileapplication;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.List;
import java.util.Set;

public class ImageClassifier {
    private final ActivityCamera activityCamera;
    private final InputImage image;
    private final Set<String> uniqueObjects;
    private ImageLabeler classifier = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);
    private final double THRESHOLD = 0.8;

    public ImageClassifier(ActivityCamera activityCamera, InputImage image, Set<String> uniqueObjects) {
        this.activityCamera = activityCamera;
        this.image = image;
        this.uniqueObjects = uniqueObjects;
        imageLabelling();
    }
    // Reference for the implementation of Image Classification
    // https://developers.google.com/ml-kit/vision/image-labeling
    public void imageLabelling() {
        classifier.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
                    @Override
                    public void onSuccess(List<ImageLabel> imageLabels) {
                        Log.i("ImageClassifier","Success!");
                        for(ImageLabel i : imageLabels){
                            String result = i.getText();
                            double accuracy = i.getConfidence();
                            int index = i.getIndex();
                            Log.e("ImageClassifier","Text: "+result + " Accuracy: "+accuracy + " Index: " + index);
                            // add only labels that exist needed confidence
                            if (accuracy >= THRESHOLD){
                                uniqueObjects.add(result.toLowerCase());
                            }

                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i("ImageClassifier","Failure with Image Classification!");
                    }
                });
    }
}
