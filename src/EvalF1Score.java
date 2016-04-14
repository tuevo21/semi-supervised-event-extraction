import java.util.*;
import java.lang.*;
import java.io.*;

public class EvalF1Score{

	public static String expectedpredicted = "../data/filteredAcquisitionEventLabeled.txt";
	public static String original = "../data/filteredAcquisitionEvent.txt";
	public static String resultEM = "../data/resultAt10.tsv";

	public static void main(String[] args) {

		String[] a = getExpected();
		String[] b = getPredicted(a.length);
		String[] expected_array = initializeNull(a);
		String[] predicted_array = initializeNull(b);
		EvalF1Score(predicted_array, expected_array);
		
	}


	public static String[] getExpected() {

		//create an array of string containing expected
		List<String> expected_list = new ArrayList<String>();
		try {
			Scanner sc = new Scanner(new File(expectedpredicted));
			while (sc.hasNextLine()) {
				String[] parts = sc.nextLine().split("\t");
				if (parts.length == 6) {
					expected_list.add(parts[parts.length - 1]);
				}
				else {
					expected_list.add(null);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		String [] expected = new String[expected_list.size()];
		expected_list.toArray(expected);
		return expected;

	}


	public static String[] getPredicted(int length) {

		//create an array of strings containing predicted
		String[] predicted = new String[length];
		try {
			Scanner sc = new Scanner(new File(resultEM));
			String currentDesignatedClass = null;
			String[] temp;
			boolean signOfStart = false;
			boolean shouldAdd = false;
	
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				temp = line.split("\t");

				if (line.contains("*****")) {
					shouldAdd = false;
				}
				else if (shouldAdd) {
					//start adding 
					try {
						Scanner sc2 = new Scanner(new File(original));
						int index = 0;
						boolean notOut = true;
						while (sc2.hasNextLine() && notOut) {
							String currentLine = sc2.nextLine();
							if (line.contains(currentLine)) {
								predicted[index] = currentDesignatedClass;
								notOut = false;
							}
							index++;
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
				
				else if (equalsDesignatedTypes(line)) {
					shouldAdd = true; 
					currentDesignatedClass = line;
				}

				else {
					shouldAdd = false;
				}
								
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return predicted;

		//after getting the 
	}

	public static void EvalF1Score(String[] predicted, String[] expected) {

		//calculate true positive, false positive, false negative, true negative

		String[] types = {"Source", "Amount", "Date", "PreviousOwner"};
		double[][] f_score = new double[4][4];


		for (int i = 0; i < 4; i++) {
			//calculate true positive, false positive, false negative, true negative
			int true_positive = 0, false_positive = 0, false_negative = 0, true_negative = 0;
			for (int j = 0; j < predicted.length; j++) {
				if (minimum_distance(predicted[j], types[i]) < 2 && minimum_distance(expected[j], types[i]) < 2) true_positive++;
				else if (minimum_distance(predicted[j], types[i]) < 2 && minimum_distance(expected[j], types[i]) >= 2) { 
					false_positive++;
				//	System.out.println(predicted[j] + " " + expected[j] + " " + j);
				}
				else if (minimum_distance(predicted[j], types[i]) >= 2 && minimum_distance(expected[j], types[i]) < 2) false_negative++;
				else if (minimum_distance(expected[j], types[i]) >= 2 && minimum_distance(expected[j], types[i]) >= 2) true_negative++;
			}
			f_score[i][0] = true_positive;
			f_score[i][1] = false_positive;
			f_score[i][2] = false_negative;
			f_score[i][3] = true_negative;
			System.out.println(true_positive + " " + false_positive + " " + false_negative + " " + true_negative + " ");
		}
		//calculate precision, recall for each type
		double[][] precrec = new double[4][2];
		for (int i = 0; i < 4; i++) {
			precrec[i][0] = f_score[i][0]/ (double) (f_score[i][0] + f_score[i][1]); //precision
			precrec[i][1] = f_score[i][0]/ (double) (f_score[i][0] + f_score[i][2]); //recall
			System.out.println("For " + types[i] + " precision = " + precrec[i][0] + " recall = " + precrec[i][1]);
		}

		//calculate precision, recall for all
		int numSource = 0, numAmount = 0, numDate = 0, numPreviousOwner = 0;
		for (int i = 0; i < expected.length; i++) {
			if (expected[i].equals(types[0])) numSource++;
			else if (expected[i].equals(types[1])) numAmount++;
			else if (expected[i].equals(types[2])) numDate++;
			else if (expected[i].equals(types[3])) numPreviousOwner++;			
		}

		int numTotal = numSource + numAmount + numDate + numPreviousOwner;
		double probSource = numSource/ (double) numTotal;
		double probAmount = numAmount/ (double) numTotal;
		double probDate = numDate/ (double) numTotal;
		double probPreviousOwner = numPreviousOwner/ (double) numTotal;
		double totalPrecision = probSource * precrec[0][0] + probAmount * precrec[1][0] + probDate * precrec[2][0] + probPreviousOwner * precrec[3][0];
		double totalRecall = probSource * precrec[0][1] + probAmount * precrec[1][1] + probDate * precrec[2][1] + probPreviousOwner * precrec[3][1];
		System.out.println("In general precision = " + totalPrecision + " recall = " + totalRecall);
	}

////SUPPORT METHODS////////////
	
	public static boolean equalsDesignatedTypes(String a) {
		if (a != null && !a.isEmpty()) {
			if (minimum_distance(a, "Date") < 2) return true;
			else if (minimum_distance(a, "PreviousOwner") < 2) return true;
			else if (minimum_distance(a, "Source") < 2) return true;
			else if (minimum_distance(a, "Amount") < 2) return true;
		}
		return false;
	}

	public static String[] initializeNull(String[] org) {
		String[] res = new String[org.length];
		for (int i = 0; i < org.length; i++) {
			if (org[i] != null) res[i] = org[i];
			else res[i] = "a";
		}
		return res;
	}


	//A support method which calculates the minimum distance between two strings
	public static int minimum_distance(String string1, String string2) {
		int m = string1.length();
		int n = string2.length();

		int[][] table = new int[m+1][n+1];
		for (int i = 0; i < m+1; i++) {
			table[i][0] = i;
		}
		for (int i = 1; i < n+1; i++) {
			table[0][i] = i;
		}

		for (int i = 1; i < m + 1; i++) {
			for (int j = 1; j < n + 1; j++) {
				int temp = 0;
				if (string1.charAt(i-1) == string2.charAt(j-1)) {
					temp = table[i-1][j-1];
				}
				else {
					temp =  table[i-1][j-1] + 2;
				}
				table[i][j] = min(temp, table[i-1][j] + 1, table[i][j-1] + 1);
			}
		}

		return table[m][n];	
	}

	public static int min(int x, int y, int z) { //for calculation of minimum editing distance
		int min = x;
		if (y < min) min = y;
		if (z < min) min = z;
		return min;
	}

}
