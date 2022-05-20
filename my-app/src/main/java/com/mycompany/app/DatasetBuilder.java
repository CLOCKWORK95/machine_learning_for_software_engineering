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

    //------------------------------------- Builders ---------------------------------------------

    public DatasetBuilder( Multimap<LocalDate,String> versionMap ){
        this.versionMap = versionMap;
        this.lastVersion =  (versionMap.size() / 2) / 2;
    }

    //------------------------------------- Methods ----------------------------------------------

    /*  This Method is used to populate the  Multi Key Map representing the dataset to be created. */
    public void populateFileDataset( ArrayList<IssueObject> issues ){
        int     version;
        String  filepath;
        Metrics newMetrics = new Metrics();
        for ( IssueObject issue : issues ){
            for ( CommitObject commit : issue.getCommits() ){
                for ( FileObject file : commit.getFiles() ){
                    version = file.getVersion();
                    filepath = file.getFilepath();
                    newMetrics.setVersion(version);
                    newMetrics.setFilepath(filepath);
                    newMetrics.setNR( 1 );
                    newMetrics.setAGE( file.getAGE());
                    newMetrics.setCHURN(file.getCHURN());
                    newMetrics.appendAuthor(file.getAUTHOR());
                    newMetrics.setMAX_LOC_ADDED(file.getLinesAdded());
                    newMetrics.setLOC(file.getLOC());
                    newMetrics.setAVG_LOC_ADDED(file.getLinesAdded());
                    newMetrics.setAVG_CHANGE_SET(file.getChangeSetSize());
                    newMetrics.setMAX_CHANGE_SET(file.getChangeSetSize());
                    newMetrics.setBUGGYNESS(file.getBuggyness());
                    if( !fileDataset.containsKey(version,filepath) ){
                        fileDataset.put( version, filepath, newMetrics );
                    } else{
                        Metrics oldMetrics = ( Metrics ) fileDataset.get( version, filepath );
                        newMetrics.update( oldMetrics );
                        fileDataset.put( version, filepath, newMetrics );
                    }
                }
            }
        }
    }


	public void writeToCSV(String projectName) throws IOException {

		// Set the name of the file
		try (FileWriter csvWriter = new FileWriter("output/" + projectName + "_dataset.csv")) {

			/*	
			 * Metrics Data Structure
             *   (version)
             *   (filepath)
			 *  0 - NumberRevisions
			 *  1 - NumberAuthors
			 *  2 - LOC
			 *  3 - AGE
			 *  4 - CHURN
			 *  5 - Max_Loc_Added
			 *  6 - Avg_Chg_set
			 *  7 - Max_Chg_Set
			 *  8 - Avg_LOC_Added
			 * 	9 - Buggyness
			 * 
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
			csvWriter.append("MaxLocAdded");
			csvWriter.append(",");
			csvWriter.append("Avg_Chg_Set");
			csvWriter.append(",");
			csvWriter.append("Max_Chg_Set");
			csvWriter.append(",");
			csvWriter.append("Avg_LOC_Added");
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

				orderedMap.put(String.valueOf(key.getKey(0)) + "," + (String)key.getKey(1), metrics);
			}

			for ( Map.Entry<String, Metrics> entry : orderedMap.entrySet() ) {

				Metrics metrics = (Metrics) entry.getValue();
				// Check that the version index is contained in the first half of the releases
				if (Integer.valueOf(entry.getKey().split(",")[0]) <= (lastVersion) + 1) {
                    int nr =                metrics.getNR();
                    String NR =             Integer.toString((metrics.getNR()));
                    String NAUTH =          Integer.toString(metrics.getAUTHORS().size());
                    String LOC =            Integer.toString((int)metrics.getLOC()/nr);
                    String AGE =            Integer.toString(metrics.getAGE());
                    String CHURN =          Integer.toString(metrics.getCHURN());
                    String MaxLocAdded =    Integer.toString(metrics.getMAX_LOC_ADDED());
                    String AvgChgSet =      Integer.toString((int)metrics.getAVG_CHANGE_SET()/nr);
                    String MaxChgSet =      Integer.toString(metrics.getMAX_CHANGE_SET());
                    String AvgLocAdded =    Integer.toString((int)metrics.getAVG_LOC_ADDED()/nr);
                    String buggy =          metrics.getBUGGYNESS();

					// Append the data to CSV file
					csvWriter.append(entry.getKey().split(",")[0] + "," + entry.getKey().split(",")[1] + "," + metrics.getNR() + "," + NAUTH + ","
							+ LOC + "," + AGE + "," + CHURN + "," + MaxLocAdded + ","
							+ AvgChgSet + "," + MaxChgSet + "," + AvgLocAdded + "," + buggy);

					csvWriter.append("\n");
				}
			}

			// Flish the data to the file
			csvWriter.flush();
		}
	}

    
}
