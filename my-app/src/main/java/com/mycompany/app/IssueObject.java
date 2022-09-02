package com.mycompany.app;
import java.util.ArrayList;

public class IssueObject{

    // ------------------------------ Attributes ------------------------------

    private ArrayList<CommitObject>     commits = null;
    private String                      ticketID;
    private String                      resolutionDate;
    private String                      creationDate;
    private ArrayList<String>           affectedVersions = null;
    private ArrayList<Integer>          avs;    // Affected Versions indexes
    private int                         iv;     // Injected Version  index
    private int                         ov;     // Opening Versions  index
    private int                         fv;     // Fixed Versions    index

    // ------------------------------ Builders --------------------------------

    public IssueObject(){
        this.commits = new ArrayList<>();
        this.affectedVersions = new ArrayList<>();
    }

    public IssueObject( String ticketID ){
        this.ticketID = ticketID;
        this.commits = new ArrayList<>();
        this.affectedVersions = new ArrayList<>();
    }

    public IssueObject( String ticketID, String resolutionDate, String creationDate, ArrayList<String> affectedVersions ){
        this.commits = new ArrayList<>();
        this.ticketID = ticketID;
        this.resolutionDate = resolutionDate;
        this.creationDate = creationDate;
        this.affectedVersions = affectedVersions;
        this.avs = new ArrayList<>();
    }

    // ------------------------------ Getters --------------------------------

    public String getTicketID(){
        return this.ticketID;
    }
    public String getResolutionDate(){
        return this.resolutionDate;
    }
    public ArrayList<String> getAffectedVersions(){
        return this.affectedVersions;
    }
    public ArrayList<CommitObject> getCommits(){
        return this.commits;
    }
    public String getCreationDate(){
        return this.creationDate;
    }
    public int getFv(){
        return this.fv;
    }
    public int getOv(){
        return this.ov;
    }
    public int getIv(){
        return this.iv;
    }
    public ArrayList<Integer> getAvs(){
        return this.avs;
    }

    // ------------------------------ Setters --------------------------------

    public void setTicketID( String ticketID ){
        this.ticketID = ticketID;
    }
    public void setResolutionDate( String resolutionDate ){
        this.resolutionDate = resolutionDate;
    }
    public void setAffectedVersions( ArrayList<String> affectedVersions ){
        this.affectedVersions = affectedVersions;
    }
    public void setCommits( ArrayList<CommitObject> commits ){
        this.commits = commits;
    }
    public String setCreationDate(){
        return this.creationDate;
    }
    public void setFv( int fv ){
        this.fv = fv;
    }
    public void setOv( int ov ){
        this.ov = ov;
    }
    public void setIv( int iv ){
        this.iv = iv;
    }
    public void setAvs( ArrayList<Integer> avs ){
        this.avs = avs;
    }

    // ------------------------------ Methods --------------------------------
    
    public void append( CommitObject commit ){
        this.commits.add( commit );
    }

    public void clean(){
        commits.removeIf( commit -> commit.getFiles().isEmpty() );
    }

    public void classify(){
        for( CommitObject commitObject : this.commits ){
            commitObject.classify();
        }
    }

}