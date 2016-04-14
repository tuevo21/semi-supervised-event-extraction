/*a java program which takes input as a list of Clause and the kBP dataset, and return features in binary format.
*/

package classes;

import java.util.*;
import java.io.*;
import classes.Clause;
import classes.DataItem;

public class CreateBinaryFeatures{

	public static String[] VerbType = {"elected", "won", "receive", "awarded", "hired", "appointed", "fired",
						"acquired", "bought", "marry", "wed", "divorce", "defeat", "beat", 
						"meet", "attack", "launch", "introduce", "release", "devastate", "destroy", "affected",
						"murder", "killed", "perform", "sue", "file", "bomb", "endorse", "shot"};
	public static String[] EntityType = null;

	public static String[] PrepType = {"of","in","to","for","with","on","at","from","by","about","as","into","like","through","after","over",	"between","out","against","during","without","before","under","around","among", "according", "off", "under", "between", "before", "around", "into", "despite", "following", "via", "after", "above", "up", "like", "as", "amid", "near"};
	/////MAIN METHOD

	//function: Given a list of clauses and kbp, create 2-d array of features

	public static int[][] CreateBinaryFeaturesFromList(List<Clause> res, List<DataItem> kbp) {

		String entitiesfile = "../data/NELL.08m.734.categories.csv";
		List<String[]> categories = new ArrayList<String[]>();
		try {
			Scanner sc = new Scanner(new File(entitiesfile));
			List<String> types = new ArrayList<String>();
			while (sc.hasNextLine()) {
					String[] content = sc.nextLine().split("\t");
					categories.add(content);
				}
			}
		catch (Exception e) {
			System.out.println(e);
			}

		List<int[]> binary = new ArrayList<int[]>();
			//after storing kbp and clauses in memory, create a binary vector
		for (int i = 0; i < res.size(); i++) {
		int[] semantic_feature, syntactic_feature, prep_feature;
		if (((semantic_feature = classes.CreateBinaryFeatures.convertToBinarySemantic(res.get(i), kbp, categories)) != null )
	//				&& (prep_feature = classes.CreateBinaryFeatures.convertToBinaryPrep(clauses.get(i))) != null)
			&& ((syntactic_feature = classes.CreateBinaryFeatures.convertToBinarySyntactic(res.get(i))) != null)) {
			binary.add(classes.CreateBinaryFeatures.joint_two(semantic_feature, syntactic_feature));
			}
		}

		int[][] binary_X = classes.Run.copyListToArray(binary);

		return binary_X;
	}

	public static List<int[]> CreateBinaryFeatures(String mentionfile, List<DataItem> kbp) {

		List<int[]> binary = new ArrayList<int[]>();

		//first query entity file to get array of entity types
		String entitiesfile = "../data/NELL.08m.734.categories.csv";
		List<String[]> categories = new ArrayList<String[]>();
		try {
			Scanner sc = new Scanner(new File(entitiesfile));
			List<String> types = new ArrayList<String>();
			while (sc.hasNextLine()) {
					String[] content = sc.nextLine().split("\t");
					categories.add(content);
				}
			EntityType = types.toArray(new String[types.size()]);
			}
		catch (Exception e) {
			System.out.println(e);
			}

		List<Clause> clauses = new ArrayList<Clause>();
		try {
			clauses = generateClause(mentionfile);	
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		//after storing kbp and clauses in memory, create a binary vector
		for (int i = 0; i < clauses.size(); i++) {
			int[] semantic_feature, syntactic_feature, prep_feature;
	
			if (((semantic_feature = convertToBinarySemantic(clauses.get(i), kbp, categories)) != null )
//				&& (prep_feature = convertToBinaryPrep(clauses.get(i))) != null)
				&& ((syntactic_feature = convertToBinarySyntactic(clauses.get(i))) != null)) {
				binary.add(joint_two(semantic_feature, syntactic_feature));
			}
		}

		//return the result
		return binary;
	}

	//GET FEATURES OF TYPE OF PREPOSITIONS (CONTEXTUAL FEATURES)

	public static int[] convertToBinaryPrep(Clause c) {
		int[] res = new int[PrepType.length];
		String prep = c.getPrep();
		for (int i = 0; i < PrepType.length; i++) {
			if (prep.equalsIgnoreCase(PrepType[i])){
				res[i] = 1;
			}
			else res[i] = 0;
		}
		return res;
	}

	//GET SYNTACTIC FEATURES

	public static boolean isCity(String a) {
		Scanner sc = new Scanner("../data/cities.csv");
		while (sc.hasNextLine()) {
			String[] lines = sc.nextLine().split(",");
			if (lines[lines.length - 1].toLowerCase().contains(a.toLowerCase())) return true;
		}
		return false;

	}

	public static int[] convertToBinarySyntactic(Clause c) {
		int[] res = new int[6];
		String entity = c.getEntity();
		String prep = c.getPrep();
		if (entity.contains("%")) {res[0] = 1;} else {res[0] = 0; }
		if (entity.contains("$")) {res[1] = 1;} else {res[1] = 0; }
		if (entity.matches(".*\\d+.*")) {res[2] = 1;} else {res[2] = 0;}
		if (prep.equalsIgnoreCase("according")) {res[3] = 1;} else {res[3] = 0; }
		if (prep.equalsIgnoreCase("from")) {res[4] = 1;} else {res[4] = 0; }
		if (isCity(entity)) {res[5] = 1; } else {res[5] = 0; }
		return res;
	}

	//GET SEMANTICALLY ANNOTATED FEATURES FROM NELL DATASET

	//A method to get the MostPrevalentType of a data item d
	public static String MostPrevalentType(DataItem d) { //finish this 
		if (d.getContent().get(1) != null) return d.getContent().get(1);
		return null;
	}

	//A method to perform binary search of an item on the kbp list
	//Return the DataItem if found and null if not
	public static DataItem binary_search(String item, List<DataItem> full, int min, int max) {		
		List<String> NAlist = new ArrayList<String>();
		NAlist.add("NA");
		NAlist.add("NA");
		DataItem NA = new DataItem(NAlist);

		double temp = 0.5 * (min + max);
		int mid = (int) temp;

		if (min > max) return NA;

		else if (minimum_distance(item, full.get(min).getContent().get(0)) < 4) return full.get(min);
		else if (minimum_distance(item, full.get(max).getContent().get(0)) < 4) return full.get(max);

		else if (item.compareToIgnoreCase(full.get(mid).getContent().get(0)) > 0) return binary_search(item, full, mid + 1, max);
		else if (item.compareToIgnoreCase(full.get(mid).getContent().get(0)) < 0) return binary_search(item, full, min, mid - 1);
		else return NA;
	}

	public static String getMetaType(String name, List<String[]> categories) {
		String result = null;		
		for (int i = 0; i < categories.size(); i++) {
			String[] temp = categories.get(i);
			if (temp[0].equalsIgnoreCase(name)) {
				String full = null;
				for (int j = 0; j < temp.length; j++) {
					full += temp[j] + " ";
				}
				
				if (full.contains("date")) return "date";
				else if (full.contains("location")) return "location"; 
				else if (full.contains("organization")) return "organization";
				else if (full.contains("person")) return "person";
				else if (full.contains("company")) return "company";
				else if (full.contains("event")) return "event";
			}
		}
		return "";
	}

	//A method to convert a Clause into the sparse Binary vector
	public static int[] convertToBinarySemantic(Clause clause, List<DataItem> kbp, List<String[]> categories) {
		//convert mentions to binary features of event type, preposition, NER type of related entity
		DataItem matchRel = binary_search(clause.getEntity(), kbp, 0, kbp.size() - 1);
		if (matchRel == null) return null;
		else {
			List<String> dataItem = matchRel.getContent();
			List<String> allTypes = new ArrayList<String>();
			for (int i = 1; i < dataItem.size(); i++) {
				String[] parseGroupWord = dataItem.get(i).split(" ");
				allTypes.add(parseGroupWord[0]);
			}

			String[] shortenedEntityType = {"date", "location", "person", "organization", "company", "event"};
 
			int[] index_rel = new int[allTypes.size()];

			for (int i = 0; i < index_rel.length; i++) {
				index_rel[i] = getIndex(shortenedEntityType, getMetaType(allTypes.get(i), categories));
			}
			
			String verb = clause.getVerb();
			String prep = clause.getPrep();
			int[] final_result = createBinaryArray(index_rel, 
							getIndex(PrepType, prep),
							(shortenedEntityType.length), //the last on is the missing value
							(PrepType.length)); 
			return final_result;
		}
	}

///////////////////////
///SUPPORT METHODS/////
///////////////////////
	//support method for convert to Binary: get index of a string from an array of string (if the string is contained in the array)


	public static int[] joint_two(int[] a1, int[] a2) {
		int[] res = new int[a1.length + a2.length];
		System.arraycopy(a1, 0, res, 0, a1.length);
		System.arraycopy(a2, 0, res, a1.length, a2.length);
		return res;		
	}

	public static int[] joint_three(int[] a1, int[] a2, int[] a3) {
		int[] res = new int[a1.length + a2.length + a3.length];
		System.arraycopy(a1, 0, res, 0, a1.length);
		System.arraycopy(a2, 0, res, a1.length, a2.length);
		System.arraycopy(a3, 0, res, a1.length + a2.length, a3.length);
		return res;		
	}

	public static int getIndex(String[] fullString, String individual) {
		int index = Arrays.asList(fullString).indexOf(individual);
		if (index == -1 || individual == null) return fullString.length;
		return index;
	}

	//A method to create a binary array using information from index
	//order of the sparse vector: rel, verb, prep
	public static int[] createBinaryArray(int[] index_rel, int index_prep, int size_rel, int size_prep) {
		int m2 = index_prep + size_rel;
		int[] result = new int[size_rel + size_prep];	
		for (int i = 0; i < result.length; i++) {
			if (equality(i, index_rel) || i == m2) result[i] = 1; 
			else result[i] = 0;
		}
		return result;
	}

	public static int[] createBinaryArray(int length, int index) {
		int[] result = new int[length];
		for (int i = 0; i < length; i++) {
			if (i == index) result[i] = 1;
			else result[i] = 0;
		}
		return result;
	}

	public static boolean equality(int i, int[] index_rel) {
		for (int j : index_rel) {
			if (j == i) return true;
		}
		return false;
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

	public static int[] concatArray(int[] array1, int[] array2) {
		int[] array1and2 = new int[array1.length + array2.length];
		System.arraycopy(array1, 0, array1and2, 0, array1.length);
		System.arraycopy(array2, 0, array1and2, array1.length, array2.length);
		return array1and2;
	}

	public static List<Clause> generateClause(String mentionfile) {
	//store mention file in clause data structure so that each example has one related entity			
		List<Clause> clauses = new ArrayList<Clause>();
		try {
			Scanner sc = new Scanner(new File(mentionfile));
			while (sc.hasNextLine()) {
				String[] content = sc.nextLine().split("\t");
				Clause c = new Clause();
				c.addActor(content[0]); c.addVerb(content[1]); c.addActee(content[2]); 
				c.addPrep(content[3]); c.addEntity(content[4]);
				clauses.add(c);
			}
		}
		catch (Exception e) {System.out.println(e);}
		return clauses;
	}


}
