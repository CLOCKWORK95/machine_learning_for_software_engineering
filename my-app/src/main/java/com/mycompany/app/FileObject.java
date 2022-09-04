package com.mycompany.app;

public class FileObject{

    // ------------------------------ Attributes --------------------------------

    private String                  filepath;
    private int                     version;
    private String                  buggyness;
    private int                     linesAdded;
    private int                     linesDeleted;
    private int                     loc;
    private int                     locTouched;
    private int                     age;
    private String                  author;
    private int                     churn;
    private int                     changeSetSize;
    private int                     numImports;
    private int                     numComments;

    // ------------------------------ Builders ----------------------------------
    
    public FileObject( String filepath, int version, String buggyness ){
        this.filepath = filepath;
        this.version = version;
        this.buggyness = buggyness;
    }


    public FileObject(  String filepath, int version, int age, int loc, int linesAdded, int linesDeleted, int linesReplaced ){
        this.filepath = filepath;
        this.version = version;
        this.age = age;
        this.loc = loc;
        this.locTouched = linesAdded + linesDeleted + linesReplaced;
        this.linesAdded = linesAdded;
        this.linesDeleted = linesDeleted;
        this.churn = linesAdded - linesDeleted;
    }

    // ------------------------------ Setters ----------------------------------

    public void setChangeSetSize( int changeSetSize ){
        this.changeSetSize = changeSetSize;
    }

    public void setAuthor( String author ){
        this.author = author;
    }

    public void setNumImports( int numImports ){
        this.numImports = numImports;
    }

    public void setNumComments( int numComments ){
        this.numComments = numComments;
    }

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
        return this.loc;
    }
    public int getLocTouched(){
        return this.locTouched;
    }
    public int getAGE(){
        return this.age;
    }
    public int getCHURN() {
        return this.churn;
    }
    public int getChangeSetSize() {
        return this.changeSetSize;
    }
    public String getAUTHOR() {
        return this.author;
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


    
    
}