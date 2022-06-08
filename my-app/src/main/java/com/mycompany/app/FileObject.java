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
    private int                     numImports;
    private int                     numComments;

    // ------------------------------ Builders ----------------------------------
    
    public FileObject( String filepath, int version ){
        this.filepath = filepath;
        this.version = version;
    }

    public FileObject( String filepath, int version, String buggyness ){
        this.filepath = filepath;
        this.version = version;
        this.buggyness = buggyness;
    }


    public FileObject(  String filepath, int version, int AGE, int LOC, int linesAdded, int linesDeleted, int linesReplaced, 
                        int changeSetSize, String AUTHOR, int numImports, int numComments ){
        this.filepath = filepath;
        this.version = version;
        this.AGE = AGE;
        this.LOC = LOC;
        this.LOC_TOUCHED = linesAdded + linesDeleted + linesReplaced;
        this.linesAdded = linesAdded;
        this.linesDeleted = linesDeleted;
        this.CHURN = linesAdded - linesDeleted;
        this.AUTHOR = AUTHOR;
        this.CHANGE_SET_SIZE = changeSetSize;
        this.numImports = numImports;
        this.numComments = numComments;
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
    public int getLOC_TOUCHED(){
        return this.LOC_TOUCHED;
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
    public int getNumComments(){
        return this.numComments;
    }
    public int getNumImports(){
        return this.numImports;
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

    public void classifyDummy(){
        this.buggyness = "No";
    }


    
    
}