package classes;

import java.util.*;

public class DataItem implements Comparable<DataItem> {

    public List<String> content = new ArrayList<String>();
    
    public DataItem(List<String> feature) {
	content = feature;
    }

    public List<String> getContent() {
	return content;
    }

    @Override
    public int compareTo(DataItem other) {
	return this.content.get(0).compareTo(other.content.get(0));
    }
}
