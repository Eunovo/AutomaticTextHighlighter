/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.novo.twilight;

import com.uniben.autotexthighlighter.Twilight;
import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.Microphone;
import edu.cmu.sphinx.api.SpeechResult;
import java.io.IOException;

/**
 *
 * @author Eunovo
 */
public class SpeechRecognizer {
    
    Thread speechThread;
    
    private String result;
    
    //resources
    private final String acousticModelPath = getClass().getResource("/en-us-adapt").toExternalForm();
    private final String dictionaryPath = getClass().getResource("/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict").toExternalForm();
    private final String languageModelPath = getClass().getResource("/edu/cmu/sphinx/models/en-us/en-us.lm.bin").toExternalForm();
    
    private boolean alive = false;
    
    private final LiveSpeechRecognizer recognizer;
    //private final Microphone micro;
    
    public SpeechRecognizer() throws IOException, IllegalStateException{
        Configuration config = new Configuration();
    
        //load model
        config.setAcousticModelPath(acousticModelPath);
        config.setDictionaryPath(dictionaryPath);
        config.setLanguageModelPath(languageModelPath);
        //config.setSampleRate(44100);
        
        //micro = new Microphone(44100, 16, true, false);

        //micro.startRecording();

        //recognizer = new StreamSpeechRecognizer(
            //config);
        
        recognizer = new LiveSpeechRecognizer(config);
        
    }
    
    
    /**
     * Starts the speech recognizer thread
     * @param obj  takes the Twilight Application obj
     * @return true if the speech thread was started successfully, false otherwise
     */
    public boolean start(Twilight obj){
        recognizer.startRecognition(false);
        return startSpeechThread(obj);
    }
    
    /**
     * Tries to Start the speech thread
     * @param obj takes the twilight obj
     * @return true if the speech thread was started successfully, false otherwise
     */
    public boolean startSpeechThread(Twilight obj){
        //alive?
        if(speechThread != null && speechThread.isAlive())
            return false;
        
        alive = true;
        speechThread = new Thread(() -> {
            while(alive){
                SpeechResult speechResult = recognizer.getResult();
                if(speechResult != null)
                {
                    synchronized(obj){
                        result = speechResult.getHypothesis();
                        if(result != null)
                        {
                            System.out.println("You said [\""+result+"\"]");
                            obj.log("You said [\""+result+"\"]");
                            obj.act(result);
                        }
                        else System.out.println("I didn't get you");
                    }
                }
            }
            recognizer.stopRecognition();
        });
        //start
        speechThread.start();
        System.out.println("You can speak now");
        obj.log("You can speak now");
        return true;
    }
    
    public String getResult(){
        return result;
    }
    
    /**
     * stops the speech thread if it is alive
     */
    public void stop(){
        //alive?
        if(speechThread != null && speechThread.isAlive())
        {
            //recognizer.stopRecognition();
            alive = false;
        }
    }
    
    /**
     * 
     * @return true if speech thread is alive, false otherwise 
     */
    public boolean isAlive(){
        return ( speechThread != null && speechThread.isAlive());
    }
    
}
