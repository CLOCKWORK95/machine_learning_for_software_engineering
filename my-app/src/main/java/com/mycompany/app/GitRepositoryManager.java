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
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
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

    private String          projectName = "";
    private String          repositoryPath = "";
    private Repository      repository;
    public final String     FILE_EXTENSION = ".java";
    private Git             git;

    // ------------------------------ Builders --------------------------------


    public GitRepositoryManager( String projectName, String repositoryPath ) throws IOException, InvalidRemoteException {
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
    
    // ------------------------------ Methods --------------------------------
    

    // This method returns a Repository instance from the path specified in 
    // private attribute repositoryPath.
    public Repository openJGitRepository() throws IOException, InvalidRemoteException{
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder.setGitDir( new File( this.repositoryPath ) )
                .readEnvironment()  // scan environment GIT_* variables
                .findGitDir()       // scan up the file system tree
                .build();
    }


    public void cloneRepositoryFromUrl() throws InvalidRemoteException, TransportException, GitAPIException {
        Git.cloneRepository()
          .setURI("https://github.com/apache/" + this.projectName )
          .setDirectory(new File( this.repositoryPath )) 
          .call();
    }


    public void listDiff( String oldCommit, String newCommit ) throws GitAPIException, IOException {
        final List<DiffEntry> diffs = this.git.diff()
                .setOldTree(prepareTreeParser( this.repository, oldCommit ) )
                .setNewTree(prepareTreeParser( this.repository, newCommit ) )
                .call();

        System.out.println( "Found: " + diffs.size() + " differences" );
        for (DiffEntry diff : diffs) {
            System.out.println("Diff: " + diff.getChangeType() + ": " +
                    (diff.getOldPath().equals(diff.getNewPath()) ? diff.getNewPath() : diff.getOldPath() + " -> " + diff.getNewPath()));
        }
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


    public void commitChanges( RevCommit commit ) throws IOException, GitAPIException {

        final List<DiffEntry> diffs = this.git.diff()
                .setOldTree(prepareTreeParser(openJGitRepository(), commit.getParent(0).getId().getName()))
                .setNewTree(prepareTreeParser(openJGitRepository(), commit.getId().getName()))
                .call();
        System.out.println("Found " + diffs.size() + " differences.");
        for (DiffEntry diff : diffs) {
            System.out.println("Diff: " + diff.getChangeType() + ": " +
                    (diff.getOldPath().equals(diff.getNewPath()) ? diff.getNewPath() : diff.getOldPath() + " -> " + diff.getNewPath()));
        }
    }


    /*  This Method is called to retrieve all files that have been modified, added or removed by a single commit.
        The method returns an array of file paths (String). */
    public ArrayList<String> getCommitChangedFiles( RevCommit commit ) throws IOException, InvalidRemoteException, GitAPIException{
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


    
    public ArrayList<FileObject> getCommitChangedFilesWithMetrics( CommitObject commitObject ) throws IOException, GitAPIException {
        Git git = this.git;
        RevCommit commit = commitObject.getCommit();
        ArrayList<FileObject> files = new ArrayList<FileObject>();
        DiffFormatter df = new DiffFormatter( DisabledOutputStream.INSTANCE );
        df.setRepository( openJGitRepository() );
        df.setDiffComparator( RawTextComparator.DEFAULT );
        df.setDetectRenames( true );
        final List<DiffEntry> diffs = git.diff()
                .setOldTree(prepareTreeParser(openJGitRepository(), commit.getParent(0).getId().getName()))
                .setNewTree(prepareTreeParser(openJGitRepository(), commit.getId().getName()))
                .call();
        int changeSetSize = diffs.size();
        int javafiles = 0;
        for ( DiffEntry diff : diffs ) {
            //String file_name = diff.getOldPath().equals(diff.getNewPath()) ? diff.getNewPath() : diff.getOldPath() + " -> " + diff.getNewPath();
            int linesAdded = 0;
            int linesDeleted = 0;
            int linesReplaced = 0;
            if ( diff.getNewPath().endsWith( FILE_EXTENSION ) ){
                javafiles ++;
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
                files.add( new FileObject(  filepath, version, fileAge, loc, linesAdded, linesDeleted, linesReplaced, 
                                            changeSetSize, commitObject.getAuthorName(), numImports, numComments ) );
            }       
        }
        df.close();
        return files;
    }




    public ArrayList<FileObject> getCommitChangedFilesEasy( CommitObject commitObject ) throws IOException, GitAPIException {
        Git git = this.git;
        RevCommit commit = commitObject.getCommit();
        ArrayList<FileObject> files = new ArrayList<FileObject>();
        DiffFormatter df = new DiffFormatter( DisabledOutputStream.INSTANCE );
        df.setRepository( openJGitRepository() );
        df.setDiffComparator( RawTextComparator.DEFAULT );
        df.setDetectRenames( true );
        final List<DiffEntry> diffs = git.diff()
                .setOldTree(prepareTreeParser(openJGitRepository(), commit.getParent(0).getId().getName()))
                .setNewTree(prepareTreeParser(openJGitRepository(), commit.getId().getName()))
                .call();
        int changeSetSize = diffs.size();
        
        for ( DiffEntry diff : diffs ) {
            
            if ( diff.getNewPath().endsWith( FILE_EXTENSION ) ){
                String  filepath = diff.getNewPath();  
                int     version = commitObject.getVersion();
                files.add( new FileObject(  filepath, version ) );
            }       
        }
        df.close();
        return files;
    }








    public String getTextfromCommittedFile( RevCommit commit, String filename ) throws IOException, InvalidRemoteException {
        RevTree tree = commit.getTree();
        Repository repository = openJGitRepository();
        String file_text;
        // now try to find a specific file
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(filename));
            if (!treeWalk.next()) {
                return "";
            }
            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = repository.open(objectId);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            loader.copyTo(stream);
            file_text = stream.toString();
            return file_text;
        }
    }



    public int getFileAgeInWeeks( RevCommit startCommit, String filename ) throws IOException, InvalidRemoteException {
        //SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date date_first_commit, date_current_commit;
        int age;
        RevWalk revWalk = new RevWalk( this.repository );
        revWalk.markStart( revWalk.parseCommit( this.repository.resolve( startCommit.getName() ) ) );
        revWalk.setTreeFilter( PathFilter.create( filename ) );
        revWalk.sort( RevSort.COMMIT_TIME_DESC );
        revWalk.sort( RevSort.REVERSE, true );
        RevCommit commit = revWalk.next();
        if ( commit != null ){
            date_first_commit = commit.getAuthorIdent().getWhen();
            date_current_commit = startCommit.getAuthorIdent().getWhen();
            long diffInMillies = Math.abs(date_current_commit.getTime() - date_first_commit.getTime());
            long diff_in_days = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
            age = (int) (diff_in_days/7);
            revWalk.close();
            return age;
        } else{
            revWalk.close();
            return -1;
        }
        
    }



    public int getLoc( String fileText ) {
        int lines = 0;
        try ( BufferedReader reader = new BufferedReader(new StringReader(fileText)) ) {
            while ( reader.readLine() != null ) lines++;
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return lines;
    }



    public int getNumImports( String fileText ){
        int numImports = 0;
        try ( BufferedReader reader = new BufferedReader(new StringReader(fileText)) ) {
            for ( String line = reader.readLine(); line != null; line = reader.readLine()) {
                if( line.contains( "import" )){
                    numImports ++;
                }
            }         
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return numImports;
    }



    public int getNumComments( String fileText ){
        int numComments = 0;
        try ( BufferedReader reader = new BufferedReader(new StringReader(fileText)) ) {
            for ( String line = reader.readLine(); line != null; line = reader.readLine()) {
                if ( line.endsWith("}") || line.endsWith("{") || line.endsWith(";") ) continue;
                else numComments ++;              
            }         
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return numComments;
    }


}



