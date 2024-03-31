package com.example.mobileapplication;

import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class SpeechSynthesis extends ActivityCamera{
    private  ActivityCamera activityCamera;
    private final Set<String> labelsList;
    private String labelName;
    private final String TAG = "TTS";
    private ActivityCameraYolo activityCameraYolo;
    private StringBuilder ocrText;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public SpeechSynthesis(ActivityCamera activityCamera, Set<String> labelsList, StringBuilder joinedOCR) {
        this.labelsList = labelsList;
        this.activityCamera = activityCamera;
        this.ocrText = joinedOCR;
        naturalLanguageProcessing();
    }

    // used to process text
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void naturalLanguageProcessing(){
        // output processing according to logic
        List<String> result = audioTemplates();
        labelName = String.join(", ", result);

        String outMessage = "";
        // nothing detected
        if (ocrText.length() == 0 && labelName.length() == 0){
            outMessage = "";
        }
        // only text detected
        else if (labelName.length() == 0){
            outMessage += "Text ahead saying " + ocrText;
        }
        // only objects detected
        else if (ocrText.length() == 0){
            outMessage += "Ahead is a " + labelName;
        }
        // both objects and text are detected
        else {
            outMessage += "Ahead is a " + labelName + ". Text ahead saying " + ocrText;
        }

        TextView sceneDescriptionView = activityCamera.findViewById(R.id.sceneDescription);
        sceneDescriptionView.setMovementMethod(new ScrollingMovementMethod());
        sceneDescriptionView.setText(outMessage);

        // check whether the audio should be fed to the user
        if (activityCamera.audioOn) {
            audioFeedbackMain(outMessage);
        }
    }

    // output processing according to logic
    @RequiresApi(api = Build.VERSION_CODES.N)
    private List<String> audioTemplates() {
        List<String> result = labelsList.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        // unnecessary classes
        result.remove("monochrome");
        result.remove("sky");
        result.remove("pattern");
        result.remove("musical instrument");

        // some audio templates to improve scene description
        if (result.contains("bedroom") || result.contains("kitchen"))
            result.remove("room");
        if (result.contains("bedroom"))
            result.remove("bed");
        if (result.contains("bed") && result.contains("room")) {
            result.remove("bed");
            result.remove("room");
            result.add("bedroom");
        }
        if ((result.contains("tv") && result.contains("keyboard")) ||(result.contains("tv") && result.contains("mouse"))) {
            result.remove("tv");
            result.remove("keyboard");
            result.remove("mouse");
            result.add("desktop PC with monitor, keyboard, and a mouse");
        }
        if ((result.contains("toe") || result.contains("foot")) && result.contains("hand")) {
            result.remove("hand");
            result.remove("toe");
            result.remove("foot");
            result.add("person's foot");
        }
        if ((result.contains("toe") || result.contains("foot")) && !result.contains("person's foot")) {
            result.remove("toe");
            result.remove("foot");
            result.add("person's foot");
        }
        if (result.contains("hand") && result.contains("person")) {
            result.remove("person");
            result.remove("hand");
            result.add("person's hand");
        }
        if (result.contains("cell phone")) {
            result.remove("mobile phone");
        }
        if ((result.contains("hand") || result.contains("person's hand")) && (result.contains("cell phone") || result.contains("mobile phone"))) {
            result.remove("mobile phone");
            result.remove("cell phone");
            result.remove("hand");
            result.remove("person's hand");
            result.add("hand holding a cell phone");
        }
        if (result.contains("person") && result.contains("glasses")){
            result.remove("person");
            result.remove("glasses");
            result.add("person with glasses");
        }
        if (result.contains("couch") && result.contains("room")){
            result.remove("room");
            result.add("living room");
        }
        if (result.contains("couch") && result.contains("tv") && !result.contains("living room")){
            result.remove("room");
            result.add("living room");
        }
        if (result.contains("car") && result.contains("road")){
            result.remove("car");
            result.remove("road");
            result.remove("vehicle");
            result.add("Road with cars");
        }
        if (result.contains("vehicle") && result.contains("road")){
            result.remove("vehicle");
            result.remove("road");
            result.add("road with cars");
        }
        if (result.contains("vehicle") && result.contains("car") && result.contains("road")){
            result.remove("vehicle");
            result.remove("road");
            result.remove("car");
            result.add("road with cars and vehicles");
        }

        if (result.size() > 1) {
            String lastItem = result.get(result.size() - 1);
            result.remove(result.get(result.size() - 1));
            result.add("and a "+ lastItem);
        }
        return result;
    }

    private void audioFeedbackMain(String outMessage){
        float pitch = 1.0f;
        float speed = 1.3f;
        activityCamera.textToSpeech.setPitch(pitch);
        activityCamera.textToSpeech.setSpeechRate(speed);
        activityCamera.map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "SR-START");
        activityCamera.textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
                Log.e("Talking","START");
                activityCamera.audioFeedEnded = false;
            }

            // when audio stops talking --> then do scene scan again (booleans used to achieve that)
            // help for utterance progress listener found on https://stackoverflow.com/questions/24956784/speech-recognition-intent-starts-before-tts-ends-speaking
            @Override
            public void onDone(String s) {
                Log.e("Talking","ENDED");
                activityCamera.listOfWords.clear();
                activityCamera.uniqueObjects.clear();
                activityCamera.listOfText.clear();
                activityCamera.audioFeedEnded = true;
            }

            @Override
            public void onError(String s) {

            }
        });
        activityCamera.textToSpeech.speak(outMessage,TextToSpeech.QUEUE_FLUSH, activityCamera.map);
    }

    // not in use
    public SpeechSynthesis(ActivityCameraYolo activityCameraYolo, String LabelsList) {
        this.labelsList = Collections.singleton(LabelsList);
        this.activityCameraYolo = activityCameraYolo;
        ttsAudioYolo();
    }
    // not in use
    public void ttsAudioYolo(){
        if (textToSpeech==null) {
            textToSpeech = new TextToSpeech(activityCameraYolo, i -> {
                if (i == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(Locale.ENGLISH);
                    audioFeedback(labelName);

                } else {
                    Log.e(TAG, "TTS was not loaded");
                }
            });
        }
    }
    // not in use
    private void audioFeedback(String labelName){
        float pitch = 1.0f;
        float speed = 1.0f;
        textToSpeech.setPitch(pitch);
        textToSpeech.setSpeechRate(speed);
        textToSpeech.speak(labelName,TextToSpeech.QUEUE_FLUSH, null);
    }
}
