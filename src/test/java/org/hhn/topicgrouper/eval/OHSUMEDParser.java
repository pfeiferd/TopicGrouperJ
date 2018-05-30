package org.hhn.topicgrouper.eval;

import java.util.Enumeration;

import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultLabelingDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultLabelingDocumentProvider.DefaultLabeledDocument;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class OHSUMEDParser {

	public LabelingDocumentProvider<String, String> getCorpusDocumentProvider(
			String fileName) {
		DataSource source;
		try {
			source = new DataSource(fileName);
			Instances instances = source.getDataSet();
			instances.setClassIndex(instances.numAttributes() - 1);

			DefaultLabelingDocumentProvider<String, String> documentProvider = new DefaultLabelingDocumentProvider<String, String>();
			for (Instance instance : instances) {
				Attribute ca = instance.classAttribute();
				DefaultLabelingDocumentProvider<String, String>.DefaultLabeledDocument d = documentProvider
						.newLabeledDocument(instance.stringValue(ca));
				for (int i = 0; i < instance.numValues(); i++) {
					Attribute a = instance.attributeSparse(i);
					if (!a.equals(ca)) {
						int fr = (int) instance.value(a);
						d.addWord(a.name(), fr);
					}
				}
			}
			return documentProvider;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		LabelingDocumentProvider<String, String> p = new OHSUMEDParser()
				.getCorpusDocumentProvider("src/test/resources/ohscal.wc.arff");
		System.out.println(p.getAllLabels());
		System.out.println(p.getDocuments().size());
		System.out.println(p.getSize());
		System.out.println(p.getVocab().getNumberOfWords());
	}
}
