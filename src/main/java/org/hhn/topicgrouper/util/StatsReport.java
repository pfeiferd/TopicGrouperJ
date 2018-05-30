package org.hhn.topicgrouper.util;

import java.io.PrintStream;
import java.util.List;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;

public class StatsReport {

	public static <T> void report(DocumentProvider<T> entryProvider, PrintStream out) {
		List<Document<T>> docs = entryProvider.getDocuments();
		out.println("Number of Docs: " + docs.size());
		out.println("Number of Words: " + entryProvider.getVocab().getNumberOfWords());

		double sum1 = 0;
		double diffWordsSum = 0;
		int nDocs = docs.size();
		for (Document<T> d : entryProvider.getDocuments()) {
			sum1 += d.getSize();
			diffWordsSum += d.getWords();
		}
		double avgDocSize = sum1 / nDocs;
		double avgDiffWordsPerDoc = diffWordsSum / nDocs;
		
		double stdDevDocSize = 0;
		double stdDevDiffWordsSum = 0;
		for (Document<T> d : docs) {
			stdDevDocSize += square(d.getSize() - avgDocSize);
			stdDevDiffWordsSum += square(d.getWords() - avgDiffWordsPerDoc);
		}
		
		out.println("Average Doc Size: " + avgDocSize);		
		out.println("Doc Size Std Dev: " +  Math.sqrt(stdDevDocSize / (nDocs - 1)));
		out.println("Average Number of Different Words per Doc: " + avgDiffWordsPerDoc);
		out.println("Number of Different Words per Doc Std Dev: " +  Math.sqrt(stdDevDiffWordsSum / (nDocs - 1)));
		
		int words = entryProvider.getVocab().getNumberOfWords();
		double wordFrSum = 0;
		for (int i = 0; i < words; i++) {
			wordFrSum += entryProvider.getWordFrequency(i);
		}
		double avgWordFr = wordFrSum / words;
		double stdDevWordFr = 0;
		for (int i = 0; i < words; i++) {
			stdDevWordFr += square(entryProvider.getWordFrequency(i) - avgWordFr);
		}
		out.println("Average Word Frequency: " +  avgWordFr);
		out.println("Word Frequency Std Dev: " +  Math.sqrt(stdDevWordFr / (words - 1)));
		
		int sumDF = 0;
		for (int i = 0; i < words; i++) {
			int dfW = 0;
			for (Document<T> d : docs) {
				if (d.getWordFrequency(i) > 0) {
					dfW++;
				}
			}
			sumDF += dfW;
		}
		double avgDF = sumDF / words;
		out.println("Average Document Frequency: " +  avgDF);
		double stdDevDF = 0;
		for (int i = 0; i < words; i++) {
			int dfW = 0;
			for (Document<T> d : docs) {
				if (d.getWordFrequency(i) > 0) {
					dfW++;
				}
			}
			stdDevDF += square(dfW - avgDF);
		}
		out.println("Document Frequency Std Dev: " +  Math.sqrt(stdDevDF / (words - 1)));
	}
	
	private static double square(double a) {
		return a * a;
	}
}
