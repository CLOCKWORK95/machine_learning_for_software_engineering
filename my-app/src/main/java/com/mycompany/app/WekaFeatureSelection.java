package com.mycompany.app;

import weka.core.Instances;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.core.converters.ConverterUtils.DataSource;
import java.util.List;
import java.util.ArrayList;



public class WekaFeatureSelection{


	public static List<Instances> featureSelection( Instances training, Instances testing ) throws Exception{
		
		// Create "Evaluator" and "Search" Algorithm Objects to perform Attribute selection over the given dataset.
		// Both objects will be used as settings for an Attribute Selection Object.
		CfsSubsetEval 		evaluator = new CfsSubsetEval();
		GreedyStepwise 		search = new GreedyStepwise();
							search.setSearchBackwards(true);
		// Create the Attribute Selection Object and setup it.
		AttributeSelection 	filter = new AttributeSelection();
							filter.setEvaluator( evaluator );
							filter.setSearch( search );
							filter.setInputFormat(training);
		// Apply the attribute selection filter to training and test sets to gain the filtered versions of them.
		Instances filteredTraining = Filter.useFilter( training, filter );
		Instances filteredTesting = Filter.useFilter( testing, filter );
		
		int numAttrNoFilter = training.numAttributes();
		int numAttrFiltered = filteredTraining.numAttributes();	
		System.out.println("No filter attr: "+ numAttrNoFilter);
		System.out.println("Filtered attr: "+ numAttrFiltered);

		List<Instances> datasets =  new ArrayList<>();
		datasets.add( filteredTraining );
		datasets.add( filteredTesting );
		return datasets;

	}




}
