//Compile: javac -cp stanford-corenlp-3.5.2-models.jar:stanford-corenlp-3.5.2.jar:jsoup-1.8.2.jar:. Extractor.java
//Run: java -cp stanford-corenlp-3.5.2-models.jar:stanford-corenlp-3.5.2.jar:jsoup-1.8.2.jar:. -Xmx2g ExtractMention

package classes;

import java.io.*;
import java.util.*;
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

public class Extractor {

//retrieve all dependencies from dependencies graph of a word 
//dependencies:
//appos: appositional modifier (Sam, my brother)
//compound
//nn: noun compound modifier (oil price futures -> nn(futures, oil), nn(futures, price))
//nmod: 

//get through a loop to retrieve dependencies

/*public static String[] listOfVerbs = {"elected", "won", "receive", "awarded", "hired", "appointed", "fired",
					"acquired", "bought", "marry", "wed", "divorce", "defeat", "beat", 
					"meet", "attack", "launch", "introduce", "release", "devastate", "destroy", "affected",
					"murder", "killed", "perform", "sue", "file", "bomb", "endorse", "shot"};
*/

public static String[] listOfVerbs = {"bought", "buy", "buys", "acquired", "acquire", "acquires", "purchased", "purchase", "purchases"};

public static void loop(List<IndexedWord> RelList, IndexedWord start, SemanticGraph dependencies, boolean cont) {
	RelList.add(start);
	Set<GrammaticalRelation> s = dependencies.childRelns(start);
	Iterator i = s.iterator();
	while (i.hasNext()) {
		GrammaticalRelation res = (GrammaticalRelation) i.next();
		if (res.toString().equals("compound") || res.toString().equals("nummod") || res.toString().equals("nn") || 
			res.toString().equals("amod") || res.toString().equals("case") || res.toString().equals("appos") || 
			res.toString().equals("det") || res.toString().equals("dep") || res.toString().substring(0,4).equals("nmod")) {
			IndexedWord new_start = dependencies.getChildWithReln(start, res);
			if (!RelList.contains(new_start)) { 
				loop(RelList, new_start, dependencies, true);
			}
		}		
	}
}

public static void loop_entity(List<IndexedWord> RelList, IndexedWord start, SemanticGraph dependencies, boolean cont, List<IndexedWord>[] EntityRelList) {

	RelList.add(start);
	Set<GrammaticalRelation> s = dependencies.childRelns(start);
	Iterator i = s.iterator();
	while (i.hasNext()) {
		GrammaticalRelation res = (GrammaticalRelation) i.next();
		if (res.toString().equals("compound") || res.toString().equals("nummod") || res.toString().equals("nn") || 
			res.toString().equals("amod") || res.toString().equals("case") || res.toString().equals("appos") || 
			res.toString().equals("det") || res.toString().equals("dep")) {

			IndexedWord new_start = dependencies.getChildWithReln(start, res);
			if (!RelList.contains(new_start)) { 
				loop_entity(RelList, new_start, dependencies, true, EntityRelList);
			}
		}
		else if (cont && (res.toString().length() > 4 && res.toString().substring(0, 4).equals("nmod"))) {		
			//continue one more round
			IndexedWord new_start = dependencies.getChildWithReln(start, res);
			//create an additional item
			List<IndexedWord> NewList = new ArrayList<IndexedWord>();
			int org_size = EntityRelList.length;
			List<IndexedWord>[] CopyEntityRelList = (ArrayList<IndexedWord>[]) new ArrayList[org_size + 1];
			System.arraycopy(EntityRelList, 0, CopyEntityRelList, 0, org_size);
			CopyEntityRelList[org_size] = NewList;
			EntityRelList = CopyEntityRelList;
			//
			if (!RelList.contains(new_start)) { 
				loop_entity(NewList, new_start, dependencies, false, EntityRelList);
			}
		}
	}
}

public static List<String> ExtractEvent(String text) { //extract potential event from a sentence

	  List<String> result = new ArrayList<String>();	 
	  if (containsVerb(text)) {
	
		Properties props = new Properties();
		//props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, depparse, dcoref");
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, depparse");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		//Annotation document = new Annotation(SentencesContainingVerb.get(j));
		Annotation document = new Annotation(text);    
		pipeline.annotate(document);

		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	        int SentenceIndex = 0; //keep track of sentence index
	    	for(CoreMap sentence: sentences) {
		      SentenceIndex++;
	    	      boolean triggerVerbMatched = false;
		      CoreLabel TargetedVerbCL = null;
			
		      for (CoreLabel token: sentence.get(TokensAnnotation.class)) { 	
			String pos = token.get(PartOfSpeechAnnotation.class);
			if (pos.charAt(0) == 'V' 
				&& triggerVerbMatching(token.word())
				) { 
				triggerVerbMatched = true;
				TargetedVerbCL = token;
			}
		      }

		      if (triggerVerbMatched) {
		      	SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
			List<Pair<GrammaticalRelation, IndexedWord>> TargetedVerbDependencies = new ArrayList<Pair<GrammaticalRelation, IndexedWord>>();
			IndexedWord actor = null; IndexedWord actee = null; List<IndexedWord> entities = new ArrayList<IndexedWord>(); 
			IndexedWord verb = new IndexedWord(TargetedVerbCL);

			//EXTRACTION OF VERB
			//EXTRACT ALL VERB RELATIONS

			Set<GrammaticalRelation> x = dependencies.childRelns(verb);
			Iterator it = x.iterator();
			System.out.println("verb: " + verb.word());
			while (it.hasNext()) {
				GrammaticalRelation res = (GrammaticalRelation) it.next(); 
				System.out.println(res.toString() + " " + dependencies.getChildWithReln(verb, res).word());

				if (res.toString().equals("nsubj") || res.toString().equals("agent") || res.toString().equals("xsubj")) {
					actor = dependencies.getChildWithReln(verb, res); 
				} 
				else if (res.toString().equals("iobj") || res.toString().equals("dobj") || res.toString().equals("nsubjpass")){
					actee = dependencies.getChildWithReln(verb, res); 
				} 
				else if (res.toString().length() > 3 && res.toString().substring(0,4).equals("nmod")) {
					entities.add(dependencies.getChildWithReln(verb, res));
				}		
			} //end of while loop
			
			//EXTRACTION OF ALL VERTICES CONNECTED TO VERB

			List<IndexedWord> ActorRelList = new ArrayList<IndexedWord>();
			List<IndexedWord> ActeeRelList = new ArrayList<IndexedWord>();
			List<IndexedWord>[] EntityRelList = (ArrayList<IndexedWord>[]) new ArrayList[entities.size()];

			//EXTRACT ALL ACTOR AND ACTEE RELATIONS
			if (actor != null && ((!actor.word().equals("you")) && (!actor.word().equals("You")))) {
				loop_entity(ActorRelList, actor, dependencies, true, EntityRelList);
			}

			if (actee != null) {
				loop_entity(ActeeRelList, actee, dependencies, true, EntityRelList);
			}
			//EXTRACT PREP ASSOCIATED WITH ENTITY 

			if (entities != null && entities.size() > 0) {
				for (int i = 0; i < entities.size(); i++) {
					if (EntityRelList[i] == null) {
						EntityRelList[i] = new ArrayList<IndexedWord>();
					}				
					loop_entity(EntityRelList[i], entities.get(i), dependencies, true, EntityRelList); 
				}
			}
			//END OF EXTRACTION
			//GET SENTENCE INDEX AND REFORMAT TO CAPTURE THE EXACT WORD ORDER

			if ((ActorRelList.size() > 0 && ActeeRelList.size() > 0) && EntityRelList.length > 0) {			
				Collections.sort(ActorRelList);
				Collections.sort(ActeeRelList);

				int ActorMinInd = ActorRelList.get(0).sentIndex();
				int ActorMaxInd = ActorRelList.get(ActorRelList.size() - 1).sentIndex();
				int ActeeMinInd = ActeeRelList.get(0).sentIndex();
				int ActeeMaxInd = ActeeRelList.get(ActeeRelList.size() - 1).sentIndex();
				int[] EntityMinInd = new int[EntityRelList.length];
				int[] EntityMaxInd = new int[EntityRelList.length];

				for (int i = 0; i < EntityRelList.length; i++) {
					Collections.sort(EntityRelList[i]);
					EntityMinInd[i] = EntityRelList[i].get(0).sentIndex();
					EntityMaxInd[i] = EntityRelList[i].get(EntityRelList[i].size() - 1).sentIndex();
				}

				for (CoreLabel token: sentence.get(TokensAnnotation.class)) { 	
					IndexedWord word = new IndexedWord(token);
					if (word.sentIndex() > ActorMinInd && word.sentIndex() < ActorMaxInd ) {
						ActorRelList.add(word);
					}			
					else if (word.sentIndex() > ActeeMinInd && word.sentIndex() < ActeeMaxInd ) {
						ActeeRelList.add(word);
					}	
					else {
						for (int i = 0; i < EntityRelList.length; i++) {
							if (word.sentIndex() > EntityMinInd[i] && word.sentIndex() < EntityMaxInd[i] ) {
								EntityRelList[i].add(word);
							}
						}
					}
				}			
				Collections.sort(ActorRelList);
				Collections.sort(ActeeRelList);
				for (int i = 0; i < EntityRelList.length; i++) {
					Collections.sort(EntityRelList[i]);
				}
			}
			//RETURN LIST OF STRING CONTAINING THE MENTION
		
			String mention = null;
			if ((ActorRelList.size() > 0 && ActeeRelList.size() > 0) && EntityRelList.length > 0) {
				//Add actor
				for (int i = 0; i < ActorRelList.size(); i++) {if (ActorRelList.get(i) != null) mention += ActorRelList.get(i).word() + " ";}
//
				mention += "\t";

				//add verb
				mention += verb.word() + "\t";
	
				//add actee
				for (int i = 0; i < ActeeRelList.size(); i++) {if (ActeeRelList.get(i) != null) mention += ActeeRelList.get(i).word() + " ";}
				mention += "\t";
				
				//add related entities
				for (int i = 0; i < EntityRelList.length; i++) {
					for (int j = 0; j < EntityRelList[i].size(); j++) {
						if (EntityRelList[i].get(j) != null) mention += EntityRelList[i].get(j).word() + " ";
					}
					mention += "\t";
				}

				
			}	

			if (mention != null) {
				result.add(mention.substring(4));
			}
		    }
		} //end of if statement
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

	public static String html2text(String html) {
		    return Jsoup.parse(html).text();	
	}

	public static boolean triggerVerbMatching(String verb) {
/*
		String[] listOfVerbs = {"elected", "won", "receive", "awarded", "hired", "appointed", "fired",
					"acquired", "bought", "marry", "wed", "divorce", "defeat", "beat", 
					"meet", "attack", "launch", "introduce", "release", "devastate", "destroy", "affected",
					"murder", "killed", "perform", "sue", "file", "bomb", "endorse", "shot"};
*/
		for (int i = 0; i < listOfVerbs.length; i++) {
			if (listOfVerbs[i].equals(verb)) return true;
		}
		return false;
	}

	public static boolean containsVerb(String sentence) {
		String[] text = sentence.split(" ");
		/*
		String[] listOfVerbs = {"elected", "won", "receive", "awarded", "hired", "appointed", "fired",
					"acquired", "bought", "marry", "wed", "divorce", "defeat", "beat", 
					"meet", "attack", "launch", "introduce", "release", "devastate", "destroy", "affected",
					"murder", "killed", "perform", "sue", "file", "bomb", "endorse", "shot"};
		*/
		
		for (int i = 0; i < text.length; i++) {
			for (int j = 0; j < listOfVerbs.length; j++) {
				if (text[i].equals(listOfVerbs[j])) {
					//System.out.println(text[i-1] + " " + text[i] + " " + text[i+1]); 
					return true;
				}
			}
		}
		return false;
	}

	public static List<String> getSentenceContainingVerb(String document){

		String[] sentences = document.split(".");
		List<String> result = null;		
		for (int i = 0; i < sentences.length; i++) {
			System.out.println(sentences[i]);
			if (containsVerb(sentences[i])) {
				result.add(sentences[i]);
			}
		}
		return result;

	}

	public static List<String> displayDirectoryContents(File dir) {
		List<String> result = new ArrayList<String>();
		try {
			File[] files = dir.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					result.addAll(displayDirectoryContents(file));
				} else {
					//System.out.println("     file:" + file.getCanonicalPath());
					result.add(file.getCanonicalPath().toString());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}


	public static void main(String[] args) throws IOException{
		System.out.println("Starting extraction ... ");

		List<String> fileList = new ArrayList<String>();
		File file = new File(args[0]);
		fileList.addAll(displayDirectoryContents(file));
		
		List<String> mentionList = new ArrayList<String>();
		List<Integer> docList = new ArrayList<Integer>();

		FileWriter fw = null;
		FileWriter fwDoc = null;

		//WRITE OUT MENTION
		try {
		fw = new FileWriter("../data/RawExtractionResult.tsv");
		
			for (int docID = 0; docID < fileList.size(); docID++) {
				
				System.out.println(fileList.get(docID));
				String content = readFile(fileList.get(docID));
				List<String> mentions = ExtractEvent(html2text(content));
				if (mentions.size() > 0)	{
					docList.add(docID);
					for (int j = 0; j < mentions.size(); j++) 
						{fw.append(mentions.get(j)); fw.append("\t");fw.append(Integer.toString(docID)); fw.append("\n");}

				}	
				
			}
		}//end of try statement
		catch (IOException e) {System.out.println("error of writing file");}
		finally{
	        try {
	            fw.flush();
	            fw.close();
	            } 	   
	        catch (IOException e) {
	            System.out.println("Error while closing file writer");
	            }
	         }
		

/*
		try {
			String content = readFile("testfile/test4.txt");
			ExtractEvent(html2text(content));

		}
		catch (Exception e) {
			System.out.println("error" + e);		
		}
*/
		

	}

}
