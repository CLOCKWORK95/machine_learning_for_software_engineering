package com.mycompany.app;
import java.util.Scanner;
import java.util.logging.Logger;

public class App {

    private static Logger logger = Logger.getLogger(App.class.getName());


    public static void main( String[] args ) throws Exception {

    String[] projectNames = {"bookkeeper"/* ,"storm" */};

    String[] projectPaths = {"/home/gianmarco/Scrivania/ML_4_SE/bookkeeper/.git" /* ,"/home/gianmarco/Scrivania/ML_4_SE/storm/.git" */};

        Scanner sc= new Scanner(System.in);    
        logger.info("\n\nDATASET CREATION                    -   0\nPREDICTORS TRAINING AND EVALUATION  -   1\nEnter the operation code : ");  
        int opcode= sc.nextInt(); 

        /*  This block of code implements the csv creation by merging project informations from jira and git. */
        if ( opcode == 0 ){

            for ( int i = 0; i < projectNames.length ; i ++ ){

                IssueLifeCycleManager controller = new IssueLifeCycleManager( projectNames[i], projectPaths[i] );

                controller.initializeVersionMap();

                controller.retrieveIssueTickets();

                controller.setOpeningAndFixedVersions();

                controller.logWalk();

                controller.removeCommitsWithoutJavaExtension();

                controller.removeIssuesWithoutCommits();

                controller.setAffectedVersionsAV();

                controller.setInjectedVersionAV();

                controller.setAffectedAndInjectedVersionsP();

                controller.classify();

                controller.populateDatasetMapAndWriteToCSV();

            }
            
        }

        /*  This block of code implements the training of classifiers models and returns their evaluation metrics. */
        else if ( opcode == 1 ){
            ClassifierModel controller = new ClassifierModel();
            controller.evaluateSamplingModified();
        }
    }

}
