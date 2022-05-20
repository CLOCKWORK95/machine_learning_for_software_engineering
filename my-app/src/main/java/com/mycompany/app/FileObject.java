package com.mycompany.app;
import java.util.*;
import java.util.ArrayList;
import org.eclipse.jgit.revwalk.RevCommit;
import java.time.LocalDate;
import java.time.ZoneId;

public class FileObject{

    // ------------------------------ Attributes --------------------------------

    private String                  filepath;
    private int                     version;
    private String                  buggyness;
    private int                     linesAdded;
    private int                     linesDeleted;
    private int                     LOC;
    private int                     LOC_TOUCHED;
    private int                     AGE;
    private String                  AUTHOR;
    private int                     CHURN;
    private int                     CHANGE_SET_SIZE;
    private String                  FILETEXT;

    // ------------------------------ Builders ----------------------------------
    
    public FileObject( String filepath, int version, String buggyness ){
        this.filepath = filepath;
        this.version = version;
        this.buggyness = buggyness;
    }


    public FileObject( String filepath, int version, String FILETEXT, int AGE, int LOC, int linesAdded, int linesDeleted, int changeSetSize ){
        this.filepath = filepath;
        this.version = version;
        this.FILETEXT = FILETEXT;
        this.AGE = AGE;
        this.LOC = LOC;
        this.linesAdded = linesAdded;
        this.linesDeleted = linesDeleted;
        this.CHURN = linesAdded + linesDeleted;
        this.AUTHOR = "";
        this.CHANGE_SET_SIZE = changeSetSize;
    }

    // ------------------------------ Setters ----------------------------------

    public void setFilepath( String filepath){
        this.filepath = filepath;
    }
    public void setVersion( int version){
        this.version = version;
    }
    public void setBuggyness( String buggyness ){
        this.buggyness = buggyness;
    }

    // ------------------------------ Getters ----------------------------------

    public String getFilepath(){
        return this.filepath;
    }
    public int getVersion(){
        return this.version;
    }
    public String getBuggyness(){
        return this.buggyness;
    }
    public int getLOC(){
        return this.LOC;
    }
    public int getAGE(){
        return this.AGE;
    }
    public int getCHURN() {
        return this.CHURN;
    }
    public int getChangeSetSize() {
        return this.CHANGE_SET_SIZE;
    }
    public String getAUTHOR() {
        return this.AUTHOR;
    }
    public int getLinesAdded(){
        return this.linesAdded;
    }
    public int getLinesDeleted(){
        return this.linesDeleted;
    }

    // ------------------------------ Methods ----------------------------------

    public void classify( CommitObject commitObject ){
        this.buggyness = "Yes";
        if ( ( commitObject.getVersion() >= commitObject.getIssue().getFv() ) ){
            this.buggyness = "No";
        }
        if ( ( commitObject.getVersion() < commitObject.getIssue().getIv() ) ){
            this.buggyness = "No";
        } 
    }


    
    
}