package org.hhn.topicgrouper.ldagibbs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hhn.topicgrouper.base.Document;
import org.hhn.topicgrouper.base.DocumentProvider;

import external.lda.models.GibbsSamplingLDA;
import gnu.trove.iterator.TIntIterator;

public class GibbsSamplingLDAAdapt extends GibbsSamplingLDA {
	public GibbsSamplingLDAAdapt(DocumentProvider<String> documentProvider,
			double[] inAlpha, double inBeta,
			int inNumIterations, int inTopWords, String inExpName,
			String pathToTAfile, int inSaveStep) throws Exception {
		super(null, inAlpha, inBeta, inNumIterations, inTopWords,
				inExpName, pathToTAfile, inSaveStep, documentProvider);
	}

	@Override
	protected void initFromCorpus(String pathToCorpus, Object[] addArgs) {
		DocumentProvider<String> documentProvider = (DocumentProvider<String>) addArgs[0];
		List<Document<String>> documents = documentProvider.getDocuments();
		
		word2IdVocabulary = new HashMap<String, Integer>();
		id2WordVocabulary = new HashMap<Integer, String>();
		corpus = new ArrayList<List<Integer>>();
		numDocuments = documents.size();
		numWordsInCorpus = 0;
		
		for (int i = 0; i < documents.size(); i++) {
			List<Integer> d = new ArrayList<Integer>();
			TIntIterator it = documents.get(i).getWordIndices().iterator();
			while (it.hasNext()) {
				int index = it.next();
				String word = documentProvider.getWord(index);
				if (word2IdVocabulary.containsKey(word)) {
					d.add(word2IdVocabulary.get(word));
				}				
				else {
					word2IdVocabulary.put(word, index);
					id2WordVocabulary.put(index, word);
					d.add(index);
				}
			}
			numWordsInCorpus += d.size();
			corpus.add(d);			
			vocabularySize = documentProvider.getNumberOfWords();
		}
	}
}
