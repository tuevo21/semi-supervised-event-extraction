package classes;

import java.util.*;
import java.io.*;
import java.lang.*;
import classes.Clause;
import classes.DataItem;

public class Filter{

	public static String COMMA = ",";
	public static String TAB = "\t";
	public static String NEWLINE = "\n";


	public static void main(String[] args) {

		String originfile = "../data/RawExtractionResult.tsv";
		String event = "../data/Medium.tsv";
		String filtered = "../data/filteredResult.tsv";

		try {
			generate_clauses(originfile, event);
			applyFilter(event, filtered);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static List<Clause> generate_clauses(List<String> mentions) {

		//write into Clause data structure
		List<Clause> clauses = new ArrayList<classes.Clause>();

		for (int i = 0; i < mentions.size(); i++) {
			String[] content = mentions.get(i).split("\t");
				
			for (int j = 3; j < content.length; j++) { //the last item in the tab is a number
				classes.Clause c = new Clause();
				c.addActor(content[0]);
				c.addVerb(content[1]);
				c.addActee(content[2]);				

				String[] cont = content[j].split(" ");
				if (cont.length > 1) { //it should have two components: preposition and entity
					c.addPrep(cont[0]);
					//System.out.println("Prep: " + cont[0]);
					String entity = "";
					for (int k = 1; k < cont.length; k++) {
						entity += cont[k] + " ";	
					}					
					c.addEntity(entity);	
					c.addIndex(j);	
					//if (c.getVerb().equals("acquired") || c.getVerb().equals("bought")) 
				clauses.add(c);	
				}
			}
		}
		return clauses;
	}

	public static void generate_clauses(String mentionfile, String output) {
		List<Clause> clauses = new ArrayList<Clause>();
		//store data of mentions
		try {
			Scanner sc = new Scanner(new File(mentionfile));
			int count = 0;
			while (sc.hasNextLine()) {
				String[] content = sc.nextLine().split("\t");
				
				for (int i = 3; i < content.length; i++) { //the last item in the tab is a number
					Clause c = new Clause();
					c.addActor(content[0]);
					c.addVerb(content[1]);
					c.addActee(content[2]);				

					String[] cont = content[i].split(" ");
					if (cont.length > 1) { //it should have two components: preposition and entity
						c.addPrep(cont[0]);
						//System.out.println("Prep: " + cont[0]);
						String entity = "";
						for (int j = 1; j < cont.length; j++) {
							entity += cont[j] + " ";	
						}					
						c.addEntity(entity);	
						c.addIndex(count);	
						//if (c.getVerb().equals("acquired") || c.getVerb().equals("bought")) 
						clauses.add(c);	
					}
				}
				count++;

			}
		}

		catch (Exception e) {System.out.println(e);}

		//write to a labeled training data file
		FileWriter fw = null;
		try {
			fw = new FileWriter(output);
			for (int i = 0; i < clauses.size(); i++) {

				fw.append(clauses.get(i).getActor()); fw.append(TAB);
				fw.append(clauses.get(i).getVerb()); fw.append(TAB);
				fw.append(clauses.get(i).getActee()); fw.append(TAB);
				fw.append(clauses.get(i).getPrep()); fw.append(TAB);
				fw.append(clauses.get(i).getEntity()); fw.append(TAB);
				fw.append(Integer.toString(clauses.get(i).getIndex())); fw.append(TAB);
				fw.append(NEWLINE);

			}
		}
		catch (Exception e) {
			System.out.println(e);
		}
		finally{
		         try {fw.flush(); fw.close();}     
          		 catch (IOException e) { System.out.println("Error while closing file writer");}
      		}	

		//write to an unlabeled training data file

	}

	public static List<Clause> filterRepetition(List<Clause> l) { //very inefficient implementation
		List<Clause> res = new ArrayList<Clause>();		
		for (int i = 0; i < l.size(); i++) {
			boolean not_matched = true;
			for (int j = 0; j < res.size(); j++) {
				if (classes.Clause.equality(l.get(i), res.get(j))) not_matched = false;
			}
			if (not_matched) {
				res.add(l.get(i)); 
			}
		}
		return res;
	}

	public static List<Clause> filterSize(List<Clause> l) {
		List<Clause> res = new ArrayList<Clause>();
		for (int i = 0; i < l.size(); i++) {
			String[] parse1 = l.get(i).getActor().split(" ");
			String[] parse2 = l.get(i).getActee().split(" ");
			String[] parse3 = l.get(i).getEntity().split(" ");
			if ((parse1.length > 5 || parse2.length > 5) || parse3.length > 5) {

			}
			else {
				res.add(l.get(i));
			} 
		}
		return res;
	}

	
	public static List<Clause> filterActorPhrases(List<Clause> l) {
		List<Clause> res = new ArrayList<Clause>();
		for (int i = 0; i < l.size(); i++) {
			String[] parse = l.get(i).getActor().split(" ");
			String newActor = "";			
			for (int j = 0; j < parse.length; j++) {
				String this_string = parse[j];
				if (Character.isUpperCase(this_string.charAt(0))) newActor += this_string + " ";
			}
			if (newActor != "" && newActor.length() > 4) {
	
				Clause c = new Clause();
				c.addActor(newActor);
				c.addVerb(l.get(i).getVerb());
				c.addActee(l.get(i).getActee());
				c.addPrep(l.get(i).getPrep());
				c.addEntity(l.get(i).getEntity());
				c.addIndex(l.get(i).getIndex());
				res.add(c);
			}
		}
		return res;
	}


	public static List<Clause> filterUpperCase(List<Clause> l) {
		List<Clause> res = new ArrayList<Clause>();
		for (int i = 0; i < l.size(); i++) {
			String actor = l.get(i).getActor();
			String actee = l.get(i).getActee();
			boolean hasUppercase = !actor.equals(actor.toLowerCase());
			if (hasUppercase) res.add(l.get(i));
		}
		return res;
	}

	public static List<Clause> filterPronouns(List<Clause> l) {
		String[] pronouns = {"i", "you", "he", "she", "we", "they", "which", "that", "me", "him", "her", "us", "them", "it"};
		List<Clause> res = new ArrayList<Clause>();
		for (int i = 0; i < l.size(); i++) {
			String actee = l.get(i).getActee();
			String actor = l.get(i).getActor();
			String entity = l.get(i).getEntity();
			boolean add = true;
			for (String j : pronouns) {
				if ((minimum_distance(actee, j) < 2 || minimum_distance(actor, j) < 2) || (minimum_distance(entity, j) < 2 || actor.length() < 4)) add = false;
			}
			if (add) res.add(l.get(i));
		}
		return res;
	}

	public static List<Clause> filterPreposition(List<Clause> l) {
		String[] PrepType = {"of","in","to","for","with","on","at","from","by","about","as","into","like","through","after","over",	"between","out","against","during","without","before","under","around","among", "according", "off", "under", "between", "before", "around", "into", "despite", "following", "via", "after", "above", "up", "like", "as", "amid", "near"};
		List<Clause> res = new ArrayList<Clause>();
		for (int i = 0; i < l.size(); i++) {
			String prep = l.get(i).getPrep();
			for (String s : PrepType) {
				if (s.equalsIgnoreCase(prep)) res.add(l.get(i));
			}
		}	
		return res;
	}

	public static boolean isPronoun(String a) {
		String[] pronouns = {"i", "you", "he", "she", "we", "they", "which", "that", "me", "him", "her", "us", "them", "it"};
		for (String i : pronouns) {
			if (minimum_distance(a, i) < 2) {
				return true;
			}
		}
		return false;
	}

	public static List<Clause> applyFilter(List<Clause> clauses) {
		return filterSize(filterActorPhrases(
			filterUpperCase(filterPreposition(
			filterPronouns(filterRepetition(clauses))))));
	}

	public static void applyFilter(String mentionfile, String outputfile) {
		List<Clause> clauses = new ArrayList<Clause>();

		try {
			Scanner sc = new Scanner(new File(mentionfile));
			while (sc.hasNextLine()) {	
				String[] content = sc.nextLine().split("\t");
				Clause c = new Clause();
				c.addActor(content[0]);
				c.addVerb(content[1]);
				c.addActee(content[2]);	
				c.addPrep(content[3]);
				c.addEntity(content[4]);
				c.addIndex(Integer.parseInt(content[5]));
				clauses.add(c);
			}
		}
		catch (Exception e) {
			System.out.println(e);
		}
		List<Clause> res = 
					filterSize(
					filterActorPhrases(
					filterUpperCase(filterPreposition(filterPronouns(filterRepetition(clauses))))
					))
					;
		FileWriter fw = null;

		try {
			fw = new FileWriter(outputfile);
			for (int i = 0; i < res.size(); i++) {

				fw.append(res.get(i).getActor()); fw.append(TAB);
				fw.append(res.get(i).getVerb()); fw.append(TAB);
				fw.append(res.get(i).getActee()); fw.append(TAB);
				fw.append(res.get(i).getPrep()); fw.append(TAB);
				fw.append(res.get(i).getEntity()); fw.append(TAB);
				fw.append(Integer.toString(res.get(i).getIndex())); fw.append(TAB);
				fw.append(NEWLINE);

			}
		}
		catch (Exception e) {
			System.out.println(e);
		}
		finally{
		         try {fw.flush(); fw.close();}     
          		 catch (IOException e) { System.out.println("Error while closing file writer");}
      		}	

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
