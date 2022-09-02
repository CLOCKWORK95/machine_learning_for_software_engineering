package com.mycompany.app;
import java.util.*;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import java.util.stream.*;
import java.util.Collection;
import java.util.Collections;
import org.json.JSONException;
import java.util.ArrayList;
import org.eclipse.jgit.api.errors.GitAPIException;
import java.io.IOException;
import org.eclipse.jgit.revwalk.filter.MessageRevFilter;
import com.google.common.collect.Iterables; 
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import me.tongfei.progressbar.ProgressBar;
import java.time.LocalDate;
import java.time.ZoneId;

public class IssueLifeCycleManager{

    // ------------------------------ Attributes ------------------------------

    private String                          projectName;
    private String                          projectPath;
    private ArrayList<IssueObject>          issues;
    private ArrayList<IssueObject>          issuesWithAffectedVersions;
    private ArrayList<IssueObject>          issuesWithoutAffectedVersions;
    private GitRepositoryManager            gitRepoManager;
    private JiraTicketManager               jiraTicketManager;
    private Multimap<LocalDate, String>     versionMap;
    private DatasetBuilder                  datasetBuilder;


    // ------------------------------ Builders --------------------------------


    public IssueLifeCycleManager( String projectName, String projectPath ) throws IOException {
        this.projectName = projectName;
        this.projectPath = projectPath;
        this.issues = new ArrayList<>();
        this.issuesWithAffectedVersions = new ArrayList<>();
        this.issuesWithoutAffectedVersions = new ArrayList<>();
        this.gitRepoManager = new GitRepositoryManager( projectName, projectPath );
        this.jiraTicketManager = new JiraTicketManager( projectName.toUpperCase() );
        this.versionMap = MultimapBuilder.treeKeys().linkedListValues().build();
        this.datasetBuilder = new DatasetBuilder( this.projectName );
    }

    // ------------------------------ Getters ---------------------------------

    public String getProjectPath(){
        return this.projectPath;
    }

    public List<IssueObject> getIssues(){
        return this.issues;
    }
    public List<IssueObject> getIssuesWithAffectedVersions(){
        return this.issuesWithAffectedVersions;
    }
    public List<IssueObject> getIssuesWithoutAffectedVersions(){
        return this.issuesWithoutAffectedVersions;
    }
    
    public Multimap<LocalDate, String> getVersionMap(){
        return this.versionMap;
    }
    public DatasetBuilder getDasetBuilder(){
        return this.datasetBuilder;
    }

    // ------------------------------ Methods ---------------------------------

    public void appendIssue( IssueObject issue ){
        this.issues.add( issue );
    }


    public void appendVersionMapEntry( LocalDate key, String value ){
        this.versionMap.put( key, value );
    }


    public Set<LocalDate> getVersionMapKeySet(){
        return this.versionMap.keySet();
    }


    /*  This Method initializes a Multimap <LocalDate,String> (class attribute versionMap)
        which has the purpose to match a date with each of the project releases, giving 
        an integer index to order such releases in time.    */
    public void initializeVersionMap() throws IOException, JSONException {
        this.jiraTicketManager.getVersionsWithReleaseDate( this );
        this.datasetBuilder.setVersionMap( this.versionMap );
    }


    /*  This Method interfaces with the jira ticket manager to retrieve all Jira Tickets
        associated with the specific project this class is handling. 
        Tickets are chosen to be CLOSED issues. For each issue, state infos are maintained 
        through an Issue Object instance which is added into issues array.  */ 
    public void retrieveIssueTickets() throws IOException, JSONException{
        this.jiraTicketManager.getTickets( this );
    }


    /*  This Method is called by Commit Objects associated to this Issue in order to retrieve their own version 
        using the value of the commit local date.  */
    public int getVersionFromLocalDate( LocalDate localDate ){
        int version = -1;
        for( LocalDate date : this.versionMap.keySet()){
            version = Integer.valueOf( Iterables.getLast( versionMap.get(date) ));
            if ( date.isEqual( localDate ) || date.isAfter( localDate ) ){                  
                break;
            }
        }
        return version;
    }


    /*  This Method interfaces with the Git Repository Manager in order to retrieve,
        for each issue in issues array, all related commits (having Issue ID inside the
        commit message).    */ 
    public void logWalk() throws IOException, GitAPIException {
        
        Collection<Ref> allRefs = this.gitRepoManager.getRepository().getAllRefs().values();

        // a RevWalk allows to walk over commits based on some filtering that is defined
 
        ProgressBar pb = new ProgressBar("SCANNING TICKETS", this.issues.size()); 
        pb.start();
        
        for ( IssueObject issue : this.issues ){
            
            pb.step();

            try ( RevWalk revWalk = new RevWalk( this.gitRepoManager.getRepository()) ) {

                revWalk.setRevFilter( MessageRevFilter.create( issue.getTicketID() ) );

                for( Ref ref : allRefs ) {
                    revWalk.markStart( revWalk.parseCommit( ref.getObjectId() ));
                }
                
                for( RevCommit commit : revWalk ) {
                    try{
                        // if the commit has not a parent one just skip it (it would be impossible to retrieve some metrics!!)
                        commit.getParent(0);
                    } catch ( ArrayIndexOutOfBoundsException e ){
                        continue;
                    }
                    LocalDate commitLocalDate = commit.getCommitterIdent().getWhen().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    int version = getVersionFromLocalDate( commitLocalDate );
                    
                    // The following is important : Jira could be not consistent in FV!!! Git has the truth!!!
                    if ( (issue.getFv() < version && (version-issue.getFv()<=5)) )  issue.setFv( version ); 
                   
                    CommitObject commitObject = new CommitObject( commit, issue, version, this.gitRepoManager );
                    issue.append( commitObject );
                    
                    
                }
            } 

        } 

        pb.stop();
    }


    /*  This Method is used in order to set Opening and Fixed Versions for every issue
        that has been retrieved. Actually, those informations are stored into the issue object's
        state as Integer values (in the shape of version's indexes). */
    public void setOpeningAndFixedVersions(){
        for ( IssueObject issue : this.issues ){
            for( LocalDate date : this.versionMap.keySet()){
                issue.setFv( Integer.valueOf( Iterables.getLast( versionMap.get(date) )) );
                if ( date.isEqual( LocalDate.parse( issue.getResolutionDate() ) ) || date.isAfter( LocalDate.parse(issue.getResolutionDate()) ) ){                  
                    break;
                }
            }
            for( LocalDate date : this.versionMap.keySet()){
                issue.setOv( Integer.valueOf( Iterables.getLast( versionMap.get(date) )) );
                if ( date.isEqual( LocalDate.parse( issue.getCreationDate() ) ) || date.isAfter( LocalDate.parse(issue.getCreationDate()) ) ){                  
                    break;
                }
            }
        }
    }


    /*  This Method is used in order to set the Affected Versions indexes for every issue
        that has been retrieved and that maintains the AVs information. 
        Furthermore, issues with declared AVs are separated from those without them, so that
        it will be possible to handle the two groups of issues separately. */
    public void setAffectedVersionsAV(){
        for ( IssueObject issue : this.issues ){           
            // If the current issue has not specified AVs it'll be appended to the array
            // that will be used to perform the Proportion Method.
            if ( issue.getAffectedVersions().size() == 0 ){
                this.issuesWithoutAffectedVersions.add( issue );
            }
            else{
                ArrayList<Integer> avs = new ArrayList<>();
                ArrayList<String> affectedVersions = issue.getAffectedVersions();
                for ( String version : affectedVersions ){
                    for( LocalDate date : this.versionMap.keySet() ){
                        if ( Iterables.get(versionMap.get(date),0).equals( version )){    
                            avs.add( Integer.valueOf( Iterables.getLast( versionMap.get(date) )) );              
                            break;
                        }
                    }
                } 
                // Check if the reported affected versions for the current issue are not coherent 
                // with the reported fixed version and opening version, and populate with all remaining 
                // versions between them.
                avs.removeIf( av -> av >= issue.getFv() );
                if (avs.isEmpty()) this.issuesWithoutAffectedVersions.add( issue ); 
                else if (Collections.min(avs)<=issue.getOv()){
                    int minValue = Collections.min(avs);
                    int maxValue = ( issue.getFv() - 1 );
                    avs = new ArrayList<>( IntStream.rangeClosed(minValue, maxValue).boxed().collect(Collectors.toList()) );
                    issue.setAvs( avs );  
                    this.issuesWithAffectedVersions.add( issue ); 
                }
                else if (Collections.min(avs)>issue.getOv()){
                    int minValue = issue.getOv();
                    int maxValue = ( issue.getFv() - 1 );
                    avs = new ArrayList<>( IntStream.rangeClosed(minValue, maxValue).boxed().collect(Collectors.toList()) );
                    issue.setAvs( avs );  
                    this.issuesWithAffectedVersions.add( issue ); 
                }            
            }
        }
    }


    /*  This Method is used to set the Injected version of every issue having declared affected
        versions. The injected version is set as the minimum between all declared affected versions. */
    public void setInjectedVersionAV(){
        int iv;
        for ( IssueObject issue : this.issuesWithAffectedVersions ){
            iv = Collections.min( issue.getAvs() );
            issue.setIv( iv );
        }
    }


    /*  This Method is used to clean the issues array from all issue tickets that have no related commits. */
    public void removeIssuesWithoutCommits(){
        issues.removeIf( issue -> issue.getCommits().isEmpty() );;
    }


    /*  This method is used to remove all commits related to files in a format different from ".java". */
    public void removeCommitsWithoutJavaExtension(){
        for ( IssueObject issue : this.issues ){
            issue.clean();
        }
    }


    /*  Computes the average of of all entries in the input double arraylist */
    public double average( List<Double> array ){
        double avg;
        double sum = 0.0;
        for ( double p : array ){
            sum += p;
        }
        avg = ( sum/( array.size() ) );
        return avg;
    }


    /*  ------ PROPORTION INCREMENT ------
        This method computes the value of P for every entry of the input array of issue objects.
        After that, the mean value is computed and returned.
        The input array should contain all issues having FV smaller than the issue for which 
        the value of P is going to be computed (this is the implementation of PROPORTION - INCREMENT)   */
    public int computeProportionIncremental( List<IssueObject> filteredIssues ){
        ArrayList<Double> proportions = new ArrayList<>();
        for ( IssueObject issue : filteredIssues ){
            if ( issue.getOv() != issue.getFv() ) {
                double fv = issue.getFv();
                double ov = issue.getOv();
                double iv = issue.getIv();
                double p = ( fv - iv )/( fv - ov );
                proportions.add( p );  
            }
        }
        return (int) average( proportions );
    }



    /*  This Method estimates IV (and set AVs) for all issues without declared AVs,
        using Proportion Increment algorithm.   */
    public void setAffectedAndInjectedVersionsP(){
        for ( IssueObject targetIssue : issuesWithoutAffectedVersions ){
            int fv = targetIssue.getFv();
            int ov = targetIssue.getOv();
            List<IssueObject> filteredIssues = issuesWithAffectedVersions.stream()
                .filter(issue -> issue.getFv() <= fv)
                .collect(Collectors.toList());
            int p = computeProportionIncremental(new ArrayList<>(filteredIssues));
            if ( fv == ov ) { 
                // The following formula is an approximation of the correct one (the one in the else block). 
                targetIssue.setIv( ( fv - ( 1 * p ) ) );
                continue;
            } else{
                targetIssue.setIv( ( fv - ( ( fv - ov ) * p ) ) );
            }
            int minAVValue = targetIssue.getIv();
            int maxAVValue = ( targetIssue.getFv() - 1 );
            ArrayList<Integer> avs = new ArrayList<>( IntStream.rangeClosed(minAVValue, maxAVValue).boxed().collect(Collectors.toList()) );
            targetIssue.setAvs( avs );
        }
    }


    /* This Method is used to set the buggyness of all considered classes to "Yes" or "No". */
    public void classify(){
        for ( IssueObject issue : issuesWithAffectedVersions ){
            issue.classify();
        }
        for ( IssueObject issue : issuesWithoutAffectedVersions ){
            issue.classify();
        }
    }


    public void populateDatasetMapAndWriteToCSV() throws IOException{
        this.datasetBuilder.initiateFileDataset();
        this.datasetBuilder.populateFileDataset(issuesWithAffectedVersions);
        this.datasetBuilder.populateFileDataset(issuesWithoutAffectedVersions);
        this.datasetBuilder.writeToCSV(this.projectName);
    }


}