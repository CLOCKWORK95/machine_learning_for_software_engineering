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
    public int getMAX_CHANGE_SET() {
        return maxChangeSet;
    }
    public void setMAX_CHANGE_SET(int maxChangeSet) {
        this.maxChangeSet = maxChangeSet;
    }
    public int getAVG_CHANGE_SET() {
        return avgChangeSet;
    }
    public void setAVG_CHANGE_SET(int avgChangeSet) {
        this.avgChangeSet = avgChangeSet;
    }
    public int getAVG_LOC_ADDED() {
        return avgLocAdded;
    }
    public void setAVG_LOC_ADDED(int avgLocAdded) {
        this.avgLocAdded = avgLocAdded;
    }
    public int getMAX_LOC_ADDED() {
        return maxLocAdded;
    }
    public void setMAX_LOC_ADDED(int maxLocAdded) {
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
    public int getLOC_TOUCHED(){
        return this.locTouched;
    }
    public void setLOC_TOUCHED( int locTouched ){
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
        this.locTouched += oldMetrics.getLOC_TOUCHED();
        // Sum up all LOC ADDED within the release (average will be computed in a second time by dividing by NR).
        this.avgLocAdded += oldMetrics.getAVG_LOC_ADDED();
        // Get the oldest version of this file within the release.
        if ( oldMetrics.getAGE() > this.age ){
            this.age = oldMetrics.getAGE();
        }
        // Sum up all LOC reported for this file over all commits within the release (average will be computed in a second time by dividing by NR).
        this.loc += oldMetrics.getLOC();
        // Update MAX LOC ADDED only if it is greater than the max loc added reached by previous commits within the release.
        if (!( this.maxLocAdded > oldMetrics.getMAX_LOC_ADDED())){
            this.maxLocAdded = oldMetrics.getMAX_LOC_ADDED();
        }
        // Sum up all CHANGE SET SIZE over commits within the release (average will be computed in a second time by dividing by NR).
        this.avgChangeSet += oldMetrics.getAVG_CHANGE_SET();
        // Update MAX CHANGE SET only if it is greater than the max chg set reached by previous commits within the release.
        if (!( this.maxChangeSet> oldMetrics.getMAX_CHANGE_SET())){
            this.maxChangeSet = oldMetrics.getMAX_CHANGE_SET();
        }
        // Sum up all LOC reported for this file over all commits within the release (average will be computed in a second time by dividing by NR).
        this.numImports += oldMetrics.getNumImports();
        // Sum up all LOC reported for this file over all commits within the release (average will be computed in a second time by dividing by NR).
        this.numComments += oldMetrics.getNumComments();

    }

    
}
