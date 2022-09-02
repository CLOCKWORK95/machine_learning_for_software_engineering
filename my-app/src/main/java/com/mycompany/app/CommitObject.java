package com.mycompany.app;
import java.util.List;
import java.util.ArrayList;
import org.eclipse.jgit.revwalk.RevCommit;
import java.time.LocalDate;
import java.time.ZoneId;
import org.eclipse.jgit.api.errors.GitAPIException;
import java.io.IOException;

public class CommitObject{

    // ------------------------------ Attributes --------------------------------

    private GitRepositoryManager    gitRepoManager;
    private IssueObject             issue;
    private RevCommit               commit;
    private RevCommit               parent;
    private String                  revisionHash;
    private String                  authorName;
    private String                  authorEmail;
    private LocalDate               commitLocalDate;
    private int                     version;
    private ArrayList<FileObject>   files = null;
    private String                  fullMessage;

    // ------------------------------ Builders ----------------------------------
    
    public CommitObject( RevCommit commit, IssueObject issue, int version, GitRepositoryManager gitRepoManager ) throws IOException, GitAPIException{
        this.issue =            issue;
        this.commit =           commit;
        this.version =          version;
        this.gitRepoManager =   gitRepoManager;
        this.parent =           this.commit.getParent(0);
        this.revisionHash =     this.commit.getName();
        this.authorName =       this.commit.getAuthorIdent().getName();
        this.authorEmail =      this.commit.getAuthorIdent().getEmailAddress();
        this.commitLocalDate =  this.commit.getCommitterIdent().getWhen().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        this.fullMessage =      this.commit.getFullMessage();
        this.fileManagement();
    }

    // ------------------------------ Getters ----------------------------------

    public RevCommit getCommit(){
        return this.commit;
    }
    public String getRevisionHash(){
        return this.revisionHash;
    }
    public String getAuthorName(){
        return this.authorName;
    }
    public String getAuthorEmail(){
        return this.authorEmail;
    }
    public LocalDate getCommitLocalDate(){
        return this.commitLocalDate;
    }
    public int getVersion(){
        return this.version;
    }
    public RevCommit getParent(){
        return this.parent;
    }
    public List<FileObject> getFiles(){
        return this.files;
    }
    public String getFullMessage(){
        return this.fullMessage;
    }
    public IssueObject getIssue(){
        return this.issue;
    }

    // ------------------------------ Methods ----------------------------------
    

    public void appendFile( FileObject file ){
        this.files.add( file );
    }

    public void fileManagementFromFilepaths() throws IOException, GitAPIException{
        ArrayList<String> filepaths = (ArrayList<String>) this.gitRepoManager.getCommitChangedFiles( this.commit );
        for ( String filepath : filepaths ){
            String buggy = "Yes";
            if ( this.version >= this.issue.getFv() || this.version < this.issue.getIv() ){
                buggy = "No";
            }
            FileObject file = new FileObject( filepath, version, buggy );
            this.files.add( file );
        }
    }


    public void fileManagement() throws IOException, GitAPIException{
        this.files = (ArrayList<FileObject>) this.gitRepoManager.getCommitChangedFilesWithMetrics( this );
    }


    public void classify(){
        for ( FileObject file : this.files ){
            file.classify( this );
        }
    }

}