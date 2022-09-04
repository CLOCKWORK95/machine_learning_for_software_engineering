package com.mycompany.app;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.concurrent.TimeUnit;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import java.io.ByteArrayOutputStream;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.revwalk.RevSort;



public class GitRepositoryManager {

    // ------------------------------ Attributes -----------------------------

    private String                  projectName = "";
    private String                  repositoryPath = "";
    private Repository              repository;
    public static final String      FILE_EXTENSION = ".java";
    private Git                     git;
    private ArrayList<String>       filepathsList = new ArrayList<>();

    // ------------------------------ Builders --------------------------------

    public GitRepositoryManager( String projectName, String repositoryPath ) throws IOException {
        this.projectName = projectName;
        this.repositoryPath = repositoryPath;
        this.repository = openJGitRepository();
        this.git = new Git( this.repository );
    }

    // ------------------------------ Setters --------------------------------

    public void setRepositoryPath( String repositoryPath ){
        this.repositoryPath = repositoryPath;
    }

    public void setProjectName( String projectName ){
        this.projectName = projectName;
    }

    public void setRepository( Repository repository ){
        this.repository = repository;
    }

    // ------------------------------ Getters --------------------------------

    public String getRepositoryPath( ){
        return this.repositoryPath;
    }

    public String getProjectName( ){
        return this.projectName;
    }

    public Repository getRepository( ){
        return this.repository;
    }

    public List<String> getFilePathsList(){
        return this.filepathsList;
    }
    
    // ------------------------------ Methods --------------------------------
    

    // This method returns a Repository instance from the path specified in 
    // private attribute repositoryPath.
    public Repository openJGitRepository() throws IOException{
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder.setGitDir( new File( this.repositoryPath ) )
                .readEnvironment()  // scan environment GIT_* variables
                .findGitDir()       // scan up the file system tree
                .build();
    }



    public void cloneRepositoryFromUrl() throws GitAPIException {
        Git.cloneRepository()
          .setURI("https://github.com/apache/" + this.projectName )
          .setDirectory(new File( this.repositoryPath )) 
          .call();
    }



    private AbstractTreeIterator prepareTreeParser( Repository repository, String objectId ) throws IOException {
        // from the commit we can build the tree which allows us to construct the TreeParser
        //noinspection Duplicates
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(repository.resolve(objectId));
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }

            walk.dispose();

            return treeParser;
        }
    }



    /*  This Method is called to retrieve all files that have been modified, added or removed by a single commit.
        The method returns an array of file paths (String). */
    public List<String> getCommitChangedFiles( RevCommit commit ) throws IOException, GitAPIException{
        final List<DiffEntry> diffs = this.git.diff()
                .setOldTree(prepareTreeParser(openJGitRepository(), commit.getParent(0).getId().getName()))
                .setNewTree(prepareTreeParser(openJGitRepository(), commit.getId().getName()))
                .call();
        String filepath;
        ArrayList<String> filepaths = new ArrayList<>();
        for (DiffEntry diff : diffs) {
            filepath = diff.getNewPath();
            if ( filepath.endsWith( FILE_EXTENSION ) ){
                filepaths.add( filepath );
            }
        }
        return filepaths;
    }


    
    public List<FileObject> getCommitChangedFilesWithMetrics( CommitObject commitObject ) throws IOException, GitAPIException {
        Git                     g = this.git;
        RevCommit               commit = commitObject.getCommit();
        ArrayList<FileObject>   files = new ArrayList<>();
        DiffFormatter           df = new DiffFormatter( DisabledOutputStream.INSTANCE );
                                df.setRepository( openJGitRepository() );
                                df.setDiffComparator( RawTextComparator.DEFAULT );
                                df.setDetectRenames( true );
        final List<DiffEntry>   diffs = g.diff()
                .setOldTree(prepareTreeParser(openJGitRepository(), commit.getParent(0).getId().getName()))
                .setNewTree(prepareTreeParser(openJGitRepository(), commit.getId().getName()))
                .call();

        int changeSetSize = diffs.size();
        
        for ( DiffEntry diff : diffs ) {
            files = (ArrayList<FileObject>) computeMetricsAndAppendFile(commitObject, files, diff, df, changeSetSize);      
        }

        df.close();
        return files;
    }



    public List<FileObject> computeMetricsAndAppendFile(CommitObject commitObject, List<FileObject>files, DiffEntry diff, DiffFormatter df, int changeSetSize) throws IOException {

        int linesAdded = 0;
        int linesDeleted = 0;
        int linesReplaced = 0;
        RevCommit commit = commitObject.getCommit();

        if ( diff.getNewPath().endsWith( FILE_EXTENSION ) ){
            
            String  filepath = diff.getNewPath();                          
            String  fileText = getTextfromCommittedFile( commit, filepath );
            int     fileAge = getFileAgeInWeeks( commit, filepath );

            for ( Edit edit : df.toFileHeader( diff ).toEditList() ) {

                if ( edit.getBeginA() < edit.getEndA() && edit.getBeginB() < edit.getEndB() ){
                    linesReplaced += edit.getEndB() - edit.getBeginB();
                }
                if ( edit.getBeginA() < edit.getEndA() && edit.getBeginB() == edit.getEndB() ){
                    linesDeleted += edit.getEndA() - edit.getBeginA();
                }
                if ( edit.getBeginA() == edit.getEndA() && edit.getBeginB() < edit.getEndB() ){
                    linesAdded += edit.getEndB() - edit.getBeginB();
                }
                
            }
            int     version = commitObject.getVersion();
            int     loc = getLoc( fileText );
            int     numImports = getNumImports(fileText);
            int     numComments = getNumComments(fileText);
            
            FileObject fileObj = new FileObject(filepath, version, fileAge, loc, linesAdded, linesDeleted, linesReplaced);
            fileObj.setChangeSetSize(changeSetSize);
            fileObj.setAuthor(commitObject.getAuthorName());
            fileObj.setNumImports(numImports);
            fileObj.setNumComments(numComments);
            
            if (!filepathsList.contains(filepath))  filepathsList.add(filepath);
            files.add(fileObj);
        }

        return files;
    }



    public String getTextfromCommittedFile( RevCommit commit, String filename ) throws IOException {
        RevTree tree = commit.getTree();
        Repository repo = openJGitRepository();
        String fileText;
        // now try to find a specific file
        try (TreeWalk treeWalk = new TreeWalk(repo)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(filename));
            if (!treeWalk.next()) {
                return "";
            }
            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = repo.open(objectId);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            loader.copyTo(stream);
            fileText = stream.toString();
            return fileText;
        }
    }



    public int getFileAgeInWeeks( RevCommit startCommit, String filename ) throws IOException {
        
        Date    dateFirstCommit;
        Date    dateCurrentCommit;
        int     age;
        RevWalk revWalk = new RevWalk( this.repository );
        revWalk.markStart( revWalk.parseCommit( this.repository.resolve( startCommit.getName() ) ) );
        revWalk.setTreeFilter( PathFilter.create( filename ) );
        revWalk.sort( RevSort.COMMIT_TIME_DESC );
        revWalk.sort( RevSort.REVERSE, true );
        RevCommit commit = revWalk.next();
        if ( commit != null ){
            dateFirstCommit = commit.getAuthorIdent().getWhen();
            dateCurrentCommit = startCommit.getAuthorIdent().getWhen();
            long diffInMillies = Math.abs(dateCurrentCommit.getTime() - dateFirstCommit.getTime());
            long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
            age = (int) (diffInDays/7);
            revWalk.close();
            return age;
        } else{
            revWalk.close();
            return -1;
        }
        
    }



    public int getLoc( String fileText ) throws IOException{
        int lines = 0;
        try ( BufferedReader reader = new BufferedReader(new StringReader(fileText)) ) {
            for ( String line = reader.readLine(); line != null; line = reader.readLine())  lines++;
        } 
        return lines;
    }



    public int getNumImports( String fileText ) throws IOException{
        int numImports = 0;
        try ( BufferedReader reader = new BufferedReader(new StringReader(fileText)) ) {
            for ( String line = reader.readLine(); line != null; line = reader.readLine()) {
                if( line.contains( "import" )){
                    numImports ++;
                }
            }         
        } 
        return numImports;
    }



    public int getNumComments( String fileText ) throws IOException{
        int numComments = 0;
        try ( BufferedReader reader = new BufferedReader(new StringReader(fileText)) ) {
            for ( String line = reader.readLine(); line != null; line = reader.readLine()) {   
                if (    line.contains("//") || 
                        line.contains("/*") || 
                        line.contains("*/") || 
                        !( line.endsWith("}") || line.endsWith("{") || line.endsWith(";") || line.equals("\n") || line.endsWith(")"))     ) {
                            numComments ++;              
                }     
            }         
        } 
        return numComments;
    }


}



