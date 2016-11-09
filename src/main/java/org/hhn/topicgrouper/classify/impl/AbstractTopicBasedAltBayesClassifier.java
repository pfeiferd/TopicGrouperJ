package org.hhn.topicgrouper.classify.impl;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

public abstract class AbstractTopicBasedAltBayesClassifier<T, L> extends AbstractTopicBasedClassifier<T, L> {
	private final List<L> labels;
	private final List<TDoubleList> pct;

	public AbstractTopicBasedAltBayesClassifier() {
		pct = new ArrayList<TDoubleList>();
		labels = new ArrayList<L>();
	}

	public void train(LabelingDocumentProvider<T, L> provider) {
		pct.clear();
		labels.clear();
		updateTopicIndices();

		int ntopics = topicIndices.length;
		double[] sum = new double[ntopics];
		
		int l = 0;		
		for (L label : provider.getAllLabels()) {
			labels.add(label);
			List<LabeledDocument<T, L>> labeledDocs = provider
					.getDocumentsWithLabel(label);
			TDoubleList pc = pct.size() <= l ? null : pct.get(l);
			if (pc == null) {
				pc = new TDoubleArrayList();
				pct.add(pc);
			}
			else {
				pc.clear();
			}
			Arrays.fill(sum, 0);
			for (LabeledDocument<T, L> d : labeledDocs) {
				computeTopicFrequency(d, sum, true);
			}

			for (int t = 0; t < ntopics; t++) {
				pc.add(sum[t] / getTopicFrequency(t));
			}
			l++;
		}
	}

	public L classify(Document<T> d) {
		double bestValue = Double.NEGATIVE_INFINITY;
		L bestLabel = null;
		int ntopics = topicIndices.length;
		double[] ftd = new double[ntopics];
		computeTopicFrequency(d, ftd, true);
		
		int l = 0;
		for (L label : labels) {
			double sum = 0;
			for (int t = 0; t < ntopics; t++) {
				sum += pct.get(l).get(t) * ftd[t];
			}
			if (sum >= bestValue) {
				bestValue = sum;
				bestLabel = label;
			}
			l++;
		}
		return bestLabel;
	}
	
	protected abstract double getTopicFrequency(int topic);
}
