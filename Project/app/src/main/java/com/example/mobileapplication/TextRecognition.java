package com.example.mobileapplication;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognizer;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.List;


public class TextRecognition{
    private final ActivityCamera activityCamera;
    private final Mat imageMatrix;
    private final TextRecognizer textRecognizer;
    private final InputImage image;
    private final List<String> listOfText;

    public TextRecognition(ActivityCamera activityCamera, Mat imageMatrix, TextRecognizer textRecognizer, InputImage image, List<String> listOfText){
        this.activityCamera = activityCamera;
        this.textRecognizer = textRecognizer;
        this.imageMatrix = imageMatrix;
        this.image = image;
        this.listOfText = listOfText;
        drawOCR();
    }

    // Reference for the implementation of Text Recognition - OCR
    // https://developers.google.com/ml-kit/vision/text-recognition
    public void drawOCR(){
        Task<Text> result = textRecognizer.process(image).addOnSuccessListener(new OnSuccessListener<Text>() {
            @Override
            public void onSuccess(Text text) {
                // a Block is a contiguous set of text lines, such as a paragraph or column
                for (Text.TextBlock block : text.getTextBlocks()) {
                    Rect boundingBox = block.getBoundingBox();  // getting a rectangle box of the detection
                    Point[] cornerPoints = block.getCornerPoints();  // getting the corner points of the detection
                    String textResult = block.getText();   // getting the string text block of the detection
                    if (textResult.length() > 2) {
                        listOfText.add(textResult);
                    }
                    Log.e("OCR", ""+textResult);
                    assert boundingBox != null;
                    Imgproc.rectangle(imageMatrix,new org.opencv.core.Point(boundingBox.left,boundingBox.top), new org.opencv.core.Point(boundingBox.right,boundingBox.bottom),new Scalar(255,0,0),2);

                    // a Line is a contiguous set of words on the same axis
                    for (Text.Line line: block.getLines()) {
                        String lineText = line.getText();
                        Rect lineFrame = line.getBoundingBox();
                        assert lineFrame != null;
                        Imgproc.putText(imageMatrix, lineText, new org.opencv.core.Point((lineFrame.left+lineFrame.right)/2,lineFrame.top),Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,2, new Scalar(0,255,0),2);
                        //current implementation does not require looping through inner elements of a line
                        // an Element is a contiguous set of word on the same axis in most Latin languages, or a character in others
                        /*for (Text.Element element: line.getElements()) {
                            String elementText = element.getText();
                            Rect elementFrame = element.getBoundingBox();
                        }*/
                    }

                }

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i("OCR","Failure with OCR!");
            }
        });
    }
}
