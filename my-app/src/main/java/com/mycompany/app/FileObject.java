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
    private ArrayList<String>       metrics = null;
    private String                  added_LOC;
    private String                  deleted_LOC;
    private String                  LOC;
    private String                  LOC_TOUCHED;
    private String                  AGE;
    private String                  AUTHOR;
    private String                  CHURN;

    // ------------------------------ Builders ----------------------------------
    
    public FileObject( String filepath, int version, String buggyness ){
        this.metrics = new ArrayList<String>();
        this.filepath = filepath;
        this.version = version;
        this.buggyness = buggyness;
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
    public void setMetrics( ArrayList<String> metrics ){
        this.metrics = metrics;
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
    public ArrayList<String> getMetrics(){
        return this.metrics;
    }

    // ------------------------------ Methods ----------------------------------
    
}