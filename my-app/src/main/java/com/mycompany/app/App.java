package com.mycompany.app;

import java.io.File;
import java.lang.Throwable;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import java.io.IOException;
import org.json.JSONException;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;



public class App {

    public static void main( String[] args ) throws Throwable, Exception, InvalidRemoteException, TransportException, GitAPIException, IOException, JSONException 
    {

        String[] projectNames = {"storm","bookkeeper"};

        String[] projectPaths = {"/home/gianmarco/Scrivania/ML_4_SE/storm/.git", "/home/gianmarco/Scrivania/ML_4_SE/bookkeeper/.git" };

        int condition = 1;

        /*  This block of code implements the csv creation by merging project informations from jira and git. */
        if ( condition == 0 ){

            for ( int i = 0; i < projectNames.length ; i ++ ){

                IssueLifeCycleManager controller = new IssueLifeCycleManager( projectNames[i], projectPaths[i] );

                controller.initializeVersionMap();

                controller.retrieveIssueTickets();

                controller.setOpeningAndFixedVersions();

                controller.logWalkGenesi();

                controller.initializeDatasetMap();

                controller.logWalk();

                controller.removeCommitsWithoutJavaExtension();

                controller.removeIssuesWithoutCommits();

                controller.setAffectedVersionsAV();

                controller.setInjectedVersionAV();

                ArrayList<IssueObject> issuesAV = controller.getIssuesWithAffectedVersions();

                ArrayList<IssueObject> issuesP = controller.getIssuesWithoutAffectedVersions();

                //controller.computeProportionAverage( issuesAV );

                controller.setAffectedAndInjectedVersionsP( issuesP );

                controller.classify( issuesAV );

                controller.classify( issuesP );

                controller.updateDatasetMapAndWriteToCSV();

                //System.out.println( controller.getP() );

            }
            
        }

        /*  This block of code implements the training of classifiers models and  returns their evaluation metrics. */
        if ( condition == 1 ){
            ClassifierModel controller = new ClassifierModel();
            //controller.evaluateSamplingModified();
            Deliverable2Milestone3.evaluate();
        }

    }

}
