package com.mycompany.app;

import java.io.File;

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
import java.util.ArrayList;



public class App {

    public static void main( String[] args ) throws Exception, InvalidRemoteException, TransportException, GitAPIException, IOException, JSONException 
    {

        String projectName = "storm";

        String projectPath = "/home/gianmarco/Scrivania/ML_4_SE/storm/.git";

        int condition = 1;

        /*  This block of code implements the csv creation by merging project informations from jira and git. */
        if ( condition == 0 ){
            IssueLifeCycleManager controller = new IssueLifeCycleManager( projectName, projectPath );

            controller.initializeVersionMap();

            controller.retrieveIssueTickets();

            controller.setOpeningAndFixedVersions();

            controller.logWalk();

            controller.removeCommitsWithoutJavaExtension();

            controller.removeIssuesWithoutCommits();

            controller.setAffectedVersionsAV();

            controller.setInjectedVersionAV();

            ArrayList<IssueObject> issuesAV = controller.getIssuesWithAffectedVersions();

            ArrayList<IssueObject> issuesP = controller.getIssuesWithoutAffectedVersions();

            controller.computeProportionIncremental( issuesAV );

            controller.setAffectedAndInjectedVersionsP( issuesP );

            controller.classify( issuesAV );

            controller.classify( issuesP );

            //controller.printIssuesInfo( issuesAV );

            //controller.printIssuesInfo( issuesP );

            controller.populateDatasetMapAndWriteToCSV();

            System.out.println( controller.getP() );
        }

        /*  This block of code implements the training of classifiers models and  returns their evaluation metrics. */
        if ( condition == 1 ){
            ClassifierModel controller = new ClassifierModel();
            controller.evaluateSamplingModified();
        }

    }

}
