package com.mycompany.app;

import java.io.File;
import java.io.IOException;

import java.util.Collection;
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
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.revwalk.filter.MessageRevFilter;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;


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


    private void listDiff( String oldCommit, String newCommit ) throws GitAPIException, IOException {
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



}



