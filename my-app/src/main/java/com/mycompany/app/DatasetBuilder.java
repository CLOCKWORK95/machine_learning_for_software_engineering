package com.mycompany.app;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections4.map.MultiKeyMap;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;


public class DatasetBuilder {

    //------------------------------------ Attributes --------------------------------------------

    // Dataset as a MultiKeyMap with key :<version,filepath> and value <metrics>
	private MultiKeyMap<MultiKey<Object>,Metrics> 			fileDataset = MultiKeyMap.multiKeyMap( new LinkedMap<>() );

	private static 	Logger 									logger = Logger.getLogger(DatasetBuilder.class.getName());

    private Multimap<LocalDate,String>      				versionMap;

    private int                             				lastVersion;

	private String											projectName;

	private static final String								PATH_TO_OUTPUTDIR = getOutputDirPath();

	private static final String								DATASET_FILE_FORMAT = "_dataset.csv";

	private static final String 							PROJECT_DIR = "/home/gianmarco/Scrivania/ML_4_SE";

	private static final String								FILE_EXTENSION = ".java";

	private static final String 							USER_DIR = "user.dir";

    //------------------------------------- Builders ---------------------------------------------

    public DatasetBuilder( String projectName ){
		this.projectName = projectName;
    }

	public void setVersionMap( Multimap<LocalDate,String> versionMap ){
		this.versionMap = versionMap;
		this.lastVersion = (versionMap.size()/2)/2;
	}

    //------------------------------------- Methods ----------------------------------------------

	public static String getCurrentDirectory() {
		return System.getProperty(USER_DIR);
	}


	public static String getOutputDirPath(){
		return getCurrentDirectory() + "/src/main/java/com/mycompany/app/output/";
	}


	public Multimap<LocalDate,String>  getVersionMap(){
		return this.versionMap;
	}


	/*  This Method is used to initialize (with all files of the project) 
		the Multi Key Map representing the dataset to be created. */
    public void initiateFileDataset() throws IOException{

		Metrics newMetrics = new Metrics();
		newMetrics.setNR(1);
		newMetrics.setAGE(0);
		newMetrics.setCHURN(0);
		newMetrics.appendAuthor("");
		newMetrics.setLOC_TOUCHED(0);
		newMetrics.setMAX_LOC_ADDED(0);
		newMetrics.setLOC(0);
		newMetrics.setAVG_LOC_ADDED(0);
		newMetrics.setAVG_CHANGE_SET(0);
		newMetrics.setMAX_CHANGE_SET(0);
		newMetrics.setNumImports(0);
		newMetrics.setNumComments(0);
		newMetrics.setBUGGYNESS("No");

		// Get all the file in the repo folder
		try (Stream<File> fileStream = Files.walk(Paths.get(PROJECT_DIR + "/" + projectName + "/"))
		.filter(Files::isRegularFile).map(Path::toFile)){

			List<File> filesInFolder = fileStream.collect(Collectors.toList());

			// For each file in the folder that ends with .java...
			for (File i : filesInFolder) {
				if (i.toString().endsWith(FILE_EXTENSION)) {
					
					// ... put the pair (version, filePath) in the dataset map
					for (int j = 1; j < (lastVersion) + 1; j++) {
						MultiKey<Object> key = new MultiKey<>( j, i.toString().replace(PROJECT_DIR + "/" + projectName + "/",""));
						putEmptyRecord(key, newMetrics);
					}
				}
			}
		}
	}


	public void putEmptyRecord(MultiKey keys, Metrics emptyMetrics){
		emptyMetrics.setVersion( (int) keys.getKey(0));
		emptyMetrics.setFilepath( (String) keys.getKey(1));
		logger.info("NEW EMPTY RECORD!!");
		fileDataset.put( keys, emptyMetrics );
	}


	public void putRecord(MultiKey keys, Metrics metrics){
		fileDataset.put( keys, metrics );
	}
        
       

    /*  This Method is used to populate the  Multi Key Map representing the dataset to be created. */
    public void populateFileDataset( List<IssueObject> issues ){
        int     version;
        String  filepath;
        for ( IssueObject issue : issues ){
            for ( CommitObject commit : issue.getCommits() ){
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
						logger.info("NEW entry!");
						MultiKey<Object> key = new MultiKey<>(version,filepath);
                        putRecord( key, newMetrics );
                    } else{
                        Metrics oldMetrics = fileDataset.get( version, filepath );
						logger.info("OLD entry!");
                        newMetrics.update( oldMetrics );
                        MultiKey<Object> key = new MultiKey<>(version,filepath);
                        putRecord( key, newMetrics );
                    }
                }
            }
        }
    }


	public void writeToCSV( String projectName ) throws IOException {

		// Set the name of the file
		try ( FileWriter csvWriter = new FileWriter( PATH_TO_OUTPUTDIR + projectName + DATASET_FILE_FORMAT ) ) {

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
				MultiKey<Object> key = (MultiKey<Object>) dataSetIterator.getKey();

				// Get the metrics list associated to the multikey
				Metrics metrics = fileDataset.get(key.getKey(0), key.getKey(1));

				int version = (int) key.getKey(0);

				if ( version <= lastVersion + 1 ){
					String revisedKey = String.valueOf( version );
					if ( revisedKey.length() == 1 ){ revisedKey = "0" + revisedKey; }
					orderedMap.put( revisedKey + "," + (String)key.getKey(1), metrics );
				}
			}

			for ( Map.Entry<String, Metrics> entry : orderedMap.entrySet() ) {

				Metrics metrics = entry.getValue();
				// Check that the version index is contained in the first half of the releases
				if ( true ) {
                    int    nr =             metrics.getNR();
                    String numRev =         Integer.toString((metrics.getNR()));
                    String nAuth =          Integer.toString(metrics.getAUTHORS().size());
                    String loc =            Integer.toString((int) ( metrics.getLOC()/nr ) );
                    String age =            Integer.toString(metrics.getAGE());
                    String churn =          Integer.toString(metrics.getCHURN());
					String locTouched = 	Integer.toString(metrics.getLOC_TOUCHED());
					String avgLocAdded =    Integer.toString((int) ( metrics.getAVG_LOC_ADDED()/nr ) );
                    String maxLocAdded =    Integer.toString(metrics.getMAX_LOC_ADDED());
                    String avgChgSet =      Integer.toString((int) ( metrics.getAVG_CHANGE_SET()/nr ) );
                    String maxChgSet =      Integer.toString(metrics.getMAX_CHANGE_SET());
					String numImports = 	Integer.toString(metrics.getNumImports()/nr);
					String numComments = 	Integer.toString(metrics.getNumComments()/nr);
                    String buggy =          metrics.getBUGGYNESS();

					// Append the data to CSV file
					csvWriter.append(entry.getKey().split(",")[0] + "," + entry.getKey().split(",")[1] + "," + numRev + "," + nAuth + ","
							+ loc + "," + age + "," + churn + ","+ locTouched + "," + avgLocAdded + "," +  maxLocAdded + ","
							+ avgChgSet + "," + maxChgSet + ","  + numImports + ","  + numComments + "," +  buggy);

					csvWriter.append("\n");
				}
			}

			// Flish the data to the file
			csvWriter.flush();
		}
	}

    
}
