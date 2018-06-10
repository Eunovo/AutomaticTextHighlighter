/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.novo.twilight;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.TreeMap;
import javax.swing.JPanel;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;



/**
 *
 * @author Eunovo
 */
public class Presentation extends JPanel {
    
    private final File file;
    
    private BufferedImage OSC;
    
    private SlideShow ppt;
    
    private int currentIndex = 0;
    
    private boolean isXSLF = false;
    
    private int maxWidth = 0;
    private final double X_MARGIN = 0.1;
    private final double Y_MARGIN = 0.1;
    
    private final ArrayList<String> wordsToHighlight = new ArrayList();
    
    //initialize defaults
    private int fontSize = (int)Double.parseDouble(Settings.get("FontSize", "18"));
    private String fontType = Settings.get("FontType", Font.SANS_SERIF);
    private String highlightColor = Settings.get("HighlightColor", "yellow");
    
    private final ArrayList<XSLFSlide> xslfSlides = new ArrayList();
    private final ArrayList<HSLFSlide> hslfSlides = new ArrayList();
    
    public Presentation(File file) throws Exception{
        this.file = file;
        parseFile();
    }
    
    
    /**
     * it extracts the presentation slides and stores the in xslfSlides for xslf files
     * and hslfSlides for hslf files
     * @throws Exception if the file is not a presentation file
     */
    private void parseFile() throws Exception{
        String fileName = file.getName();
        String[] tokens = fileName.split("\\.");
        if(tokens.length < 2 )//cannot determine file extention
            throw new Exception("Could not parse file");
        String ext = tokens[tokens.length -1]; //pick the last item in the array
        ext = ext.toLowerCase(); //switch to lower case for comparison
        if(ext.equals("pptx"))
        { 
            isXSLF = true;
            //use XSLF
            ppt = new XMLSlideShow( new FileInputStream(file));
            xslfSlides.addAll(ppt.getSlides());
        }
        else if(ext.equals("ppt"))
        {
            isXSLF = false;
            ppt = new HSLFSlideShow( new FileInputStream(file));
            hslfSlides.addAll(ppt.getSlides());
        }else
        {
            //not a valid powerpoint file
            throw new Exception("Not a valid powerpoint file");
        }
    }
    
    
    /**
     * searches for wordToMatch in all slides
     * @param wordToMatch
     * @return the word if found, null otherwise
     */
    public String findMatch(String wordToMatch){
        //return the closest word to wordToMatch in slide
        wordToMatch = wordToMatch.toLowerCase();
        if(isXSLF){
            for(XSLFSlide slide: xslfSlides){
                ArrayList<String> lines = getLinesFromXSLFSlide(slide);
                for(String line: lines){
                    String[] words = line.split(" ");
                    for(String word: words){
                        if(word.toLowerCase().equals(wordToMatch))
                            return word;
                    }
                } 
            }
        }
        else{
            for(HSLFSlide slide: hslfSlides){
                ArrayList<String> lines = getLinesFromHSLFSlide(slide);
                for(String line: lines){
                    String[] words = line.split(" ");
                    for(String word: words){
                        if(word.toLowerCase().equals(wordToMatch))
                            return word;
                    }
                } 
            }
        }
        return null;
    }
    
    /**
     * for XSLF slides
     * @param slide from which to extract lines
     * @return a String[] containing all the lines in the slide
     */
    private ArrayList<String> getLinesFromXSLFSlide(XSLFSlide slide){
        ArrayList<String> finalLines = new ArrayList();
        for (XSLFShape shape: slide.getShapes()) {
            if (shape instanceof XSLFTextShape) {
                XSLFTextShape textShape = (XSLFTextShape)shape;
                String text = textShape.getText();
                String[] lines = text.split("\n|\r");//split text with new-line char or return char
                for(String line: lines){
                    finalLines.add(line);
                }
            }
        }
        return finalLines;
    }
    
    /**
     * for HSLF slides
     * @param slide from which to extract lines
     * @return a String[] containing all the lines in the slide
     */
    private ArrayList<String> getLinesFromHSLFSlide(HSLFSlide slide){
        ArrayList<String> finalLines = new ArrayList();
        for (HSLFShape shape: slide.getShapes()) {
            if (shape instanceof HSLFTextShape) {
                HSLFTextShape textShape = (HSLFTextShape)shape;
                String text = textShape.getText();
                String[] lines = text.split("\n|\r");//split text with new-line char or return char
                for(String line: lines){
                    finalLines.add(line);
                }                    
            }
        }
        return finalLines;
    }
    
    /**
     * highlights a word if found in the current slide
     * @param word to highlight in the slide
     */
    public void highlight(String word){
        if(!word.equals("")){
            System.out.println("Attempt to highlight "+word);
            //highlight the first occurence of the word
            wordsToHighlight.add(word.toLowerCase());
            repaint();
        }
    }
    
    /**
     * creates the offScreenCanvas which acts as the background
     */
    private void createOSC(){
        OSC = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics osg = OSC.createGraphics();
        osg.setColor(Color.WHITE);
        osg.fillRect(getX(), getY(), getWidth(), getHeight());
        osg.dispose();
    }
    
    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);
        
        TreeMap highlightedWords = new TreeMap();
        
        Color highlightColor = Color.getColor(this.highlightColor);
        
        int lineGap = 10; //gp between lines
        int textHeight; //text height
        int wordSpace; //space between words
        int maxLineWidth;
        maxLineWidth = this.getWidth() - (int)(X_MARGIN * this.getWidth() * 2);
        
        Graphics2D g2 = (Graphics2D)g;
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        
        if(OSC == null || getWidth() != OSC.getWidth() || getHeight() != OSC.getHeight())
            createOSC();
        
        Graphics2D osg = OSC.createGraphics();
        
        g2.drawImage(OSC, 0, 0, this);
        
        Font font = new Font(fontType, Font.BOLD, fontSize);
        g2.setFont(font);
        FontMetrics fm = g.getFontMetrics(font);
        textHeight  = fm.getAscent() + fm.getDescent();
        wordSpace = fm.stringWidth(" ");
        
        int x = this.getX() + (int)(X_MARGIN * this.getWidth()) ;//+ (int)textShape.getAnchor().getX();
        int y = this.getY() + (int)(Y_MARGIN * this.getHeight()) ;//+ (int)textShape.getAnchor().getY();
        
        ArrayList<String> lines;
        if(isXSLF){
            XSLFSlide slide = xslfSlides.get(currentIndex);
            lines = getLinesFromXSLFSlide(slide);
        }
        else{
            HSLFSlide slide = hslfSlides.get(currentIndex);
            lines = getLinesFromHSLFSlide(slide);
        }
        for(String line: formatLines(lines, fm, maxLineWidth)){
            g2.setColor(Color.BLACK);
            int lineWidth = fm.stringWidth(line);
            g2.drawString(line, x, y);
            String[] words = line.split(" ");
            int wordX = x; //current word's x position
            for(String word: words){
                //Color fillColor = new Color(OSC.getRGB(wordX, y - textHeight ));
                Color fillColor = Color.WHITE;
                Object record = highlightedWords.get(word.toLowerCase());
                int nTimes = 0;
                if(record != null){
                    nTimes = (int)record;
                }
                int count = count(word);
                if( nTimes < count && wordsToHighlight.contains(word.toLowerCase()) )
                {
                    fillColor = Color.yellow;//highlightColor;
                    if(nTimes > 0){ 
                        //if word has been highlighted before
                        highlightedWords.put(word.toLowerCase(), nTimes ++);
                    }else //if word has not been highlighted before
                        highlightedWords.put(word.toLowerCase(), 1);
                }
                int wordWidth = fm.stringWidth(word);
                int spaceWidth = fm.stringWidth(" ");
                osg.setColor(fillColor);
                osg.fillRect(wordX, y - textHeight+fm.getDescent(), wordWidth+spaceWidth, textHeight);//draw the background in the offscreen canvas
                wordX = wordX + wordWidth + wordSpace; 
            }
            y = y + textHeight + lineGap;
        }                            
    }
    
    /**
     * 
     * @param wordToCount to count
     * @return the number of times wordToCount is to be highlighted
     */
    public int count(String wordToCount){
        int count = 0;
        for(String word: wordsToHighlight){
            if(word.toLowerCase().equals(wordToCount.toLowerCase())){
                count = count + 1;
            }
        }
        return count;
    }
    
    /**
     * make the slide occupy the whole screen
     */
    public void setFullScreen(){
        this.repaint();
    }
    
    /**
     * arrange the lines for display
     * @param raw unformatted lines
     * @param fm font metrics for measuring the length of strings 
     * with the current font
     * @param maxLength of line
     * @return a list of the formatted lines
     */
    private ArrayList<String> formatLines(ArrayList<String> rawLines, FontMetrics fm, int maxLength){
        ArrayList<String> newLines = new ArrayList();
        for(String line: rawLines){
            line = line.replace("\t", "");//remove all tab characters
            line = line.replaceAll("\\s{2,}", " ").trim(); //replace all multiple spaces with one space
            int lineWidth = fm.stringWidth(line);
            if(lineWidth <= maxLength)
                newLines.add(line); //if length of line can fit, add it directly to the new list
            else{
                //break the line into two smaller lines
                ArrayList<String> pieces = breakLine(line, fm, maxLength);
                newLines.addAll(pieces);//add them to new List
            }
        }
        return newLines;
    }
    
    /**
     * recursively break a line until each piece is less than the maximum length
     * specified
     * @param line current line to break
     * @param fm font metrics for measuring the length of strings
     * with the current font
     * @param maxLength of line
     * @return a list containing the pieces of the line
     */
    private ArrayList<String> breakLine(String line, FontMetrics fm, int maxLength){
        ArrayList<String> finalLines = new ArrayList(); //this will be returned
        ArrayList<String> newLines = new ArrayList();
        int lineWidth = fm.stringWidth(line);
        if(lineWidth <= maxLength)
        {
            finalLines.add(line);
        }else{
            int halfWidth = lineWidth/2;
            String[] words = line.split(" "); //extract all words
            //add words one by one to a new line until its width is close to half width
            String newLine = "";
            int currentLength = 0;
    
            for(String word: words){
                if(currentLength < halfWidth)
                    newLine = newLine + " "+word; //not long enough
                else 
                {
                    newLines.add(newLine);//long enough so add the line
                    newLine = ""; //reset new line

                }
                currentLength = fm.stringWidth(newLine);
            }
            if(!newLine.equals(""))
                newLines.add(newLine);

            for(String piece: newLines){
                if(fm.stringWidth(piece) > maxLength)
                {
                    ArrayList<String> pieces = breakLine(piece, fm, maxLength);//recursively call breakLine
                    finalLines.addAll(pieces);
                } 
                else{
                    finalLines.add(piece);
                }
            }
        }
        return finalLines;
    }
    
    public void setPageWidth(double width){
       Dimension pptSize = ppt.getPageSize();
       this.setSize(pptSize); 
       ppt.setPageSize(new Dimension((int)width, pptSize.height));
       repaint();
    }
    
    public void setPageHeight(double height){
       Dimension pptSize = ppt.getPageSize();
       ppt.setPageSize(new Dimension(pptSize.width, (int)height));
       repaint();
    }
    
    public void setPageSize(double width, double height){
        ppt.setPageSize(new Dimension((int)width, (int)height));
        repaint();
    }
    
    public Dimension getPageSize(){
        return ppt.getPageSize();
    }
    
    public void setWindow(){
        repaint();
    }
    
    /**
     * Set current slide to next slide
     */
    public void next(){
        
        if( (isXSLF && xslfSlides.size()-1 > currentIndex) || 
                (!isXSLF && hslfSlides.size()-1 > currentIndex) )
        {    
            currentIndex ++;
            wordsToHighlight.clear(); //slide changed, clear highlighted words
            fillOSC(Color.WHITE);
            repaint();
        }
    }
    
    /**
     * Set current slide to previous slide
     */
    public void previous(){
        if(currentIndex > 0)
        {
            currentIndex --;
            wordsToHighlight.clear(); //slide changed, clear highlighted words
            fillOSC(Color.WHITE);
            repaint();
        }
    }
    
    private void fillOSC(Color color){
        
        if(OSC != null)
        {
            Graphics g = OSC.getGraphics();
            //set graphics color
            g.setColor(color);
            //fill rect of panel size with given color
            g.fillRect(0, 0, getWidth(), getHeight()); 
            g.dispose();
        }
              
    }
    
    public void setFontSize(int newFontSize){
        fontSize = newFontSize;
        repaint();
    }
    
    public void setFontType(String newFontType){
        fontType = newFontType;
        repaint();
    }
    
}
