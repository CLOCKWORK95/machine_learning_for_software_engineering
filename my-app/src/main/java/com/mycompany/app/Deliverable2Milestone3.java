package com.mycompany.app;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class Deliverable2Milestone3 {

	private static final String TRAINING = "_training.arff";
	private static final String TESTING = "_testing.arff";

	public static void evaluate() throws Throwable{

		// Declare the list of the dataset names
		String[] projects = {"storm", "bookkeeper"};

		// Declare the number of revision for each dataset
		Integer[] limits = {14,8};

		// Open the FileWriter for the output file
		try (FileWriter csvWriter = new FileWriter("/home/gianmarco/Scrivania/ML_4_SE/my-app/src/main/java/com/mycompany/app/output/outputut_D2M3.csv")) {

			// Append the first line of the result file
			csvWriter.append("Dataset,# Training,% Training,% Defect Training,%Defect Testing,Classifier,Balancing,FeatureSelection,TP,FP,TN,FN,Precision,Recall,ROC Area,Kappa\n");	

			// For each project...
			for (int j = 0; j < projects.length; j++) {

				// Iterate over the single version for the WalkForward technique...
				for (int i = 1; i < limits[j]; i++) {

					// For training and testing, get the number of buggy, non buggy and total instancies					
					List<Integer> resultTraining = D2M2Utils.walkForwardTraining(projects[j], i);
					List<Integer> resultTesting = D2M2Utils.walkForwardTesting(projects[j], i+1);

					double percentTraining = resultTraining.get(0) / (double)(resultTraining.get(0) + resultTesting.get(0));
					double percentDefectTraining = resultTraining.get(1) / (double)resultTraining.get(0);
					double percentDefectTesting = resultTesting.get(1) / (double)resultTesting.get(0);
					double percentageMajorityClass = 1 - ( (resultTraining.get(1) + resultTesting.get(1)) / (double)(resultTraining.get(0) + resultTesting.get(0)));

					// Create the ARFF file for the training, till the i-th version
					DataSource source2 = new DataSource("/home/gianmarco/Scrivania/ML_4_SE/my-app/src/main/java/com/mycompany/app/output/"+projects[j] + TRAINING);
					Instances testingNoFilter = source2.getDataSet();
					
					// Create the ARFF file for testing, with the i+1 version
					DataSource source = new DataSource("/home/gianmarco/Scrivania/ML_4_SE/my-app/src/main/java/com/mycompany/app/output/" +projects[j] + TESTING);
					Instances noFilterTraining = source.getDataSet();
					
					// Apply sampling to the two datasets
					List<String> samplingResult = D2M3Utils.applySampling(noFilterTraining, testingNoFilter, percentageMajorityClass, "False");
					for (String result : samplingResult) {
						csvWriter.append(projects[j] + "," + i  + "," + percentTraining  + "," + percentDefectTraining  + "," + percentDefectTesting +"," + result);
					}
					
					// Apply feature selection to the two datasets
					List<String> featureSelectionResult = D2M3Utils.applyFeatureSelection(noFilterTraining, testingNoFilter, percentageMajorityClass);
					for (String result : featureSelectionResult) {
						csvWriter.append(projects[j] + "," + i  + "," + percentTraining  + "," + percentDefectTraining  + "," + percentDefectTesting +"," + result);
					}	

				}
				// Delete the temp file
				Files.deleteIfExists(Paths.get(projects[j] + TESTING));
				Files.deleteIfExists(Paths.get(projects[j] + TRAINING));
			}

			// Flush to file
			csvWriter.flush();
		}
	}
}