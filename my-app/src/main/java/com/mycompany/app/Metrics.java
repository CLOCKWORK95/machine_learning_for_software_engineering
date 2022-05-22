package com.mycompany.app;

import java.util.ArrayList;

public class Metrics {

    private int                 version;
    private String              filepath;
    private int                 NR;
    private ArrayList<String>   AUTHORS = new ArrayList<>();
    private int                 LOC;
    private int                 AGE;
    private int                 CHURN;
    private int                 MAX_LOC_ADDED;
    private int                 AVG_LOC_ADDED;
    private int                 AVG_CHANGE_SET;
    private int                 MAX_CHANGE_SET;
    private String              BUGGYNESS;

    //--------------------------------- GETTERS & SETTERS--------------------------------------
    
    public int getVersion() {
        return version;
    }
    public void setVersion(int version) {
        this.version = version;
    }
    public String getBUGGYNESS() {
        return BUGGYNESS;
    }
    public void setBUGGYNESS(String bUGGYNESS) {
        this.BUGGYNESS = bUGGYNESS;
    }
    public int getMAX_CHANGE_SET() {
        return MAX_CHANGE_SET;
    }
    public void setMAX_CHANGE_SET(int mAX_CHANGE_SET) {
        this.MAX_CHANGE_SET = mAX_CHANGE_SET;
    }
    public int getAVG_CHANGE_SET() {
        return AVG_CHANGE_SET;
    }
    public void setAVG_CHANGE_SET(int aVG_CHANGE_SET) {
        this.AVG_CHANGE_SET = aVG_CHANGE_SET;
    }
    public int getAVG_LOC_ADDED() {
        return AVG_LOC_ADDED;
    }
    public void setAVG_LOC_ADDED(int aVG_LOC_ADDED) {
        this.AVG_LOC_ADDED = aVG_LOC_ADDED;
    }
    public int getMAX_LOC_ADDED() {
        return MAX_LOC_ADDED;
    }
    public void setMAX_LOC_ADDED(int mAX_LOC_ADDED) {
        this.MAX_LOC_ADDED = mAX_LOC_ADDED;
    }
    public int getCHURN() {
        return CHURN;
    }
    public void setCHURN(int cHURN) {
        this.CHURN = cHURN;
    }
    public int getAGE() {
        return AGE;
    }
    public void setAGE(int aGE) {
        this.AGE = aGE;
    }
    public int getLOC() {
        return LOC;
    }
    public void setLOC(int lOC) {
        this.LOC = lOC;
    }
    public ArrayList<String> getAUTHORS() {
        return AUTHORS;
    }
    public void setAUTHORS( ArrayList<String> AUTHORS ) {
        this.AUTHORS = AUTHORS;
    }
    public int getNR() {
        return NR;
    }
    public void setNR(int nR) {
        this.NR = nR;
    }
    public String getFilepath() {
        return filepath;
    }
    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }
    
    //-------------------------------------------- METHODS --------------------------------------------------

    public void appendAuthor( String author ){
        this.AUTHORS.add(author);
    }

    public void update( Metrics oldMetrics ) {
        // Sum up all NR (commits touching this file within the release).
        this.NR += oldMetrics.getNR();
        /* Append this author to the list of authors who have worked at this file within the release.
           At the end, the size of this array will represent the total number of authors.*/
        if ( !oldMetrics.getAUTHORS().contains(this.AUTHORS.get(0)) ){
            ArrayList<String> updateAuthors = oldMetrics.getAUTHORS();
            updateAuthors.add( this.AUTHORS.get(0) );
            this.AUTHORS = updateAuthors;
        } else { this.AUTHORS = oldMetrics.getAUTHORS(); }
        this.CHURN += oldMetrics.getCHURN();
        // Sum up all LOC ADDED within the release (average will be computed in a second time by dividing by NR).
        this.AVG_LOC_ADDED += oldMetrics.getAVG_LOC_ADDED();
        // Get the oldest version of this file within the release.
        if ( oldMetrics.getAGE() > this.AGE ){
            this.AGE = oldMetrics.getAGE();
        }
        // Sum up all LOC reported for this file over all commits within the release (average will be computed in a second time by dividing by NR).
        this.LOC += oldMetrics.getLOC();
        // Update MAX LOC ADDED only if it is greater than the max loc added reached by previous commits within the release.
        if (!( this.MAX_LOC_ADDED > oldMetrics.getMAX_LOC_ADDED())){
            this.MAX_LOC_ADDED = oldMetrics.getMAX_LOC_ADDED();
        }
        // Sum up all CHANGE SET SIZE over commits within the release (average will be computed in a second time by dividing by NR).
        this.AVG_CHANGE_SET += oldMetrics.getAVG_CHANGE_SET();
        // Update MAX CHANGE SET only if it is greater than the max chg set reached by previous commits within the release.
        if (!( this.MAX_CHANGE_SET> oldMetrics.getMAX_CHANGE_SET())){
            this.MAX_CHANGE_SET = oldMetrics.getMAX_CHANGE_SET();
        }

    }

    
}
