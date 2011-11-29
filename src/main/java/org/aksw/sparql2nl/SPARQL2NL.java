package org.aksw.sparql2nl;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.aksw.sparql2nl.naturallanguagegeneration.NaturalLanguageGenerator;
import org.aksw.sparql2nl.queryprocessing.Query;
import org.aksw.sparql2nl.queryprocessing.Similarity;
import org.aksw.sparql2nl.queryprocessing.Similarity.SimilarityMeasure;
import org.apache.commons.lang3.StringEscapeUtils;
import org.dllearner.algorithm.tbsl.sparql.Template;

import com.hp.hpl.jena.query.QueryFactory;
import com.ibm.icu.util.Measure;

public class SPARQL2NL {
	
	private static final String TEMPLATES_FILE = "resources/corpus";
	
	private Hashtable<Template, String> corpus;
	private SimilarityMeasure measure = SimilarityMeasure.GRAPH_ISOMORPHY;
	
	public SPARQL2NL() {
		corpus = readCorpus();
	}
	
	public void setMeasure(SimilarityMeasure measure) {
		this.measure = measure;
	}
	
	private Hashtable<Template, String> readCorpus(){
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(TEMPLATES_FILE)));
			Hashtable<Template, String> templates = (Hashtable<Template, String>)ois.readObject();
			return templates;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void expandPrefixes(com.hp.hpl.jena.query.Query query){
		for(Entry<String, String> e : query.getPrefixMapping().getNsPrefixMap().entrySet()){
			query.getPrefixMapping().removeNsPrefix(e.getKey());
		}
	}
	
	private String expandPrefixes(String queryString){
		com.hp.hpl.jena.query.Query query = QueryFactory.create(queryString);
		expandPrefixes(query);
		return query.toString();
	}
	
	private <K, V extends Comparable<V>> List<Entry<K, V>> sortByValues(Map<K, V> map){
		List<Entry<K, V>> entries = new ArrayList<Entry<K, V>>(map.entrySet());
        Collections.sort(entries, new Comparator<Entry<K, V>>() {

			@Override
			public int compare(Entry<K, V> o1, Entry<K, V> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
        return entries;
	}
	
	private <K extends Comparable<K>, V extends Comparable<V>> List<Entry<K, V>> sortByValuesThanKeys(Map<K, V> map){
		List<Entry<K, V>> entries = new ArrayList<Entry<K, V>>(map.entrySet());
        Collections.sort(entries, new Comparator<Entry<K, V>>() {

			@Override
			public int compare(Entry<K, V> o1, Entry<K, V> o2) {
				int value = o2.getValue().compareTo(o1.getValue());
				if(value == 0){
					value = o2.getKey().compareTo(o1.getKey());
				}
				return value;
			}
		});
        return entries;
	}
	
	public Set<String> getNaturalLanguageRepresentations(String sparqlQuery){
		return getNaturalLanguageRepresentations(sparqlQuery, -1);
	}
	
	public Set<String> getNaturalLanguageRepresentations(String sparqlQuery, int limit){
		Set<String> nl = new HashSet<String>();
		
		Query q1 = new Query(expandPrefixes(sparqlQuery));
		
		Map<Template, Double> template2simMap = new HashMap<Template, Double>();
		for(Template t : corpus.keySet()){
			//TODO here is a bug in the template generation, where some Queries only contain the String 'ERROR'
			if(t.getQuery().toString().contains("ERROR"))continue;
			Query q2 = new Query(t.getQuery().toString());
			double sim = Similarity.getSimilarity(q1, q2, measure);
			template2simMap.put(t, sim);
		}
		
		List<Entry<Template, Double>> sortedEntries = sortByValuesThanKeys(template2simMap);
		if(limit > 0){
			sortedEntries = sortedEntries.subList(0, Math.min(sortedEntries.size(), limit));
		}
		NaturalLanguageGenerator nlGen;
		for(Entry<Template, Double> entry : sortedEntries){
			nlGen = new NaturalLanguageGenerator(q1, entry.getKey(), corpus.get(entry.getKey()));
//			System.out.println(q1.getOriginalQuery());
			System.out.println(entry.getKey().getQuery());
			System.out.println(corpus.get(entry.getKey()));
			nl.add(nlGen.generateNaturalLanguageFromSparqlQuery());
		}
		
		return nl;
	}

	/**
	 * @param args 
	 */
	public static void main(String[] args) throws Exception{
		String filePath = "resources/single_test.txt";
	byte[] buffer = new byte[(int) new File(filePath).length()];
    BufferedInputStream f = null;
    try {
        f = new BufferedInputStream(new FileInputStream(filePath));
        f.read(buffer);
    } finally {
        if (f != null) try { f.close(); } catch (IOException ignored) { }
    }
    String queryString = new String(buffer);
    queryString = StringEscapeUtils.unescapeHtml4(queryString);
    System.out.println(queryString);
    
		System.out.println(new SPARQL2NL().getNaturalLanguageRepresentations(queryString));
	}

}