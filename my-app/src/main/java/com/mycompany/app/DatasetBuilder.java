package com.mycompany.app;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections4.map.MultiKeyMap;

public class DatasetBuilder {

    //------------------------------------ Attributes --------------------------------------------

    // Dataset as a MultiKeyMap with key :<version,filepath> and value <metrics>
	private MultiKeyMap                     fileDataset = MultiKeyMap.multiKeyMap( new LinkedMap() );

    private Multimap<LocalDate,String>      versionMap;

    private int                             lastVersion;

	private String							projectName;

	private String 							path_to_dir = "/home/gianmarco/Scrivania/ML_4_SE/my-app/src/main/java/com/mycompany/app/";

    //------------------------------------- Builders ---------------------------------------------

    public DatasetBuilder( String projectName ){
		this.projectName = projectName;
    }

	public void setVersionMap( Multimap<LocalDate,String> versionMap ){
		this.versionMap = versionMap;
		this.lastVersion = ( versionMap.size()/2 )/2;
		System.out.println( this.lastVersion );
	}

    //------------------------------------- Methods ----------------------------------------------

	/*  This Method is used to initialize the Multi Key Map representing the dataset to be created. */
    public void initializeFileDataset( ArrayList<CommitObject> allCommits ){
        int     version;
        String  filepath;
        
		for ( CommitObject commit : allCommits ){
			for ( FileObject file : commit.getFiles() ){
				Metrics newMetrics = new Metrics();
				version = file.getVersion();
				filepath = file.getFilepath();
				newMetrics.setVersion(version);
				newMetrics.setFilepath(filepath);
				newMetrics.setNR( 1 );
				newMetrics.setAGE( file.getAGE());
				newMetrics.setCHURN(file.getCHURN());
				newMetrics.appendAuthor(file.getAUTHOR());
				newMetrics.setLOC_TOUCHED(file.getLOC_TOUCHED());
				newMetrics.setMAX_LOC_ADDED(file.getLinesAdded());
				newMetrics.setLOC(file.getLOC());
				newMetrics.setAVG_LOC_ADDED(file.getLinesAdded());
				newMetrics.setAVG_CHANGE_SET(file.getChangeSetSize());
				newMetrics.setMAX_CHANGE_SET(file.getChangeSetSize());
				newMetrics.setNumImports(file.getNumImports());
				newMetrics.setNumComments(file.getNumComments());
				newMetrics.setBUGGYNESS(file.getBuggyness());
				if( !fileDataset.containsKey(version,filepath) ){
					System.out.println("NEW entry!");
					fileDataset.put( version, filepath, newMetrics );
				} else{
					Metrics oldMetrics = ( Metrics ) fileDataset.get( version, filepath );
					System.out.println("OLD entry!");
					newMetrics.update( oldMetrics );
					fileDataset.put( version, filepath, newMetrics );
				}
			}
		}
        
    }



    /*  This Method is used to update/populate the Multi Key Map with file informations derived 
		from the analysis of the Jira Issue Tickets. */
    public void populateFileDataset( ArrayList<IssueObject> issues ){
        int     version;
        String  filepath;
		String  buggyness;
        for ( IssueObject issue : issues ){
            for ( CommitObject commit : issue.getCommits() ){
                for ( FileObject file : commit.getFiles() ){

                    version = file.getVersion();
                    filepath = file.getFilepath();
					buggyness = file.getBuggyness();

                    Metrics oldMetrics = ( Metrics ) fileDataset.get( version, filepath );
					System.out.println("UPDATING entry!");
					oldMetrics.setBUGGYNESS(buggyness);
					fileDataset.put( version, filepath, oldMetrics );
                }
            }
        }
    }


    /*  This Method is used to update/populate the Multi Key Map with file informations derived 
		from the analysis of the Jira Issue Tickets. */
	public void populateFileDatasetHeavy( ArrayList<IssueObject> issues ){
		int     version;
		String  filepath;
		String  buggyness;
		for ( IssueObject issue : issues ){
			for ( CommitObject commit : issue.getCommits() ){
				for ( FileObject file : commit.getFiles() ){
					Metrics newMetrics = new Metrics();
					version = file.getVersion();
					filepath = file.getFilepath();
					buggyness = file.getBuggyness();
					newMetrics.setVersion(version);
					newMetrics.setFilepath(filepath);
					newMetrics.setNR( 1 );
					newMetrics.setAGE( file.getAGE());
					newMetrics.setCHURN(file.getCHURN());
					newMetrics.appendAuthor(file.getAUTHOR());
					newMetrics.setLOC_TOUCHED(file.getLOC_TOUCHED());
					newMetrics.setMAX_LOC_ADDED(file.getLinesAdded());
					newMetrics.setLOC(file.getLOC());
					newMetrics.setAVG_LOC_ADDED(file.getLinesAdded());
					newMetrics.setAVG_CHANGE_SET(file.getChangeSetSize());
					newMetrics.setMAX_CHANGE_SET(file.getChangeSetSize());
					newMetrics.setNumImports(file.getNumImports());
					newMetrics.setNumComments(file.getNumComments());
					newMetrics.setBUGGYNESS(file.getBuggyness());
					if( !fileDataset.containsKey(version,filepath) ){
						System.out.println("NEW entry!");
						fileDataset.put( version, filepath, newMetrics );
					} else{
						Metrics oldMetrics = ( Metrics ) fileDataset.get( version, filepath );
						System.out.println("OLD entry!");
						newMetrics.update( oldMetrics );
						fileDataset.put( version, filepath, newMetrics );
					}
				}
			}
		}
	}


	public void writeToCSV( String projectName ) throws IOException {

		// Set the name of the file
		try ( FileWriter csvWriter = new FileWriter( path_to_dir + "output/" + projectName + "_dataset.csv" ) ) {

			/*	
			 * Metrics Data Structure
             *   (version)
             *   (filepath)
			 *  0 - NumberRevisions
			 *  1 - NumberAuthors
			 *  2 - LOC
			 *  3 - AGE
			 *  4 - CHURN
			 *  5 - LOC_TOUCHED
			 *  6 - Max_Loc_Added
			 *  7 - Avg_Chg_set
			 *  8 - Max_Chg_Set
			 *  9 - Avg_LOC_Added
			 *  10 - numImports
			 *  11 - numComments
			 * 	12 - Buggyness
			 * */

			// Append the first line
			csvWriter.append("Version Number");
			csvWriter.append(",");
			csvWriter.append("File Name");
			csvWriter.append(",");
			csvWriter.append("NumberRevisions");
			csvWriter.append(",");
			csvWriter.append("NumberAuthors");
			csvWriter.append(",");
			csvWriter.append("LOC");
			csvWriter.append(",");
			csvWriter.append("AGE");
			csvWriter.append(",");
			csvWriter.append("CHURN");
			csvWriter.append(",");
			csvWriter.append("LOC_TOUCHED");
			csvWriter.append(",");
			csvWriter.append("Avg_LOC_Added");
			csvWriter.append(",");
			csvWriter.append("MaxLocAdded");
			csvWriter.append(",");
			csvWriter.append("Avg_Chg_Set");
			csvWriter.append(",");
			csvWriter.append("Max_Chg_Set");
			csvWriter.append(",");
			csvWriter.append("numImports");
			csvWriter.append(",");
			csvWriter.append("numComments");
			csvWriter.append(",");
			csvWriter.append("Buggy");
			csvWriter.append("\n");

            // The following Tree Map is used to insert dataset entries in the csv following the correct order (by version).
			Map<String, Metrics> orderedMap = new TreeMap<>();
			MapIterator dataSetIterator = fileDataset.mapIterator();

			// Iterate over the dataset
			while ( dataSetIterator.hasNext() ) {
				dataSetIterator.next();
				MultiKey key = (MultiKey) dataSetIterator.getKey();

				// Get the metrics list associated to the multikey
				Metrics metrics = (Metrics) fileDataset.get(key.getKey(0), key.getKey(1));

				int version = (int) key.getKey(0);

				if ( version <= lastVersion + 1 ){
					String revisedKey = String.valueOf( version );
					if ( revisedKey.length() == 1 ){ revisedKey = "0" + revisedKey; }
					orderedMap.put( revisedKey + "," + (String)key.getKey(1), metrics );
				}
			}

			for ( Map.Entry<String, Metrics> entry : orderedMap.entrySet() ) {

				Metrics metrics = (Metrics) entry.getValue();
				// Check that the version index is contained in the first half of the releases
				//if (Integer.valueOf(entry.getKey().split(",")[0]) <= (lastVersion) + 1) {
				if ( true ) {
                    int nr =                metrics.getNR();
                    String NR =             Integer.toString((metrics.getNR()));
                    String NAUTH =          Integer.toString(metrics.getAUTHORS().size());
                    String LOC =            Integer.toString((int) ( metrics.getLOC()/nr ) );
                    String AGE =            Integer.toString(metrics.getAGE());
                    String CHURN =          Integer.toString(metrics.getCHURN());
					String LOC_TOUCHED = 	Integer.toString(metrics.getLOC_TOUCHED());
					String AvgLocAdded =    Integer.toString((int) ( metrics.getAVG_LOC_ADDED()/nr ) );
                    String MaxLocAdded =    Integer.toString(metrics.getMAX_LOC_ADDED());
                    String AvgChgSet =      Integer.toString((int) ( metrics.getAVG_CHANGE_SET()/nr ) );
                    String MaxChgSet =      Integer.toString(metrics.getMAX_CHANGE_SET());
					String numImports = 	Integer.toString(metrics.getNumImports());
					String numComments = 	Integer.toString(metrics.getNumComments());
                    String buggy =          metrics.getBUGGYNESS();

					// Append the data to CSV file
					csvWriter.append(entry.getKey().split(",")[0] + "," + entry.getKey().split(",")[1] + "," + metrics.getNR() + "," + NAUTH + ","
							+ LOC + "," + AGE + "," + CHURN + ","+ LOC_TOUCHED + "," + AvgLocAdded + "," +  MaxLocAdded + ","
							+ AvgChgSet + "," + MaxChgSet + ","  + numImports + ","  + numComments + "," +  buggy);

					csvWriter.append("\n");
				}
			}

			// Flish the data to the file
			csvWriter.flush();
		}
	}

    
}
