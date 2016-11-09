package org.hhn.topicgrouper.classify.impl;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

import de.hsheilbronn.mi.configuration.SvmConfigurationImpl;
import de.hsheilbronn.mi.configuration.SvmType;
import de.hsheilbronn.mi.domain.SvmClassLabel;
import de.hsheilbronn.mi.domain.SvmClassLabelImpl;
import de.hsheilbronn.mi.domain.SvmDocument;
import de.hsheilbronn.mi.domain.SvmFeature;
import de.hsheilbronn.mi.domain.SvmFeatureImpl;
import de.hsheilbronn.mi.domain.SvmModel;
import de.hsheilbronn.mi.process.SvmClassifier;
import de.hsheilbronn.mi.process.SvmClassifierImpl;
import de.hsheilbronn.mi.process.SvmTrainer;
import de.hsheilbronn.mi.process.SvmTrainerImpl;

public abstract class AbstractTopicBasedSVMClassifier<T, L> extends
		AbstractTopicBasedClassifier<T, L> {
	private final TIntDoubleMap idf;
	private final Map<L, SvmLabelImpl<L>> labelToSvmLabel;
	private final List<L> usedLabels;
	private int labelCounter;
	private SvmModel model;

	public AbstractTopicBasedSVMClassifier() {
		labelToSvmLabel = new HashMap<L, SvmLabelImpl<L>>();
		usedLabels = new ArrayList<L>();
		idf = new TIntDoubleHashMap();
	}

	public void train(LabelingDocumentProvider<T, L> provider) {
		idf.clear();
		labelCounter = 0;
		labelToSvmLabel.clear();
		usedLabels.clear();
		updateTopicIndices();

		List<Document<T>> ds = provider.getDocuments();
		for (int i = 0; i < topicIndices.length; i++) {
			int df = 0;
			for (Document<T> d : ds) {
				TIntIterator it = d.getWordIndices().iterator();
				while (it.hasNext()) {
					int wordIndex = it.next();
					int fr = d.getWordFrequency(wordIndex);
					if (fr > 0 && getTopicIndex(wordIndex) == topicIndices[i]) {
						df++;
						break;
					}
				}
			}
			// Using idf (whith log) improves accuracy considerably (by about
			// 4%)
			idf.put(i, Math.log(((double) ds.size()) / df));
		}

		SvmTrainer trainer = new SvmTrainerImpl(
				new SvmConfigurationImpl.Builder().setSvmType(SvmType.C_SVC)
						.build(), "my-custom-trained-model");

		List<SvmDocument> docs = new ArrayList<SvmDocument>();
		double[] ftd = new double[topicIndices.length];
		for (LabeledDocument<T, L> d : provider.getLabeledDocuments()) {
			computeTopicFrequency(d, ftd, false);
			docs.add(new SVMDocumentAdapter(d, d.getLabel(), ftd));
		}

		model = trainer.train(docs);
	}

	public L classify(Document<T> d) {
		SvmClassifier classifier = new SvmClassifierImpl(model);
		double[] ftd = new double[topicIndices.length];
		computeTopicFrequency(d, ftd, true);

		List<SvmDocument> classified = classifier.classify(Collections
				.singletonList((SvmDocument) new SVMDocumentAdapter(d, null, ftd)),
				false);
		SvmClassLabel l = classified.get(0)
				.getClassLabelWithHighestProbability();
		return usedLabels.get((int) l.getNumeric());
	}

	protected SvmLabelImpl<L> getForLabel(L label) {
		SvmLabelImpl<L> sl = labelToSvmLabel.get(label);
		if (sl == null) {
			sl = new SvmLabelImpl<L>(label, labelCounter++);
			labelToSvmLabel.put(label, sl);
			usedLabels.add(label);
		}
		return sl;
	}

	@SuppressWarnings("serial")
	protected class SVMDocumentAdapter implements SvmDocument {
		private final List<SvmFeature> features;
		private final Document<T> document;
		private final List<SvmClassLabel> classLabels;

		public SVMDocumentAdapter(Document<T> document, L label, double[] ftd) {
			this.document = document;
			this.features = new ArrayList<SvmFeature>();
			this.classLabels = new ArrayList<SvmClassLabel>();
			
			int sumSquare = 0;
			for (int i = 0; i < ftd.length; i++) {
				double tfidf = ftd[i] * idf.get(i);
				SvmFeature f = new SvmFeatureImpl(i, tfidf);
				features.add(f);
				sumSquare += tfidf * tfidf;
			}
			double norm = Math.sqrt(sumSquare);
			for (SvmFeature f : features) {
				f.setValue(f.getValue() / norm);
			}
			if (label != null) {
				addClassLabel(getForLabel(label));
			}
		}

		public Document<T> getDocument() {
			return document;
		}

		@Override
		public void addClassLabel(SvmClassLabel classLabel) {
			classLabels.add(classLabel);
		}

		@Override
		public List<SvmClassLabel> getAllClassLabels() {
			return classLabels;
		}

		@Override
		public SvmClassLabel getClassLabelWithHighestProbability() {
			if (classLabels.isEmpty()) {
				return null;
			}

			return Collections.max(classLabels);
		}

		@Override
		public List<SvmFeature> getSvmFeatures() {
			return features;
		}
	}

	@SuppressWarnings("serial")
	protected static class SvmLabelImpl<L> extends SvmClassLabelImpl {
		private final L label;

		public SvmLabelImpl(L label, int id) {
			super(id, label.toString());
			this.label = label;
		}

		public L getLabel() {
			return label;
		}
	}
}
