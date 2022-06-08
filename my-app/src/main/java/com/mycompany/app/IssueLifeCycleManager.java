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
    private double                          p;
    private DatasetBuilder                  datasetBuilder;
    private ArrayList<CommitObject>         allCommits;                    


    // ------------------------------ Builders --------------------------------


    public IssueLifeCycleManager( String projectName, String projectPath ) throws IOException, InvalidRemoteException{
        this.projectName = projectName;
        this.projectPath = projectPath;
        this.issues = new ArrayList<IssueObject>();
        this.issuesWithAffectedVersions = new ArrayList<IssueObject>();
        this.issuesWithoutAffectedVersions = new ArrayList<IssueObject>();
        this.gitRepoManager = new GitRepositoryManager( projectName, projectPath );
        this.jiraTicketManager = new JiraTicketManager( projectName.toUpperCase() );
        this.versionMap = MultimapBuilder.treeKeys().linkedListValues().build();
        this.datasetBuilder = new DatasetBuilder( this.projectName );
        this.allCommits = new ArrayList<CommitObject>();
    }

    // ------------------------------ Getters ---------------------------------

    public ArrayList<IssueObject> getIssues(){
        return this.issues;
    }
    public ArrayList<IssueObject> getIssuesWithAffectedVersions(){
        return this.issuesWithAffectedVersions;
    }
    public ArrayList<IssueObject> getIssuesWithoutAffectedVersions(){
        return this.issuesWithoutAffectedVersions;
    }
    public double getP(){
        return this.p;
    }
    public Multimap<LocalDate, String> getVersionMap(){
        return this.versionMap;
    }
    public DatasetBuilder getDasetBuilder(){
        return this.datasetBuilder;
    }

    // ------------------------------ Setters ---------------------------------

    public void setP( double p ){
        this.p = p;
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
    public int getVersionFromLocalDate( LocalDate localDate ){;
        int version = -1;
        for( LocalDate date : this.versionMap.keySet()){
            version = Integer.valueOf( Iterables.getLast( versionMap.get(date) ));
            if ( date.isEqual( localDate ) || date.isAfter( localDate ) ){                  
                break;
            }
        }
        return version;
    }




    public void logWalkGenesi() throws MissingObjectException, IncorrectObjectTypeException, IOException, GitAPIException {
        
        Collection<Ref> allRefs = this.gitRepoManager.getRepository().getAllRefs().values();

        // a RevWalk allows to walk over commits based on some filtering that is defined
        int stop = 0;

        try ( RevWalk revWalk = new RevWalk( this.gitRepoManager.getRepository()) ) {
            
            ProgressBar pb = new ProgressBar("SCANNING THE GIT LOG TO INITIALIZE THE FILE DATASET", Iterables.size( revWalk)); 
            pb.start();

            for( Ref ref : allRefs ) {
                revWalk.markStart( revWalk.parseCommit( ref.getObjectId() ));
            }

            for( RevCommit commit : revWalk ) {
                pb.step();

                LocalDate commitLocalDate = commit.getCommitterIdent().getWhen().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                
                int version = getVersionFromLocalDate( commitLocalDate );
                
                try{
                    RevCommit checkparent = commit.getParent(0);
                } catch ( ArrayIndexOutOfBoundsException e ){
                    continue;
                }
                CommitObject commitObject = new CommitObject( commit, version, this.gitRepoManager );
                this.allCommits.add( commitObject );
                
            }

            pb.stop();
        } 

    }




    /*  This Method interfaces with the Git Repository Manager in order to retrieve,
        for each issue in issues array, all related commits (having Issue ID inside the
        commit message).    */ 
    public void logWalk() throws MissingObjectException, IncorrectObjectTypeException, IOException, GitAPIException {
        
        Collection<Ref> allRefs = this.gitRepoManager.getRepository().getAllRefs().values();

        // a RevWalk allows to walk over commits based on some filtering that is defined
        int stop = 0;

        ProgressBar pb = new ProgressBar("SCANNING ISSUE TICKETS", this.issues.size()); 
        pb.start();

        for ( IssueObject issue : this.issues ){
            
            pb.step();

            try ( RevWalk revWalk = new RevWalk( this.gitRepoManager.getRepository()) ) {

                revWalk.setRevFilter( MessageRevFilter.create( issue.getTicketID() ) );

                for( Ref ref : allRefs ) {
                    revWalk.markStart( revWalk.parseCommit( ref.getObjectId() ));
                }
                
                for( RevCommit commit : revWalk ) {
                    LocalDate commitLocalDate = commit.getCommitterIdent().getWhen().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    int version = getVersionFromLocalDate( commitLocalDate );
                    if ( issue.getFv() >= version ){ // bypass all the commits with version greater than ticket's fixed version.
                        try{
                            RevCommit checkparent = commit.getParent(0);
                        } catch ( ArrayIndexOutOfBoundsException e ){
                            continue;
                        }
                        CommitObject commitObject = new CommitObject( commit, issue, version, this.gitRepoManager, true );
                        issue.append( commitObject );
                    }
                    
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
            // of issues without AVs. The Proportion Method will be applied on it to retrieve an
            // estimate of the injected ( and so the affected ) version(s).
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
                // with the reported fixed version ( i.e. av > fv ).
                avs.removeIf( av -> av >= issue.getFv() );
                if ( avs.isEmpty() ) { 
                    this.issuesWithoutAffectedVersions.add( issue ); 
                } 
                else{
                    int minValue = Collections.min( avs );
                    int maxValue = ( issue.getFv() - 1 );
                    avs = new ArrayList<>( IntStream.rangeClosed(minValue, maxValue).boxed().collect(Collectors.toList()) );
                    issue.setAvs( avs );  
                    this.issuesWithAffectedVersions.add( issue ); 
                }            
            }
        }
    }


    /*  This Method is used to set the Injected version of every issue having declared affected
        versions. The injected version is set as the minimum between all declared affected versions. 
        Also, into this method all proportion values (the ones associated to every single issue) are computed,
        in order to make IVs estimations for IV and AV of all tickets with not declared AVs.*/
    public void setInjectedVersionAV(){
        int iv;
        for ( IssueObject issue : this.issuesWithAffectedVersions ){
            iv = Collections.min( issue.getAvs() );
            issue.setIv( iv );
            issue.computeProportion();
        }
    }


    /*  This Method is used to clean the issues array from all issue tickets that have no related commits. */
    public void removeIssuesWithoutCommits(){
        System.out.println(this.issues.size());
        issues.removeIf( issue -> issue.getCommits().isEmpty() );
        System.out.println(this.issues.size());
    }


    public void removeCommitsWithoutJavaExtension(){
        for ( IssueObject issue : this.issues ){
            issue.clean();
        }
    }


    public double average( ArrayList<Double> array ){
        double avg = 0.0;
        double sum = 0.0;
        for ( double p : array ){
            sum += p;
        }
        avg = ( sum/( array.size() ) );
        return avg;
    }


    /*  This method computes a value of proportion that averages the proportion values
        computed over each single issue from the list of AVs native owners. 
        This is NOT a canonical way to compute proportion.  */
    public void computeProportionAverage( ArrayList<IssueObject> issues ){
        ArrayList<Double> proportions = new ArrayList<>();
        for ( IssueObject issue : issues ){
            if ( !( issue.getOv() == issue.getFv() ) ) {
                double fv = (double) issue.getFv();
                double ov = (double) issue.getOv();
                double iv = (double) issue.getIv();
                double p = ( fv - iv )/( fv - ov );
                proportions.add( p );  
            }
        }
        this.p = average( proportions );
    }


    public void setAffectedAndInjectedVersionsP( ArrayList<IssueObject> issues ){
        for ( IssueObject issue : issues ){
            int fv = issue.getFv();
            int ov = issue.getOv();
            ArrayList<Double> proportions = new ArrayList<>();
            double P;
            // Compute value of Incremental Proportion for the current issue, by averaging
            // proportion values of all those tickets contained into the array of issues 
            // with AVs having earlier Injected Version with respect to this one.
            for ( IssueObject previousTicket : this.issuesWithAffectedVersions ){
                if ( previousTicket.getFv() <= fv ){
                    proportions.add( previousTicket.getProportion() );
                }
            }
            // If there were not previous tickets wrt this one to compute proportion on,
            // just set the injected version of this issue as its opening version.
            if( proportions.size() == 0 ){
                issue.setIv( ov );
            }
            else{
                P = (int) average( proportions );
                int iv;
                if ( fv == ov ) { 
                    // IS THIS CORRECT? MAYBE IN THIS CASE I JUST SHOULD DELETE THE ISSUE? 
                    // CAUSE I AM ACTUALLY APPLYING PROPORTION RIGHT (IN THE ELSE BLOCK)!
                    iv = (int) ( fv - ( ( 1 ) * P ) );
                    if ( iv <= 1 )  iv = 1;
                    issue.setIv( iv );
                } else {
                    iv = (int) ( fv - ( ( fv - ov ) * P ) );
                    if ( iv <= 1 )  iv = 1;
                    issue.setIv( iv );
                }
            }

            int minAVValue = issue.getIv();
            int maxAVValue = ( issue.getFv() - 1 );
            ArrayList<Integer> avs = new ArrayList<>( IntStream.rangeClosed(minAVValue, maxAVValue).boxed().collect(Collectors.toList()) );
            issue.setAvs( avs );
        }
    }


    public void classify( ArrayList<IssueObject> issues ){
        for ( IssueObject issue : issues ){
            issue.classify();
        }
    }


    public void initializeDatasetMap(){
        this.datasetBuilder.initializeFileDataset(allCommits);
    }


    public void updateDatasetMapAndWriteToCSV() throws IOException{
        this.datasetBuilder.populateFileDataset(issuesWithAffectedVersions);
        this.datasetBuilder.populateFileDataset(issuesWithoutAffectedVersions);
        this.datasetBuilder.writeToCSV(this.projectName);
    }


    public void printIssuesInfo( ArrayList<IssueObject> issues ) throws IOException, GitAPIException{
        int count = 0;
        for ( IssueObject issue : issues ){
            printInfo( issue );
            count ++;
            if (count == 300) break;
        }
    }


    public void printInfo( IssueObject issue ) throws IOException, GitAPIException {

        System.out.println("\n\nTicket Identifier : " + issue.getTicketID() );
        System.out.println("Resolution Date : " + issue.getResolutionDate() + " -> Fixed Version (index) : " + issue.getFv() );
        System.out.println("Creation Date : " + issue.getCreationDate() + " -> Opening Version (index) : " + issue.getOv());
        System.out.println("Affected Versions : " + issue.getAffectedVersions().toString() + " AVs indexes : " + issue.getAvs().toString() );
        System.out.println("Injected Version (index) : " +  issue.getIv() );
        System.out.println("Related Commits :");
        int count = 1;
        for ( CommitObject commit : issue.getCommits() ) {
            //System.out.println( "Commit " + count + ") \nRevision hash : " + commit.getRevisionHash() );
            //System.out.println( "Message : " + commit.getFullMessage() );
            System.out.println( "Date : " + commit.getCommitLocalDate() + " --> Software Version : " + commit.getVersion() );
            System.out.println( "Author : "   + commit.getAuthorName() + " Author e-mail : " + commit.getAuthorEmail() );
            System.out.println( "Touched Files (.java extension) and classification :");
            int fileCount = 1;
            for ( FileObject file : commit.getFiles() ){
                System.out.println( "    " + fileCount + ") Filepath : " + file.getFilepath() + " | Version : "  + file.getVersion() + 
                " | LOC : " + file.getLOC() + " | AGE : " + file.getAGE()  + " | Buggy : "  + file.getBuggyness());
                fileCount ++;
            }
            count = count + 1;
        }
    }


    public void printVersionMap(){
        System.out.println(this.versionMap.toString());
    }


}