package org.hhn.dm.demo.task2;

import java.io.File;
import java.util.Random;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.doc.impl.LabelingHoldOutSplitter;
import org.hhn.topicgrouper.data.OHSUMEDParser;
import org.hhn.topicgrouper.data.TwentyNGParser;
import org.hhn.topicgrouper.util.StatsReport;

public class TwentyNGClassifierBase {
	public TwentyNGClassifierBase() {
	}

	public LabelingDocumentProvider<String, String> loadData(boolean oshumed) {
		if (oshumed) {
			return new OHSUMEDParser()
					.getCorpusDocumentProvider("src/test/resources/ohscal.wc.arff");
		}
		else {
			return new TwentyNGParser(System.out, true)
					.getCorpusDocumentProvider(new File("src/test/resources/20news-18828"), 1d);
		}
	}

	public LabelingHoldOutSplitter<String, String> splitData(LabelingDocumentProvider<String, String> provider) {
		return new LabelingHoldOutSplitter<String, String>(new Random(42), provider, 0.25, 5, -1);
	}

	protected Model train(LabelingDocumentProvider<String, String> trainingData) {
		return new NullModel();
	}

	protected double test(Model model, LabelingDocumentProvider<String, String> testData) {
		int samples = 0;
		int correct = 0;
		for (LabeledDocument<String, String> d : testData.getLabeledDocuments()) {
			String prediction = model.predict(d);
			samples++;
			if ((prediction != null && prediction.equals(d.getLabel()))
					|| (prediction == null && d.getLabel() == null)) {
				correct++;
			}
		}
		return ((double) correct) / samples;
	}

	public interface Model {
		String predict(Document<String> doc);
	}
	
	public class NullModel implements Model {
		@Override
		public String predict(Document<String> doc) {
			return null;
		}
	}

	public void runMiniPipeline(boolean oshumed) {
		System.out.println(oshumed ? "** OSHUMED **" : "** Twenty Newsgroups **");
		LabelingDocumentProvider<String, String> data = loadData(oshumed);
		StatsReport.report(data, System.out);
		System.out.println("Labels: " + data.getAllLabels());

		LabelingHoldOutSplitter<String, String> splitter = splitData(data);
		Model model = train(splitter.getRest());
		double accuracy = test(model, splitter.getHoldOut());

		System.out.println("Accuracy: " + accuracy);
	}
	
	public static void main(String[] args) {
		new TwentyNGNBClassifier().runMiniPipeline(true);
		new TwentyNGNBClassifier().runMiniPipeline(false);
	}
}
