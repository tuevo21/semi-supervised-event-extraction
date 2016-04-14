package classes;

import java.util.*;

public class Clause {


    public static String[] PrepType = {"of","in","to","for","with","on","at","from","by","about","as","into","like","through","after","over",
					"between","out","against","during","without","before","under","around","among"};

    public String actor, actee, verb, prep, entity;

    public int index;

    
    public Clause() {

    }

    public void addActor(String a) {
	actor = a;
	}

    public void addActee(String a) {
	actee = a;	
	}

    public void addVerb(String a) {
	verb = a;	
	}

    public void addPrep(String a) {
	prep = a;
	}

    public void addEntity(String a) {
	entity = a;
	}

    public void addIndex(int a) {
	index = a;
	}

    public String getVerb() {
	return verb;
	}

    public String getEntity() {
	return entity;
	}

    public String getActor() {
	return actor;
	}

    public String getActee() {
	return actee;
	}


    public String getPrep() {
	return prep;
	}

    public int getIndex() {
	return index;
	}

    public static boolean equality(Clause c1, Clause c2) {
	if ( (c1.getVerb().equals(c2.getVerb())) && (c1.getPrep().equals(c2.getPrep())) ) {
		if ( (c1.getActor().equals(c2.getActor())) && (c1.getActee().equals(c2.getActee())) ) {
			if (c1.getEntity().equals(c2.getEntity())) {
				return true;
			}	
		}
	}
	return false;
        }
}
