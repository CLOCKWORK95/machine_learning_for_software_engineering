package com.mycompany.app;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import weka.core.Instances;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.lazy.IBk;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClassifierModel {

    private String[]        projects = {"storm"};
    private Integer[]       limits = {14};
	private final String    TRAINING = "_training.arff";
	private final String    TESTING = "_testing.arff";
    private String 			path_to_dir = "/home/gianmarco/Scrivania/ML_4_SE/my-app/src/main/java/com/mycompany/app/";


	public void evaluate() throws Exception{

		// For each project...
		for ( int j = 0; j < this.projects.length; j++ ) {

			// Open the FileWriter for the output file
			try ( FileWriter csvWriter = new FileWriter( path_to_dir + "output/" + this.projects[j]+ "_evaluation.csv" ) ) {

				// Append the first line of the evaluation results file.
				csvWriter.append("Dataset,#TrainingRelease,Classifier,Precision,Recall,AUC,Kappa,Accuracy\n");

				// Iterate over the single version for the WalkForward technique...
				for ( int i = 1; i < this.limits[j]; i++ ) {

					// Create the ARFF file for the training, till the i-th version
					walkForwardTraining( this.projects[j], i );

					// Create the ARFF file for testing, with the i+1 version
					walkForwardTesting( projects[j] , i + 1 );

					// Read the Datasource created before and get each dataset
					DataSource  source1 = new DataSource( path_to_dir + "output/" + projects[j] + TRAINING);
					Instances   trainingSet = source1.getDataSet();
					DataSource  source2 = new DataSource( path_to_dir + "output/" + projects[j] + TESTING);
					Instances   testSet = source2.getDataSet();

					// Get the number of attributes
					int numAttr = trainingSet.numAttributes();

					/* Set the number of attributes for each dataset,
					 * remembering that the last attribute is the one that we want to predict
					 * */
					trainingSet.setClassIndex(numAttr - 1);
					testSet.setClassIndex(numAttr - 1);

					// Get the three classifier
					IBk             classifierIBk = new IBk();
					RandomForest    classifierRF = new RandomForest();
					NaiveBayes      classifierNB = new NaiveBayes();

					// Build the classifier
					classifierNB.buildClassifier(trainingSet);
					classifierRF.buildClassifier(trainingSet);
					classifierIBk.buildClassifier(trainingSet);

					// Get an evaluation object
					Evaluation eval = new Evaluation(trainingSet);	

					// Evaluate each model and add the result to the output file
					eval.evaluateModel(classifierNB, testSet); 
					csvWriter.append(projects[j] + "," + i + ",NaiveBayes," + eval.precision(0) + "," + eval.recall(0) +  "," + eval.areaUnderROC(0) + "," + eval.kappa() + "," + (1-eval.errorRate()) + "\n");

					eval.evaluateModel(classifierRF, testSet); 
					csvWriter.append(projects[j] + "," + i + ",RandomForest," + eval.precision(0) + "," + eval.recall(0) +  "," + eval.areaUnderROC(0) + "," + eval.kappa() + "," + (1-eval.errorRate()) + "\n");

					eval.evaluateModel(classifierIBk, testSet); 
					csvWriter.append(projects[j] + "," + i + ",IBk," + eval.precision(0) + "," + eval.recall(0) +  "," + eval.areaUnderROC(0) + "," + eval.kappa() + "," + (1-eval.errorRate()) +"\n");

				}

				// Delete the temp file
				//Files.deleteIfExists(Paths.get( path_to_dir + "output/" + projects[j] + TESTING ));
				//Files.deleteIfExists(Paths.get( path_to_dir + "output/" + projects[j] + TRAINING ));
				csvWriter.flush();
			}

			// Flush the output file to disk
		}
	}


    /*  This method reads a row from the original csv file and replaces it with all of its content 
        but the first two column values. Also, it returns 0 if the line corresponds to a non-buggy 
        version of the file, 1 otherwise. */
    public int appendToCSV( FileWriter csvWriter, String line ) throws IOException {
		int bug = 0;
		String[] array = line.split(",");
		for ( int i = 2; i < array.length; i++ ) {
			if (i == array.length - 1) {
				if(array[i].equals("Yes"))
					bug += 1;
				csvWriter.append(array[i] + "\n");
			} else {
				csvWriter.append(array[i] + ",");
			}
		}
		return bug;
	}


    /*  This function build the ARFF file for the specified project used as Training Set.
	    param : projectName, the name of the project.
	    param : trainingLimit, the index of the last version to be included in the training set. */ 
	public List<Integer> walkForwardTraining( String projectName, int trainingLimit ) throws IOException {

		int counterElement = 0;
		int counterBuggies = 0;

		ArrayList<Integer> counterList = new ArrayList<>();

		// Create the output ARFF file (.arff)
		try ( FileWriter csvWriter = new FileWriter( path_to_dir + "output/" + projectName + TRAINING ) ) {

			// Append the static line of the ARFF file
			csvWriter.append("@relation " + projectName + "\n\n");
			csvWriter.append("@attribute NumberRevisions real\n");
			csvWriter.append("@attribute NumberAuthors real\n");
			csvWriter.append("@attribute LOC real\n");
			csvWriter.append("@attribute AGE real\n");
			csvWriter.append("@attribute CHURN real\n");
			csvWriter.append("@attribute MaxLocAdded real\n");
			csvWriter.append("@attribute AvgChgSet real\n");
			csvWriter.append("@attribute MaxChgSet real\n");
			csvWriter.append("@attribute AvgLocAdded real\n");
			csvWriter.append("@attribute Buggy {Yes, No}\n\n");
			csvWriter.append("@data\n");

			// Read the project dataset
			try ( BufferedReader br = new BufferedReader( new FileReader( path_to_dir + "output/" + projectName + "_dataset.csv" ) ) ){ 

				// Skip the first line (contains just column name)
				String line = br.readLine();

				// Read till the last row 
				while ( ( line = br.readLine() ) != null ){  

					// Check if the version number is contained in the limit index
					if ( Integer.parseInt( line.split(",")[0] ) <= trainingLimit ) {

						counterElement = counterElement + 1;

						counterBuggies = counterBuggies + appendToCSV( csvWriter, line );
					}
				}

				// Flush the file to the disk
				csvWriter.flush();

				counterList.add(counterElement);
				counterList.add(counterBuggies);

				return counterList;

			}
		}
	}



    /*  This function build the ARFF file for the specific project relative to the Test Set.
	    param : projectName, the name of the project.
	    param : testing, the index of the version to be included in the test set.  */ 
	public List<Integer> walkForwardTesting( String projectName, int testing ) throws IOException {

		int counterElement = 0;
		int counterBuggies = 0;
		ArrayList<Integer> counterList = new ArrayList<>();
		// Create the output ARFF file (.arff)
		try ( FileWriter csvWriter = new FileWriter( path_to_dir + "output/" + projectName + TESTING ) ) {

			// Append the static line of the ARFF file
			csvWriter.append("@relation " + projectName + "\n\n");
			csvWriter.append("@attribute NumberRevisions real\n");
			csvWriter.append("@attribute NumberAuthors real\n");
			csvWriter.append("@attribute LOC real\n");
			csvWriter.append("@attribute AGE real\n");
			csvWriter.append("@attribute CHURN real\n");
			csvWriter.append("@attribute MaxLocAdded real\n");
			csvWriter.append("@attribute AvgChgSet real\n");
			csvWriter.append("@attribute MaxChgSet real\n");
			csvWriter.append("@attribute AvgLocAdded real\n");
			csvWriter.append("@attribute Buggy {Yes, No}\n\n");
			csvWriter.append("@data\n");

			// Read the project dataset
			try ( BufferedReader br = new BufferedReader( new FileReader( path_to_dir + "output/" + projectName + "_dataset.csv" ) ) ){  

				// Skip the first line (contains just column name)
				String line = br.readLine();

				// Read till the last row 
				while ( ( line = br.readLine() ) != null ){  

					// Check if the version number is equal to the one equal to the test index
					if ( Integer.parseInt( line.split(",")[0] ) == testing ) {

						counterElement = counterElement + 1;

						// Append the row readed from the CSV file, but without the first 2 column
						counterBuggies = counterBuggies + appendToCSV( csvWriter, line );
					}
				}

				// Flush the file to the disk
				csvWriter.flush();
				counterList.add(counterElement);
				counterList.add(counterBuggies);

			}
		}
		return counterList;
	}




    public static String getMetrics( Evaluation eval, String classifier, String balancing, String featureSelection ) {
		return classifier + "," + balancing + "," + featureSelection + "," + eval.truePositiveRate(1)  + "," + eval.falsePositiveRate(1)  + "," + eval.trueNegativeRate(1)  + "," + eval.falseNegativeRate(1)  + "," + eval.precision(1)  + "," + eval.recall(1)  + "," + eval.areaUnderROC(1)  + "," + eval.kappa() + "\n";
	}


}
