package com.mycompany.app;
import java.util.ArrayList;

public class Metrics {

    private int                 version;
    private String              filepath;
    private int                 nr;
    private ArrayList<String>   authors = new ArrayList<>();
    private int                 loc;
    private int                 locTouched;
    private int                 age;
    private int                 churn;
    private int                 maxLocAdded;
    private int                 avgLocAdded;
    private int                 avgChangeSet;
    private int                 maxChangeSet;
    private int                 numImports;
    private int                 numComments;
    private String              buggyness;

    //--------------------------------- GETTERS & SETTERS--------------------------------------
    
    public int getVersion() {
        return version;
    }
    public void setVersion(int version) {
        this.version = version;
    }
    public String getBUGGYNESS() {
        return buggyness;
    }
    public void setBUGGYNESS(String buggyness) {
        this.buggyness = buggyness;
    }
    public int getMaxChangeSetSize() {
        return maxChangeSet;
    }
    public void setMaxChangeSetSize(int maxChangeSet) {
        this.maxChangeSet = maxChangeSet;
    }
    public int getAvgChangeSetSize() {
        return avgChangeSet;
    }
    public void setAvgChangeSetSize(int avgChangeSet) {
        this.avgChangeSet = avgChangeSet;
    }
    public int getAvgLocAdded() {
        return avgLocAdded;
    }
    public void setAvgLocAdded(int avgLocAdded) {
        this.avgLocAdded = avgLocAdded;
    }
    public int getMaxLocAdded() {
        return maxLocAdded;
    }
    public void setMaxLocAdded(int maxLocAdded) {
        this.maxLocAdded = maxLocAdded;
    }
    public int getCHURN() {
        return churn;
    }
    public void setCHURN(int churn) {
        this.churn = churn;
    }
    public int getAGE() {
        return age;
    }
    public void setAGE(int age) {
        this.age = age;
    }
    public int getLocTouched(){
        return this.locTouched;
    }
    public void setLocTouched( int locTouched ){
        this.locTouched = locTouched;
    }
    public int getLOC() {
        return loc;
    }
    public void setLOC(int loc) {
        this.loc = loc;
    }
    public ArrayList<String> getAUTHORS() {
        return authors;
    }
    public void setAUTHORS( ArrayList<String> authors ) {
        this.authors = authors;
    }
    public int getNR() {
        return nr;
    }
    public void setNR(int nr) {
        this.nr = nr;
    }
    public String getFilepath() {
        return filepath;
    }
    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }
    public int getNumComments(){
        return this.numComments;
    }
    public void setNumComments( int numComments ){
        this.numComments = numComments;
    }
    public int getNumImports(){
        return this.numImports;
    }
    public void setNumImports( int numImports ){
        this.numImports = numImports;
    }
    //-------------------------------------------- METHODS --------------------------------------------------

    public void appendAuthor( String author ){
        this.authors.add(author);
    }

    public void update( Metrics oldMetrics ) {
        // Sum up all NR (commits touching this file within the release).
        this.nr += oldMetrics.getNR();
        /* Append this author to the list of authors who have worked at this file within the release.
           At the end, the size of this array will represent the total number of authors.*/
        if ( !oldMetrics.getAUTHORS().contains(this.authors.get(0)) ){
            ArrayList<String> updateAuthors = oldMetrics.getAUTHORS();
            updateAuthors.add( this.authors.get(0) );
            this.authors = updateAuthors;
        } else { this.authors = oldMetrics.getAUTHORS(); }
        // Sum up all CHURN (loc added - loc deleted) within the release.
        this.churn += oldMetrics.getCHURN();
        // Sum up all LOC TOUCHED within the release.
        this.locTouched += oldMetrics.getLocTouched();
        // Sum up all LOC ADDED within the release (average will be computed in a second time by dividing by NR).
        this.avgLocAdded += oldMetrics.getAvgLocAdded();
        // Get the oldest version of this file within the release.
        if ( oldMetrics.getAGE() > this.age ){
            this.age = oldMetrics.getAGE();
        }
        // Sum up all LOC reported for this file over all commits within the release (average will be computed in a second time by dividing by NR).
        this.loc += oldMetrics.getLOC();
        // Update MAX LOC ADDED only if it is greater than the max loc added reached by previous commits within the release.
        if (!( this.maxLocAdded > oldMetrics.getMaxLocAdded())){
            this.maxLocAdded = oldMetrics.getMaxLocAdded();
        }
        // Sum up all CHANGE SET SIZE over commits within the release (average will be computed in a second time by dividing by NR).
        this.avgChangeSet += oldMetrics.getAvgChangeSetSize();
        // Update MAX CHANGE SET only if it is greater than the max chg set reached by previous commits within the release.
        if (!( this.maxChangeSet> oldMetrics.getMaxChangeSetSize())){
            this.maxChangeSet = oldMetrics.getMaxChangeSetSize();
        }
        // Sum up all LOC reported for this file over all commits within the release (average will be computed in a second time by dividing by NR).
        this.numImports += oldMetrics.getNumImports();
        // Sum up all LOC reported for this file over all commits within the release (average will be computed in a second time by dividing by NR).
        this.numComments += oldMetrics.getNumComments();

    }

    
}
