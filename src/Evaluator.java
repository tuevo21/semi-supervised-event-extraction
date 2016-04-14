/*Evaluation:

separator function: 
input: labeled
output: separate labeled files to train + validate, using a random mechanism, make sure they have all the classes
 
evaluator function:
input: train labeled, validation labeled, hypothesis Qy and Qxy in output
use train labeled + hypothesis to match class name
use validation labeled + hypothesis to output class name according to EM, compare it with actual output and provide results
*/
package classes;

import java.io.*;
import java.util.*;

public class Evaluator {

	public static final String TAB = "\t";
	public static final String COMMA = ",";
	public static final String NEWLINE = "\n";

	public static String labeled_dir = "../data/labeled.tsv";
	public static String output_dir = "";
	public static String train_labeled_dir = "../data/train_labeled.tsv";
	public static String validation_labeled_dir = "../data/validation_labeled.tsv";

	public static double fullEvaluation(List<DataItem> kbp, double[][] Qxy, double[] Qy) {
		separate();
		double percent = evaluate(kbp, Qxy, Qy);
		return percent;
	}

	public static double evaluate(List<DataItem> kbp, double[][] Qxy, double[] Qy) {

		//asume having both Qxy, Qy and the sparse vector List<int[]> of train and validation		
		List<int[]> train_list = classes.CreateBinaryFeatures.CreateBinaryFeatures(train_labeled_dir, kbp);
		List<int[]> validation_list = classes.CreateBinaryFeatures.CreateBinaryFeatures(validation_labeled_dir, kbp);
		int[][] train = copyListToArray(train_list);
		int[][] validation = copyListToArray(validation_list);
		double[][] delta_train = new double[train.length][Qy.length];
		double[][] delta_validation = new double[validation.length][Qy.length];

		//one E step to calculate probability of train + validation
		classes.EM.E_step(Qxy, Qy, delta_train, train);
		classes.EM.E_step(Qxy, Qy, delta_validation, validation);
		int[] train_result = getMaxResultOfArray(delta_train);
		int[] validation_result = getMaxResultOfArray(delta_validation);

		//get the objs for the example's String type (for example, EventDate) for train + validation
		List<String> train_classes = new ArrayList<String>();
		List<String> validation_actual_classes = new ArrayList<String>();
		try {
			train_classes = readOutputClasses(train_labeled_dir);
			validation_actual_classes = readOutputClasses(validation_labeled_dir);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		//get the map from the index of class to name of class, using train + name_of_classes obj
		String[] ClassNameIndex = new String[Qy.length];
		for (String s : ClassNameIndex) {
			s = "";
		}
		for (int i = 0; i < train_classes.size(); i++) {
			ClassNameIndex[train_result[i]] = train_classes.get(i);
		}

		//output them on validation
		List<String> validation_output_classes = new ArrayList<String>();
		for (int i = 0; i < validation_result.length; i++) {
			String s = ClassNameIndex[validation_result[i]];
			validation_output_classes.add(s);
		}
		//create a function to calculate score: compare between validation_actual_classes and validation_output_classes
		double percent = outputStatistic(train_classes, validation_output_classes, validation_actual_classes);
		return percent;
	}


	public static void separate() {

		//store data + types
		List<String> labeled_classes = new ArrayList<String>();
		List<String[]> labeled = new ArrayList<String[]>();
		try {
			Scanner sc = new Scanner(new File(labeled_dir));
			while (sc.hasNextLine()) {
				String[] line = sc.nextLine().split(TAB);
				labeled.add(line);
			}
			sc.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		Set<String> labeled_classes_set = new HashSet<String>();
		for (int i = 0; i < labeled.size(); i++) {
			String[] s = labeled.get(i);
			labeled_classes_set.add(s[s.length - 1]); //add types
		}

		labeled_classes.addAll(labeled_classes_set);

		//write to 2 files		
		//write to train_labeled_dir (only labeled_classes_size or k examples for each)
		FileWriter fw1 = null;
		FileWriter fw2 = null;

		try {
			fw1 = new FileWriter(train_labeled_dir);
			for (int i = 0; i < labeled.size() && labeled_classes.size() > 0; i++) { 
				String[] this_example = labeled.get(i);
				for (int j = 0; j < labeled_classes.size(); j++) {
					if (this_example[this_example.length - 1].equals(labeled_classes.get(j))) {
						fw1.append(concatenateWithTab(this_example)); //append the example
						fw1.append(NEWLINE);
						labeled_classes.remove(j); //remove this labeled class from list
						labeled.remove(i); //remove this example from the big labeled data struct
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}	
		finally{
		         try {fw1.flush(); fw1.close();}     
          		 catch (IOException e) { System.out.println("Error while closing file writer");}
      		}	

		//write to validation_labeled_dir (the rest);	
		
		try {
			fw2 = new FileWriter(validation_labeled_dir);
			for (int i = 0; i < labeled.size(); i++) { 
				fw2.append(concatenateWithTab(labeled.get(i))); //append the example
				fw2.append(NEWLINE);					
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}	
		finally{
		         try {fw2.flush(); fw2.close();}     
          		 catch (IOException e) { System.out.println("Error while closing file writer");}
      		}	
	}


////////////////////////
///SUPPORT METHOD///////
////////////////////////

	public static double outputStatistic(List<String> train_classes, List<String> validation_output_classes, List<String> validation_actual_classes) {
		
		int total = validation_actual_classes.size();
		int match = 0;

		int[] sum_each_class = new int[train_classes.size()];
		int[] correct_each_class = new int[train_classes.size()];
		for (int i : sum_each_class) {i = 0; }
		for (int i : correct_each_class) {i = 0; }

		for (int i = 0; i < validation_actual_classes.size(); i++) {
			boolean matched = false;
			if (validation_actual_classes.get(i).equals(validation_output_classes.get(i))) {
				match++; matched = true;
			}
			for (int j = 0; j < train_classes.size(); j++) {
				if (validation_actual_classes.get(i).equals(train_classes.get(j)))  { 
					sum_each_class[j]++;
					if (matched) { correct_each_class[j]++; }				
				}
			}
		}
		double percent = match/ (double) total;
		System.out.println("Result for total: " + percent);
		for (int i = 0; i < train_classes.size(); i++) {
			System.out.println(train_classes.get(i) + ": " + correct_each_class[i]/ (double) sum_each_class[i]);
		}
		return percent;
	}


	public static List<String> readOutputClasses(String dir) {
		List<String> output = new ArrayList<String>();
		try {
			Scanner sc = new Scanner(new File(dir));
			while (sc.hasNextLine()) {
				String[] items = sc.nextLine().split(TAB);
				output.add(items[items.length - 1]);
			}
			sc.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return output;
	}

	public static int[] getMaxResultOfArray(double[][] array) {
		int[] result = new int[array.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = classes.EM.maxIndex(array[i]);
		}
		return result;
	}

	public static String concatenateWithTab(String[] full) {
		String result = "";
		for (String s : full) {
			result += s + TAB;
		}	
		return result;
	}

	public static String readFile(String input) throws IOException {
		File file = new File(input);
		Scanner sc = new Scanner(file);
		String lineSeparator = System.getProperty("line.separator");
		StringBuilder fileContent = new StringBuilder((int) file.length());
		try {
			while(sc.hasNextLine()) {        
			    fileContent.append(sc.nextLine() + lineSeparator);
			}
			return fileContent.toString();
	        } 
		finally {
			sc.close();
		}
	}

	public static int[][] copyListToArray(List<int[]> list) {
		int n = list.size();
		int d = list.get(0).length;
		int[][] array = new int[n][d];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < d; j++) {
				array[i][j] = list.get(i)[j];				
			}
		} 
		return array;
	}
}

