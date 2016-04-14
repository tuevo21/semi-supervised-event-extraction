//java -cp stanford-corenlp-3.5.2-models.jar:stanford-corenlp-3.5.2.jar:jsoup-1.8.2.jar:. classes/Run /home/ndapa/data/newsstream2014/2014
//Input: directory of corpus, (list of interest verbs), interested "major" argument associated with verbs, a small number of seeded data of "major" arguments

//Need to work on: expanding syntactic features, semantic features
 
package classes;

import classes.*;
import java.io.*;
import java.util.*;
import java.math.*;
import java.lang.*;

import edu.stanford.nlp.io.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.*;
import edu.stanford.nlp.parser.*;
import edu.stanford.nlp.semgraph.*;
import edu.stanford.nlp.international.*;
import edu.stanford.nlp.dcoref.*;

import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.international.*;

import java.util.Collections;

import org.jsoup.*;


public class Run{

	public static String COMMA = ",";
	public static String TAB = "\t";
	public static String NEWLINE = "\n";

	public static void main(String[] args) throws IOException{
		
		String originfile = "../data/RawExtractionResult.tsv";
		String filter_medium = "../data/Medium.tsv";
		String fulltrain = "../data/filteredResult.tsv";
		String labeled = "../data/seededLabeled.tsv"; //already exists
		String final_output = "../data/finalOutput.tsv";

		//EXTRACTION STEP//

		System.out.println("Starting extraction ... ");

		List<String> fileList = new ArrayList<String>();
		File file = new File(args[0]);
		fileList.addAll(classes.Extractor.displayDirectoryContents(file));
		
		List<String> mentionList = new ArrayList<String>();
		List<Integer> docList = new ArrayList<Integer>();

		FileWriter fw = null;
		FileWriter fwDoc = null;

		try {
		fw = new FileWriter(originfile);
		
			for (int docID = 0; docID < fileList.size(); docID++) {
				
				System.out.println(fileList.get(docID));
				String content = classes.Extractor.readFile(fileList.get(docID));
				List<String> mentions = classes.Extractor.ExtractEvent(classes.Extractor.html2text(content));
				if (mentions.size() > 0)	{
					docList.add(docID);
					for (int j = 0; j < mentions.size(); j++) 
						{fw.append(mentions.get(j)); fw.append("\t");fw.append(Integer.toString(docID)); fw.append("\n");}

				}	
				
			}
		}
		catch (IOException e) {e.printStackTrace();}
		finally{
	        try {
	            fw.flush();
	            fw.close();
	            } 	   
	        catch (IOException e) {
	            e.printStackTrace();
	            }
	         }

		////FILTER STEP////
		System.out.println("Starting filter ... ");
		try {
			classes.Filter.generate_clauses(originfile, filter_medium);
			classes.Filter.applyFilter(filter_medium, fulltrain);
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		////ROLE LABELING STEP ///
		System.out.println("Starting role labeling ... ");

		List<DataItem> kbp = queryKBP();

		List<int[]> binary_fulltrain = classes.CreateBinaryFeatures.CreateBinaryFeatures(fulltrain, kbp);		
		List<int[]> binary_labeled = classes.CreateBinaryFeatures.CreateBinaryFeatures(labeled, kbp);		

		int[][] X = copyListToArray(binary_fulltrain);
		int[][] small_binary_labeled_set = copyListToArray(binary_labeled);

		int NumClasses = 15;

		int[] Y = new int[NumClasses];
		for (int i = 0; i < Y.length; i++) {
			Y[i] = i; //array of 1..k
		} 		
		EM obj = new classes.EM(X, Y);
		
	
		String[] labeled_argument = match_with_labeled(obj, labeled, small_binary_labeled_set, NumClasses);
		writeResult(obj, labeled_argument, fulltrain, labeled, final_output, NumClasses);		

	}

////////////////////////
///SUPPORT METHOD///////
////////////////////////
		
//read from labeled, get proper link between labeled and argument type
//return a string of num classes length, with the name of labels in the guessed indices and null in all other

	public static String[] match_with_labeled(EM obj, String labeled, int[][] small_binary_labeled_set, int NumClasses) {
		
		//retrieve an array of label, store in labels
		List<String> labels_list = new ArrayList<String>();
		try {
			Scanner sc = new Scanner(new File(labeled));
			while (sc.hasNextLine()) {
				String[] this_line = sc.nextLine().split(TAB);
				labels_list.add(this_line[this_line.length - 1]);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		String[] labels = labels_list.toArray(new String[labels_list.size()]);

		//create a set holding all types of label
		Set<String> all_types = new HashSet<String>();
		for (String i : labels) {
			all_types.add(i);
		}

		//retrieve an array of output index, store in output_index
		int n = small_binary_labeled_set.length;
		int d = small_binary_labeled_set[0].length;
		int k = NumClasses;

		double[][] Qxy = obj.getQxy();
		double[] Qy = obj.getQy();
		double[][] delta = new double[n][k];

		EM.E_step(Qxy, Qy, delta, small_binary_labeled_set);
		int[] output_index = new int[n];
		for (int i = 0; i < n; i++) {
			output_index[i] = EM.maxIndex(delta[i]);
		}

		String[] labeled_argument = new String[NumClasses];

		//for each type, retrieve all matching output_indicices and vote for most common index
		Iterator<String> it = all_types.iterator();
		while (it.hasNext()) {
			List<Integer> allIndiciesForThisType = new ArrayList<Integer>();
			String label = it.next();
			for (int i = 0; i < n; i++) {
				if (labels[i].equals(label)) {
					allIndiciesForThisType.add(output_index[i]);
				}
			}
			//copy from list to array
			int[] allIndicies = new int[allIndiciesForThisType.size()];
			for (int i = 0; i < allIndiciesForThisType.size(); i++) {
				allIndicies[i] = allIndiciesForThisType.get(i);
			}
			// end of copy

			int most_common_index = getPopularElement(allIndicies);
			if (labeled_argument[most_common_index] == null) {
				labeled_argument[most_common_index] = label; //write the result in labeled_argument's appropriate array index
			}		
		}

		return labeled_argument;
		
	}

	public static void writeResult(EM obj, String[] labeled_argument, String fulltrain, String labeled, String final_output, int NumClasses) { 

		//write result to final output after EM and matching 

		int[] result = obj.getResult();
		int prev = -1;
		FileWriter fw = null;
		try {
			fw = new FileWriter(final_output);
			int count = 0;
			try {
				Scanner sc = new Scanner(new File(fulltrain));

				while (sc.hasNextLine()) {
					String this_line_in_full = sc.nextLine();
					String[] this_line = this_line_in_full.split("\t");
					if (labeled_argument[result[count]] != null) { //only add if there is a label at the resulted index, or it is in the provided classes
						if (prev == Integer.parseInt(this_line[this_line.length - 1])) { //if it and previous example refer to similar event mention, only retrieve prep and entity				
							fw.append(labeled_argument[result[count]]);
							fw.append(TAB);
							fw.append(this_line[4]);
							fw.append(TAB);

						}
						else {//write to a new example
							fw.append(NEWLINE);
							fw.append(this_line[0]); fw.append(TAB);
							fw.append(this_line[1]); fw.append(TAB);
							fw.append(this_line[2]); fw.append(TAB);
							
							fw.append(labeled_argument[result[count]]);
							fw.append(TAB);
							fw.append(this_line[4]); fw.append(TAB);
						}
					}
					prev = Integer.parseInt(this_line[this_line.length - 1]);
					count++;
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally{
		         try {fw.flush(); fw.close();}     
          		 catch (IOException e) { System.out.println("Error while closing file writer");}
      		}
	
	} 

	public static void writeResultToFile(int[] result, String fulltrain, int maxIndex, String name_file) {
		FileWriter fw = null;
		try {
			fw = new FileWriter("../data/" + name_file);
			for (int i = 0; i < maxIndex + 1; i++) {
				try {
					Scanner sc = new Scanner(new File(fulltrain));
					int count = 0;
					while (sc.hasNextLine()) {	
						String this_line = sc.nextLine();					
						if (result[count] == i) { 
							fw.append(this_line);
							fw.append("\t");
							fw.append(String.valueOf(i)); 
							fw.append(NEWLINE);
							} //get class type in String, minus one because list index starts with 0
						count++;
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				fw.append(NEWLINE);
				fw.append("*************");
				fw.append(NEWLINE);
			}

		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally{
		         try {fw.flush(); fw.close();}     
          		 catch (IOException e) { System.out.println("Error while closing file writer");}
      		}	
	}

	public static List<DataItem> queryKBP() {
		
		//store data from kbp and clauses
		String kbp2013 = "../data/NELL.ClueWeb09.v1.nps.csv";
		//String mentionfile = "data/mentions.txt";
	
		List<DataItem> kbp = new ArrayList<DataItem>();

		//store data from kbp
		try {
			Scanner sc = new Scanner(new File(kbp2013));
			while (sc.hasNextLine() ) {
				String[] features = sc.nextLine().split("\t");
				for (int j = 0; j < features.length; j++) {
					features[j] = features[j].toLowerCase();
				}
				ArrayList<String> item = new ArrayList<String>(Arrays.asList(features));
				kbp.add(new DataItem(item));
			
			}
			Collections.sort(kbp);
		}
		catch (Exception e) {e.printStackTrace();}
		return kbp;
	}


	public static double[] getMaxDelta(double[][] delta) {
		double[] res = new double[delta.length];		
		int c = 0;
		for (double[] d : delta) {
			res[c] = getMaxArray(d);
			c++;
		}
		return res;
	}

	public static double getMaxArray(double[] d) {
		double max = d[0];
		for (double s : d) {
			if (s > max) max = s;
		}
		return max;
	}

	public static void writeConfidentBeliefToFile(int[] result, String fulltrain, double[] max, int maxIndex) {
		double threshold = 0.9;
		FileWriter fw = null;
		try {
			fw = new FileWriter("../data/conf.tsv");
			for (int i = 0; i < maxIndex; i++) {
				try {
					Scanner sc = new Scanner(new File(fulltrain));
					int count = 0;
					while (sc.hasNextLine()) {	
						String this_line = sc.nextLine();					
						if (result[count] == i && max[count] > threshold) { 
							fw.append(this_line);
							fw.append("\t");
							fw.append(String.valueOf(i)); 
							fw.append("\t");
							fw.append(String.format("%.2f", max[count]));
							fw.append(NEWLINE);
							} //get class type in String, minus one because list index starts with 0
						count++;
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				fw.append(NEWLINE);
				fw.append("*************");
				fw.append(NEWLINE);
			}

		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally{
		         try {fw.flush(); fw.close();}     
          		 catch (IOException e) { System.out.println("Error while closing file writer");}
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
	public static int getPopularElement(int[] a)	{
	  int count = 1, tempCount;
	  int popular = a[0];
	  int temp = 0;
	  for (int i = 0; i < (a.length - 1); i++) {
	    	temp = a[i];
	    	tempCount = 0;
	    	for (int j = 1; j < a.length; j++) {
	      		if (temp == a[j])
				tempCount++;
	    	}
	    	if (tempCount > count) {
	      		popular = temp;
	      		count = tempCount;
	    	}
	  }

	  return popular;
	}
}
