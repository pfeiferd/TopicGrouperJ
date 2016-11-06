package org.hhn.topicgrouper.classify.impl;

import gnu.trove.iterator.TIntIterator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hhn.topicgrouper.classify.SupervisedDocumentClassifier;
import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

import de.hsheilbronn.mi.configuration.SvmConfigurationImpl;
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

public abstract class AbstractTopicBasedSVMClassifier<T, L> implements
		SupervisedDocumentClassifier<T, L> {
	private final Map<L, SvmLabelImpl<L>> labelToSvmLabel;
	private int labelCounter;
	private SvmModel model;

	public AbstractTopicBasedSVMClassifier() {
		labelToSvmLabel = new HashMap<L, SvmLabelImpl<L>>();
	}

	public void train(LabelingDocumentProvider<T, L> provider) {
		labelCounter = 0;
		labelToSvmLabel.clear();

		SvmTrainer trainer = new SvmTrainerImpl(
				new SvmConfigurationImpl.Builder().build(),
				"my-custom-trained-model");

		List<SvmDocument> docs = new ArrayList<SvmDocument>();
		for (LabeledDocument<T, L> d : provider.getLabeledDocuments()) {
			docs.add(new SVMDocumentAdapter(d, d.getLabel()));
		}

		model = trainer.train(docs);
	}

	@SuppressWarnings("unchecked")
	public L classify(Document<T> d) {
		SvmClassifier classifier = new SvmClassifierImpl(model);

		List<SvmDocument> classified = classifier.classify(
				Collections.singletonList((SvmDocument)new SVMDocumentAdapter(d, null)),
				true);
		return ((SvmLabelImpl<L>)classified.get(0).getClassLabelWithHighestProbability()).getLabel();
	}

	protected abstract int getTopicIndex(int wordIndex);

	protected abstract int[] getTopicIndices();

	protected SvmLabelImpl<L> getForLabel(L label) {
		SvmLabelImpl<L> sl = labelToSvmLabel.get(label);
		if (sl == null) {
			sl = new SvmLabelImpl<L>(label, labelCounter++);
			labelToSvmLabel.put(label, sl);
		}
		return sl;
	}

	@SuppressWarnings("serial")
	protected class SVMDocumentAdapter implements SvmDocument {
		private final List<SvmFeature> features;
		private final Document<T> document;
		private final List<SvmClassLabel> classLabels;

		public SVMDocumentAdapter(Document<T> document, L label) {
			this.document = document;
			this.features = new ArrayList<SvmFeature>();
			this.classLabels = new ArrayList<SvmClassLabel>();
			TIntIterator it = document.getWordIndices().iterator();
			while (it.hasNext()) {
				int wordIndex = it.next();
				int fr = document.getWordFrequency(wordIndex);
				SvmFeature f = new SvmFeatureImpl(wordIndex, fr);
				features.add(f);
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
