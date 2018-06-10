/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.uniben.autotexthighlighter;

import com.novo.twilight.Presentation;
import com.novo.twilight.Settings;
import com.novo.twilight.SpeechRecognizer;
import java.io.File;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.io.IOException;
import java.util.NoSuchElementException;
import com.novo.utils.LinkedBlockingQueue;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingNode;
import javafx.scene.Cursor;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.WindowEvent;
import javax.swing.Timer;



/**
 *
 * @author Eunovo
 */
public class Twilight extends Application {
    
    /*
        Next thoughts
        use POI to read pptx files, highlight them and 
            present the pngs or present the text
    */
    
    private final StackPane root = new StackPane();
    SwingNode swingNode;
    
    LinkedBlockingQueue queueOfWords = new LinkedBlockingQueue();
    
    Thread resultThread;
    
    public String result;
    
    SpeechRecognizer speech;
    
    Presentation ppt;
    
    Timer trigger;
    
    public static int height = 500, width = 700;
    private final double LOG_STAGE_HEIGHT = 200, LOG_STAGE_WIDTH = 300;
    
    private final String fileChooserDir = "FileChooserDir";
    
    private VBox sceneRoot;
    private Stage stage;
    private boolean isFullScreen = true;
    private final TextArea LOG_TEXT_VIEW = new TextArea();
    
    //init values are read from prefs
    int initSpinnerValue = (int)Double.parseDouble(Settings.get("FontSize", "18"));
    String initFontType = Settings.get("FontType", "SansSerif");
    Color initColor = Color.web(Settings.get("HighlightColor", "white"));
    
    @Override
    public void start(Stage primaryStage){
        stage = primaryStage;
        Group welcomeScr = new Group();
        Background purpleBackground = new Background(
                new BackgroundFill(Paint.valueOf("Purple"),CornerRadii.EMPTY, Insets.EMPTY));
        Paint buttonTextColor = Paint.valueOf("Purple");
        
        swingNode = new SwingNode();
        ImageView imageView = new ImageView();
        Image welcomeImage = new Image(getClass().getResource("/images/unibenlogo.png").toExternalForm());
        imageView.setImage(welcomeImage);
        AnchorPane anchor = new AnchorPane();
        anchor.setPadding(new Insets(10,10,10,10));
        
        anchor.getChildren().add(imageView);
        welcomeScr.getChildren().add(anchor);
        welcomeScr.setAutoSizeChildren(true);
        Button listenBtn = new Button();
        listenBtn.setText("Listen");
        listenBtn.setTextFill(buttonTextColor);
        listenBtn.setOnAction((ActionEvent event) -> {
            listenBtn.setCursor(Cursor.WAIT);
            swingNode.setCursor(Cursor.WAIT);
            try {
                if(ppt != null)
                {    
                    speech = new SpeechRecognizer();
                    speech.start(this);
                    startResultThread();
                    listenBtn.setText("Listening...");
                }
                else if(speech != null && speech.isAlive()){
                    speech.stop();
                    listenBtn.setText("Listen");
                }
            } catch (IOException ex) {
                Logger.getLogger(Twilight.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalStateException ex) {
                Text errorText = new Text();
                System.out.println(ex.getMessage());
                errorText.setText(ex.getMessage());
                Alert alert = new Alert(AlertType.ERROR);
                alert.getDialogPane().getChildren().add(errorText);
                alert.showAndWait();
            }finally{
                listenBtn.setCursor(Cursor.DEFAULT);
                swingNode.setCursor(Cursor.DEFAULT);
            }
        });
        
        Button nextBtn = new Button();
        nextBtn.setText("Next");
        nextBtn.setTextFill(buttonTextColor);
        nextBtn.setOnAction((ActionEvent event) -> {
            if(ppt != null){
                ppt.next();
            }
        });
        
        Button prevBtn = new Button();
        prevBtn.setText("Prev");
        prevBtn.setTextFill(buttonTextColor);
        prevBtn.setOnAction((ActionEvent event) -> {
            if(ppt != null){
                ppt.previous();
            }
        });
        
        HBox bottomPanel = new HBox();
        bottomPanel.setBackground(purpleBackground);
        bottomPanel.setPadding(new Insets(5,20,5,20));
        bottomPanel.setSpacing(10);
        
        bottomPanel.getChildren().addAll(listenBtn, prevBtn, nextBtn);
        
        StackPane.setAlignment(welcomeScr, Pos.CENTER);
        root.getChildren().add(welcomeScr);
        root.setBackground(new Background( new BackgroundFill(Paint.valueOf("white"),
                CornerRadii.EMPTY, Insets.EMPTY)));
        
        Scene scene = new Scene(new VBox());
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Presentation File");
        fileChooser.getExtensionFilters().addAll
            (new FileChooser.ExtensionFilter("PPTX","*.pptx"),
                    new FileChooser.ExtensionFilter("PPT","*.ppt"));
        String initDir = Settings.get(fileChooserDir, "C:/"); //retrieve saved dir
        try{
            fileChooser.setInitialDirectory(new File(initDir));
        }catch(Exception e){
            //do nothing 
        }
        Button importButton = new Button("Import");//import menu item
        importButton.setTextFill(buttonTextColor);
        importButton.setOnAction((ActionEvent event) -> {
            File file = fileChooser.showOpenDialog(primaryStage);
            if(file != null)
            {
                loadFile(file);
                Settings.save(fileChooserDir, file.getParent()); //save the path
            }
        });
        
        //listen for key events on the swingnode
        swingNode.setOnKeyPressed((KeyEvent keyEvent) -> {
            if(ppt != null)
                switch(keyEvent.getCode()){
                    case LEFT:
                        ppt.previous();
                        break;
                    case RIGHT:
                        ppt.next();
                        break;
                    default:
                        break;
                }
        });
        
        StackPane.setAlignment(swingNode, Pos.CENTER);
        root.getChildren().add(swingNode);
        
        sceneRoot = (VBox)scene.getRoot();
        
        HBox toolBar = new HBox();
        toolBar.setBackground(purpleBackground);
        toolBar.setPadding(new Insets(5,20,5,20));
        toolBar.setSpacing(20);
        
        //fullscreen button
        Button fullScreenButton = new Button("Window");
        fullScreenButton.setTextFill(buttonTextColor);
        fullScreenButton.setOnAction((ActionEvent fullScrEvent) -> {
            if(isFullScreen)
                primaryStage.setFullScreen(false);
            else
                primaryStage.setFullScreen(true);
        });
        
        Font font = new Font("SansSerif", 15);
        
        Text fontTypeText = new Text("Font Type");
        fontTypeText.setFont(font);
        fontTypeText.setFill(Paint.valueOf("White"));
        
        Text fontSizeText = new Text("Font Size");
        fontSizeText.setFont(font);
        fontSizeText.setFill(Paint.valueOf("White"));
        
        ComboBox fontTypeComboBox = new ComboBox();
        fontTypeComboBox.getItems().addAll("SansSerif", "Serif","Plain");
        fontTypeComboBox.setValue(initFontType);
        fontTypeComboBox.setPromptText("Select Font type");
        fontTypeComboBox.valueProperty().addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
            if(ppt != null)
                ppt.setFontType((String) newValue);
        });

        Spinner fontSizeSpinner = new Spinner();
        fontSizeSpinner.setValueFactory(new SpinnerValueFactory
                .IntegerSpinnerValueFactory(1, 25, initSpinnerValue));
        fontSizeSpinner.valueProperty().addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
            if(ppt != null)
                ppt.setFontSize((int)newValue);
        });
        Stage logStage = new Stage();
        ScrollPane logScrollPane = new ScrollPane();
        LOG_TEXT_VIEW.setEditable(false);
        LOG_TEXT_VIEW.setFont(new Font("Serif", 15));
        logScrollPane.setContent(LOG_TEXT_VIEW);
        Scene logScene = new Scene(logScrollPane);
        logStage.setScene(logScene);
        logStage.setTitle("LOGS");
        logStage.setMinHeight(LOG_STAGE_HEIGHT);
        logStage.setMinWidth(LOG_STAGE_WIDTH);
        Button showLogButton = new Button("LOG");
        showLogButton.setTextFill(buttonTextColor);
        showLogButton.setOnAction((ActionEvent event) -> {
            if(!logStage.isShowing())
            {
                logStage.showAndWait();
            }else{
                logStage.close();
            }
        });
        
        toolBar.getChildren().addAll(importButton, fullScreenButton,
                fontTypeText, fontTypeComboBox,
                fontSizeText, fontSizeSpinner, showLogButton);
        ((VBox)scene.getRoot()).getChildren().addAll(toolBar);
        VBox.setVgrow(root, Priority.ALWAYS);
        ((VBox)scene.getRoot()).getChildren().add(root);
        ((VBox) scene.getRoot()).getChildren().add(bottomPanel);
        
        scene.widthProperty().addListener(new ChangeListener<Number>(){
            @Override 
            public void changed(ObservableValue<? extends Number> observableValue,
                    Number oldSceneWidth, Number newSceneWidth) {
                if(ppt != null)
                    ppt.setPageWidth(newSceneWidth.doubleValue());
            }
        });
        scene.heightProperty().addListener(new ChangeListener<Number>() {
            @Override 
            public void changed(ObservableValue<? extends Number> observableValue,
                    Number oldSceneHeight, Number newSceneHeight) {
                if(ppt != null)
                    ppt.setPageHeight(newSceneHeight.doubleValue());
            }
        });
        
        primaryStage.setTitle("Twilight");
        primaryStage.setScene(scene);
        primaryStage.setMinHeight(height);
        primaryStage.setMinWidth(width);
        primaryStage.setFullScreen(true);
        
        //get saved height and width of last program run
        double savedWidth = Double.parseDouble(Settings.get("StageWidth", ""+width+""));
        double savedHeight = Double.parseDouble(Settings.get("StageHeight", ""+height+""));
        
        //primaryStage.setWidth(savedWidth);
        //primaryStage.setHeight(savedHeight);
        primaryStage.fullScreenProperty().addListener(new ChangeListener(){
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                isFullScreen = (boolean)newValue;
                if(primaryStage.isFullScreen())
                {
                    fullScreenButton.setText("Window");
                    if(ppt != null)
                        ppt.setFullScreen();
                }
                else
                {
                    fullScreenButton.setText("FullScreen");
                    if(ppt != null)
                        ppt.setWindow();
                }
                
            }
        });
        primaryStage.setResizable(false);
        primaryStage.show();
        
        isFullScreen = primaryStage.isFullScreen();
        
        primaryStage.setOnCloseRequest((WindowEvent event) -> {
            //stop speech thread
            if(speech != null && speech.isAlive())
                speech.stop();
                
            Double currentWidth = primaryStage.getWidth();
            Double currentHeight = primaryStage.getHeight();
            
            Settings.save("StageHeight", currentHeight.toString());
            Settings.save("StageWidth", currentWidth.toString());
            //save to prefs using the Settings class
            Settings.save("FontSize", fontSizeSpinner.getValue().toString());
            Settings.save("FontType", fontTypeComboBox.getValue().toString());
        });
    }
    
    private void loadFile(File file){
        try {
            ppt = new Presentation(file);
            Dimension size = ppt.getPageSize();
            stage.setMinWidth(size.getWidth());
            stage.setMinHeight(size.getHeight());
            swingNode.setContent(ppt);
            if(isFullScreen)
                ppt.setFullScreen();
            else
                ppt.setWindow();
        } catch (Exception ex) {
            Logger.getLogger(Twilight.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
        
    public void act(String result){
        if(ppt != null){
            String[] words = result.split(" "); //sometimes sphnix detects multiple words
            for(String word: words){
                queueOfWords.add(word); //add each word that was detected by sphinx
            }   
        }
    }
    
    public void startResultThread(){
        //alive?
        if(resultThread != null && resultThread.isAlive())
            return;
        
        resultThread = new Thread(() -> {
            while(true){
                String word;
                try{
                    word = (String)queueOfWords.take(); //this will block untill a word is avilable in the queue
                    ppt.highlight(word);
                    System.out.println("Highlight "+word);
                }catch(NoSuchElementException e){
                    //do nothing
                } catch (InterruptedException ex) {
                    Logger.getLogger(Twilight.class.getName()).log(Level.SEVERE, null, ex);
                }
                
            }
        });
        
        resultThread.start();
    }
    
    public void log(String log){
        LOG_TEXT_VIEW.setText(LOG_TEXT_VIEW.getText()+log+"\n");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
    
    
    
}
