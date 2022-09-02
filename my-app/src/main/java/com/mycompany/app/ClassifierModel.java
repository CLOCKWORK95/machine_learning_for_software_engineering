package com.mycompany.app;

import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import weka.core.converters.ConverterUtils.DataSource;
import java.util.logging.Logger;
import weka.classifiers.CostMatrix;
import weka.classifiers.trees.J48;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.core.Instances;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;

public class ClassifierModel {

    private String[]        		projects = {"storm", "bookkeeper"};
    private Integer[]       		limits = {14,8};
	private static final String    	TRAINING = "_training.arff";
	private static final String    	TESTING = "_testing.arff";
	private static final String		PATH_TO_OUTPUTDIR = getOutputDirPath();
	private static final String		EVALUATION_FILE_FORMAT = "_evaluation.csv";
	private static final String		DATASET_FILE_FORMAT = "_dataset.csv";
	private static final String 	OVER_SAMPLING = "Over sampling";
	private static final String 	UNDER_SAMPLING = "Under sampling";
	private static final String 	SMOTE = "Smote";
	private static final String		COST_SENSITIVE = "Cost Sensitive";
	private static final String 	NO_SAMPLING = "No sampling";
	private static final Logger 	logger = Logger.getLogger(ClassifierModel.class.getName());
	private static final String 	FEATURE_SELECTION = "False";


	public static String getCurrentDirectory() {
		return System.getProperty("user.dir");
	}


	public static String getOutputDirPath(){
		return getCurrentDirectory() + "/src/main/java/com/mycompany/app/output/";
	}



	public void evaluateSampling() throws Exception{

		// For each project...
		for ( int j = 0; j < this.projects.length; j++ ) {

			// Open the FileWriter for the output file
			try ( FileWriter csvWriter = new FileWriter( PATH_TO_OUTPUTDIR + this.projects[j]+ EVALUATION_FILE_FORMAT ) ) {
				
				// Iterate over the single version for the WalkForward technique...
				for ( int i = 1; i < this.limits[j]; i++ ) {

					// Create the ARFF file for the training, till the i-th version
					List<Integer> resultTraining = walkForwardTraining( this.projects[j], i );
					List<Integer> resultTesting;
					// Create the ARFF file for testing, with the i+1 version
					try{
						resultTesting = walkForwardTesting( projects[j] , i + 1 );
					} catch( NoTestSetAvailableException e ){
						continue;
					}

					// Append the first line of the evaluation results file.
					csvWriter.append("\nDataset,# Training,TrainingSet Size,TestSet Size,% Training,% Defect Training,%Defect Testing,Classifier,Balancing,FeatureSelection,TP,FP,TN,FN,Precision,Recall,ROC Area,Kappa,Accuracy\n");

					double percentTraining = resultTraining.get(0) / (double)(resultTraining.get(0) + resultTesting.get(0));
					double percentDefectTraining = resultTraining.get(1) / (double)resultTraining.get(0);
					double percentDefectTesting = resultTesting.get(1) / (double)resultTesting.get(0);
					double percentageMajorityClass = 1 - ( (resultTraining.get(1) + resultTesting.get(1)) / (double)(resultTraining.get(0) + resultTesting.get(0)));

					// Read the Datasource created before and get each dataset
					DataSource  source1 = new DataSource( PATH_TO_OUTPUTDIR + projects[j] + TRAINING);
					Instances   trainingSet = source1.getDataSet();
					DataSource  source2 = new DataSource( PATH_TO_OUTPUTDIR + projects[j] + TESTING);
					Instances   testSet = source2.getDataSet();

					// Apply sampling to the two datasets
					List<String> samplingResult = applySampling( trainingSet, testSet, percentageMajorityClass, FEATURE_SELECTION );
					for (String result : samplingResult) {
						csvWriter.append(projects[j] + "," + i  + "," + resultTraining.get(0) + "," + resultTesting.get(0) + "," + percentTraining  + "," + percentDefectTraining  + "," + percentDefectTesting +"," + result);
					}
				}
				csvWriter.flush();
			}
		}
	}



	public void evaluateSamplingModified() throws Exception{

		// For each project...
		for ( int j = 0; j < this.projects.length; j++ ) {

			// Open the FileWriter for the output file
			try ( FileWriter csvWriter = new FileWriter( PATH_TO_OUTPUTDIR + this.projects[j]+ EVALUATION_FILE_FORMAT ) ) {

				String 						projectName = this.projects[j];
				ModifiedWalkForwardReader 	reader = new ModifiedWalkForwardReader( limits[j], PATH_TO_OUTPUTDIR + projectName + DATASET_FILE_FORMAT );
				int 						steps = reader.getSteps();
				
				// Append the first line of the evaluation results file.
				csvWriter.append("\nDataset,# Training,TrainingSet Size,TestSet Size,% Training,% Defect Training,%Defect Testing,Classifier,Balancing,FeatureSelection,TP,FP,TN,FN,Precision,Recall,ROC Area,Kappa,Accuracy\n");

				// Iterate over the single version for the WalkForward technique...
				for ( int i = 1; i < steps; i++ ) {

					reader.reset();
					// Create the ARFF file for the training and test sets: training set stops at the i-th fold, test set is the (i+1)th fold.
					reader = modifiedWalkForwardTrainingAndTest( projectName, reader, i );
					List<Integer> resultTraining = reader.getCounterResults().subList( 0, 2 );
					List<Integer> resultTesting = reader.getCounterResults().subList( 2, 4 );

					double percentTraining = resultTraining.get(0) / (double)(resultTraining.get(0) + resultTesting.get(0));
					double percentDefectTraining = resultTraining.get(1) / (double)resultTraining.get(0);
					double percentDefectTesting = resultTesting.get(1) / (double)resultTesting.get(0);
					double percentageMajorityClass = 1 - ( (resultTraining.get(1) + resultTesting.get(1)) / (double)(resultTraining.get(0) + resultTesting.get(0)));

					// Read the Datasource created before and get each dataset
					DataSource  source1 = new DataSource( PATH_TO_OUTPUTDIR + projects[j] + TRAINING);
					Instances   trainingSet = source1.getDataSet();
					DataSource  source2 = new DataSource( PATH_TO_OUTPUTDIR + projects[j] + TESTING);
					Instances   testSet = source2.getDataSet();
					
					if ( FEATURE_SELECTION.equals( "True" ) ) {
						List<Instances> datasets = WekaFeatureSelection.featureSelection( trainingSet, testSet );
						trainingSet = datasets.get( 0 );
						testSet = datasets.get( 1 );
					}

					// Apply sampling to the two datasets
					List<String> samplingResult = applySampling( trainingSet, testSet, percentageMajorityClass, FEATURE_SELECTION );
					for (String result : samplingResult) {
						csvWriter.append(projects[j] + "," + i  + "," + resultTraining.get(0) + "," + resultTesting.get(0) + "," + percentTraining  + "," + percentDefectTraining  + "," + percentDefectTesting +"," + result);
					}

				}

				csvWriter.flush();
			}

			// Flush the output file to disk
		}
	}



	public void evaluate() throws Exception{

		// For each project...
		for ( int j = 0; j < this.projects.length; j++ ) {

			// Open the FileWriter for the output file
			try ( FileWriter csvWriter = new FileWriter( PATH_TO_OUTPUTDIR + this.projects[j]+ EVALUATION_FILE_FORMAT ) ) {

				// Append the first line of the evaluation results file.
				csvWriter.append("Dataset,#TrainingRelease,Classifier,Precision,Recall,AUC,Kappa,Accuracy\n");

				// Iterate over the single version for the WalkForward technique...
				for ( int i = 1; i < this.limits[j]; i++ ) {

					// Create the ARFF file for the training, till the i-th version
					walkForwardTraining( this.projects[j], i );

					// Create the ARFF file for testing, with the i+1 version
					try{
						walkForwardTesting( projects[j] , i + 1 );
					} catch( NoTestSetAvailableException e ){
						continue;
					}

					// Read the Datasource created before and get each dataset
					DataSource  source1 = new DataSource( PATH_TO_OUTPUTDIR + projects[j] + TRAINING);
					Instances   trainingSet = source1.getDataSet();
					DataSource  source2 = new DataSource( PATH_TO_OUTPUTDIR + projects[j] + TESTING);
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
					csvWriter.append(projects[j] + "," + i + ",IBk," + eval.precision(0) + "," + eval.recall(0) +  "," + eval.areaUnderROC(0) + "," + eval.kappa() + "," + (1-eval.errorRate()) +"\n\n");

				}
				csvWriter.flush();
			}
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



	public void appendHeaderToArf( FileWriter csvWriter, String projectName) throws IOException{
		// Append the header line to the ARFF file
		csvWriter.append("@relation " + projectName + "\n\n");
		csvWriter.append("@attribute NumberRevisions real\n");
		csvWriter.append("@attribute NumberAuthors real\n");
		csvWriter.append("@attribute LOC real\n");
		csvWriter.append("@attribute AGE real\n");
		csvWriter.append("@attribute CHURN real\n");
		csvWriter.append("@attribute LOC_TOUCHED real\n");
		csvWriter.append("@attribute AvgLocAdded real\n");
		csvWriter.append("@attribute MaxLocAdded real\n");
		csvWriter.append("@attribute AvgChgSet real\n");
		csvWriter.append("@attribute MaxChgSet real\n");
		csvWriter.append("@attribute numImports real\n");
		csvWriter.append("@attribute numComments real\n");
		csvWriter.append("@attribute Buggy {Yes, No}\n\n");
		csvWriter.append("@data\n");
	}



    /*  This method build the ARFF file for the specified project used as Training Set.
	    param : projectName, the name of the project.
	    param : trainingLimit, the index of the last version to be included in the training set. */ 
	public List<Integer> walkForwardTraining( String projectName, int trainingLimit ) {

		int counterElement = 0;
		int counterBuggies = 0;

		ArrayList<Integer> counterList = new ArrayList<>();

		// Create the output ARFF file (.arff)
		try ( FileWriter csvWriter = new FileWriter( PATH_TO_OUTPUTDIR + projectName + TRAINING ) ) {

			// Append the static line of the ARFF file
			appendHeaderToArf(csvWriter, projectName);

			// Read the project dataset
			try(BufferedReader br = new BufferedReader( new FileReader( PATH_TO_OUTPUTDIR + projectName + DATASET_FILE_FORMAT ) )){

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
				br.close();
				// Flush the file to the disk
				csvWriter.flush();
			}

			counterList.add(counterElement);
			counterList.add(counterBuggies);

		} catch(Exception e){
			logger.info("Some problems occurred while writing the arff file.");
		} 
		return counterList;
	}



    /*  This method build the ARFF file for the specific project relative to the Test Set.
	    param : projectName, the name of the project.
	    param : testing, the index of the version to be included in the test set.  */ 
	public List<Integer> walkForwardTesting( String projectName, int testing ) throws NoTestSetAvailableException{
	
		int counterElement = 0;
		int counterBuggies = 0;
		ArrayList<Integer> counterList = new ArrayList<>();
		// Create the output ARFF file (.arff)
		try ( FileWriter csvWriter = new FileWriter( PATH_TO_OUTPUTDIR + projectName + TESTING ) ) {

			// Append the static line of the ARFF file
			appendHeaderToArf(csvWriter, projectName);

			// Read the project dataset
			try(BufferedReader br = new BufferedReader( new FileReader( PATH_TO_OUTPUTDIR + projectName + DATASET_FILE_FORMAT ))){ 

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
				br.close();
				// Flush the file to the disk
				csvWriter.flush();
			}

			counterList.add(counterElement);
			counterList.add(counterBuggies);

		} catch( Exception e){
			logger.info("Some problems occurred while writing the arff file.");
		} 
		if ( counterElement <= 5 ) {
			NoTestSetAvailableException e = new NoTestSetAvailableException("There are no entries in version", testing );
			throw(e);
		}
		return counterList;
	}



   	/*  This function build the ARFF files for the specified project used as Training and Test Sets.
		This walk forward tecnique divides the whole dataset into 'trainingSteps' folds of the same size, 
		in order to implement the classic walk forward procedure ( in other words, it doesn't care about version 
		numbers, but still preserves the order of the time-series ).
		This implementation is thought to mantain correct balancing in training and testing set sizes over 
		the iterations of the algorithm, as the version-based implementation was quite unbalanced in sizes.
		param : projectName		the name of the project.
		param : reader			a structured reader object implemented to perform the walkforward task.
		param : trainingSteps	the number of folds to divide the dataset into.	 */ 
	public ModifiedWalkForwardReader modifiedWalkForwardTrainingAndTest( String projectName, ModifiedWalkForwardReader reader, int trainingSteps ) throws NoTestSetAvailableException {

		int 	entries = 0;
		int 	bugs = 0;
		int 	stepsDone = 1;

		// Create the output ARFF file (.arff)
		try ( FileWriter csvWriter = new FileWriter( PATH_TO_OUTPUTDIR + projectName + TRAINING ) ) {

			// Append the static line of the ARFF file which will be used as TRAINING SET for the evaluation in the WalkForward iteration.
			appendHeaderToArf(csvWriter, projectName);

			// Read the project dataset in csv format
			// Skip the first line (contains just column name)
			String line = reader.getBr().readLine();
			// Read till the last row 
			while ( ( line = reader.getBr().readLine() ) != null ){  
				if ( entries != 0 && entries % reader.getStep() == 0 && stepsDone == trainingSteps ) {
					break;
				} 
				if ( entries != 0 && entries % reader.getStep() == 0 && stepsDone < trainingSteps ) {
					stepsDone = stepsDone + 1;
				} 
				entries = entries + 1;
				bugs = bugs + appendToCSV( csvWriter, line );

			}
			// Flush the file to the disk
			csvWriter.flush();
			reader.appendCounterResult( entries );
			reader.appendCounterResult( bugs );
			
		} catch( Exception e){
			logger.info("Some problems occurred while writing the arff file.");
		}

		entries = 0;
		bugs = 0;

		// Create the output ARFF file (.arff) which will be used as TEST SET for the evaluation in the WalkForward iteration.
		try ( FileWriter csvWriter = new FileWriter( PATH_TO_OUTPUTDIR + projectName + TESTING ) ) {

			// Append the static line of the ARFF file
			appendHeaderToArf(csvWriter, projectName);

			// Read the project dataset
			String line;
			// Read untill the next step is complete.
			while ( ( line = reader.getBr().readLine() ) != null ){  

				if ( entries < reader.getStep() ) {

					entries = entries + 1;
					// Append the row readed from the CSV file, but without the first 2 column
					bugs = bugs + appendToCSV( csvWriter, line );
				}
			}
			// Flush the file to the disk
			csvWriter.flush();
			reader.appendCounterResult( entries );
			reader.appendCounterResult( bugs );

		} catch( Exception e){
			logger.info("Some problems occurred while writing the arff file.");
		}

		if ( entries <= 5 ) {
			NoTestSetAvailableException e = new NoTestSetAvailableException("There are not enough entries to build test set! " + 
													"Please decrease the integer steps parameter of ModifiedWalkForwardReader." );
			throw(e);
		}

		return reader;
	}
	


	/* 	This method applies different sampling techniques to training and test sets, and evaluates the model. 
		param training, the Evaluation object
	  	param testing, the name of the classifier
	  	param percentageMajorityClass, the percentage in the training set of the majority class
	  	return result, list string with the list of metrics separated with ',' of the various run	*/
	public List<String> applySampling( Instances training, Instances testing, double percentageMajorityClass, String featureSelection ) throws SamplingException {

		ArrayList<String> result = new ArrayList<>();

		IBk classifierIBk = new IBk();
		RandomForest classifierRF = new RandomForest();
		NaiveBayes classifierNB = new NaiveBayes();
		CostSensitiveClassifier classifierCS = new CostSensitiveClassifier();

		int numAttrNoFilter = training.numAttributes();
		training.setClassIndex(numAttrNoFilter - 1);
		testing.setClassIndex(numAttrNoFilter - 1);

		// Build the classifiers
		try {
			classifierNB.buildClassifier(training);
			classifierRF.buildClassifier(training);
			classifierIBk.buildClassifier(training);
			
			classifierCS.setClassifier(new J48());
			classifierCS.setCostMatrix(createCostMatrix(10, 1));
			classifierCS.buildClassifier(training);
		} catch (Exception e) {
			throw new SamplingException("Error building the classifier.");
		}

		Evaluation eval;
		try {

			// NO SAMPLING --------------------------------------------------------------
			eval = new Evaluation( testing );
			eval = applyFilterForSampling(null, eval, training, testing, classifierRF);
			addResult(eval, result, "RF", NO_SAMPLING, featureSelection);

			eval = new Evaluation( testing );
			eval = applyFilterForSampling(null, eval, training, testing, classifierIBk);
			addResult(eval, result, "IBk", NO_SAMPLING, featureSelection);

			eval = new Evaluation( testing );
			eval = applyFilterForSampling( null, eval, training, testing, classifierNB );
			addResult( eval, result, "NB", NO_SAMPLING, featureSelection );

			// Evaluate the Cost Sensitive Classifier
			eval = new Evaluation(testing, classifierCS.getCostMatrix());
			eval = applyFilterForSampling(null, eval, training, testing, classifierCS);
			addResult(eval, result, COST_SENSITIVE, NO_SAMPLING, featureSelection);
			//-----------------------------------------------------------------------------


			// UNDER SAMPLING the Majority Class ------------------------------------------
			FilteredClassifier fc = new FilteredClassifier();
			SpreadSubsample  underSampling = new SpreadSubsample();
			underSampling.setInputFormat( training );
			underSampling.setDistributionSpread( 1.0 );
			fc.setFilter( underSampling );

			// Evaluate the three classifiers
			eval = new Evaluation( testing );
			eval = applyFilterForSampling(fc, eval, training, testing, classifierRF);
			addResult(eval, result, "RF", UNDER_SAMPLING, featureSelection);

			eval = new Evaluation( testing );
			eval = applyFilterForSampling(fc, eval, training, testing, classifierIBk);
			addResult(eval, result, "IBk", UNDER_SAMPLING, featureSelection);

			eval = new Evaluation( testing );
			eval = applyFilterForSampling(fc, eval, training, testing, classifierNB);
			addResult(eval, result, "NB", UNDER_SAMPLING, featureSelection);

			// Evaluate the Cost Sensitive Classifier
			eval = new Evaluation(testing, classifierCS.getCostMatrix());
			eval = applyFilterForSampling(fc, eval, training, testing, classifierCS);
			addResult(eval, result, COST_SENSITIVE, UNDER_SAMPLING, featureSelection);
			//-----------------------------------------------------------------------------



			// OVER SAMPLING the Minority class--------------------------------------------
			fc = new FilteredClassifier();
			Resample  overSampling = new Resample();
			overSampling.setInputFormat( training );
			overSampling.setSampleSizePercent( 2 * percentageMajorityClass * 100 );
			overSampling.setNoReplacement( false );
			overSampling.setBiasToUniformClass( 1.0 );
			fc.setFilter( overSampling );

			// Evaluate the three classifiers
			eval = new Evaluation( testing );	
			eval = applyFilterForSampling(fc, eval, training, testing, classifierRF);
			addResult(eval, result, "RF", OVER_SAMPLING, featureSelection);

			eval = new Evaluation( testing );
			eval = applyFilterForSampling(fc, eval, training, testing, classifierIBk);
			addResult(eval, result, "IBk", OVER_SAMPLING, featureSelection);

			eval = new Evaluation( testing );
			eval = applyFilterForSampling(fc, eval, training, testing, classifierNB);
			addResult(eval, result, "NB", OVER_SAMPLING, featureSelection);

			// Evaluate the Cost Sensitive Classifier
			eval = new Evaluation(testing, classifierCS.getCostMatrix());
			eval = applyFilterForSampling(fc, eval, training, testing, classifierCS);
			addResult(eval, result, COST_SENSITIVE, OVER_SAMPLING, featureSelection);
			//-----------------------------------------------------------------------------


			// SMOTE-----------------------------------------------------------------------
			fc = new FilteredClassifier();
			SMOTE smote = new SMOTE();
			smote.setInputFormat( training );
			fc.setFilter( smote );

			// Evaluate the three classifiers
			eval = new Evaluation( testing );	
			eval = applyFilterForSampling(fc, eval, training, testing, classifierRF);
			addResult(eval, result, "RF", SMOTE, featureSelection);

			eval = new Evaluation( testing );
			eval = applyFilterForSampling(fc, eval, training, testing, classifierIBk);
			addResult(eval, result, "IBk", SMOTE, featureSelection);

			eval = new Evaluation( testing );
			eval = applyFilterForSampling(fc, eval, training, testing, classifierNB);
			addResult(eval, result, "NB", SMOTE, featureSelection);

			// Evaluate the Cost Sensitive Classifier
			eval = new Evaluation(testing, classifierCS.getCostMatrix());
			eval = applyFilterForSampling(fc, eval, training, testing, classifierCS);
			addResult(eval, result, COST_SENSITIVE, SMOTE, featureSelection);
			//-----------------------------------------------------------------------------

		} catch ( Exception e ) {
			throw new SamplingException("Errore nell'applicazione del sampling.");
		}	

		return result;


	}



	private CostMatrix createCostMatrix(double weightFalsePositive, double weightFalseNegative) {
		CostMatrix costMatrix = new CostMatrix(2);
		costMatrix.setCell(0, 0, 0.0);
		costMatrix.setCell(1, 0, weightFalsePositive);
		costMatrix.setCell(0, 1, weightFalseNegative);
		costMatrix.setCell(1, 1, 0.0);
		return costMatrix;
	}



	public Evaluation applyFilterForSampling( FilteredClassifier fc, Evaluation eval, Instances training, Instances testing, AbstractClassifier classifierName ){

		// In filter needed, apply it and evaluate the model 
		try {
			if (fc != null) {
				fc.setClassifier( classifierName );
				fc.buildClassifier( training );
				eval.evaluateModel( fc, testing );

				// If not... Just evaluate the model
			} else {
				eval.evaluateModel( classifierName, testing );

			}
		} catch (Exception e) {
			logger.info("Attenzione. Classe minoritaria insufficiente per SMOTE.");
		}
		return eval;
	}



	/*	This method build the ARFF file for the specific project relative to the testing set	  
	   	param eval, the Evaluation object
	   	param result, the list needed to append the results
	   	param classifierAbb, the abbreviation of the classifier
	   	param sampling, the name of sampling technique
	   	param featureSelection, the name of feature selection technique		*/ 
	public void addResult(Evaluation eval, List<String> result, String classifierAbb, String sampling, String featureSelection) throws Exception {
		// Add the result to the List of instances metrics
		result.add( getMetrics( eval,classifierAbb, sampling, featureSelection ) );

	}



    public String getMetrics( Evaluation eval, String classifier, String balancing, String featureSelection ){
		return classifier + "," + balancing + "," + featureSelection + "," + eval.truePositiveRate(0)  + "," + eval.falsePositiveRate(0)  + "," + eval.trueNegativeRate(0)  + "," + eval.falseNegativeRate(0)  + "," + eval.precision(0)  + "," + eval.recall(0)  + "," + eval.areaUnderROC(0)  + "," + eval.kappa() + "," + (1-eval.errorRate()) + "\n";
	}



}
