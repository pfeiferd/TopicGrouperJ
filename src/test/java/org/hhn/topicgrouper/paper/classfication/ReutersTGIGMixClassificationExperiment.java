package org.hhn.topicgrouper.paper.classfication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

import org.hhn.topicgrouper.classify.impl.AbstractTopicBasedNBClassifier;
import org.hhn.topicgrouper.classify.impl.tg.TGIGMixNBClassifier2;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.tg.report.store.MapNode;

public class ReutersTGIGMixClassificationExperiment extends
		ReutersVocabIGClassificationExperiment {
	private TGIGMixNBClassifier2<String, String> classifier;
	private final List<MapNode<String>> allNodes;

	public ReutersTGIGMixClassificationExperiment(Class<?> clazz)
			throws IOException {
		super();
		allNodes = loadFile(new File("./target/" + clazz.getSimpleName()
				+ ".ser"));
	}

	@SuppressWarnings("unchecked")
	protected List<MapNode<String>> loadFile(File file) {
		try {
			ObjectInputStream oi = new ObjectInputStream(new FileInputStream(
					file));
			List<MapNode<String>> res = (List<MapNode<String>>) oi.readObject();
			oi.close();
			return res;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected AbstractTopicBasedNBClassifier<String, String> createClassifier(
			int topics,
			LabelingDocumentProvider<String, String> documentProvider) {
		if (classifier == null) {
			try {
				classifier = new TGIGMixNBClassifier2<String, String>(allNodes,
						documentProvider, new File("./target/"
								+ this.getClass().getSimpleName()
								+ "_TGIGMixScoreList.ser"));
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		classifier.setMaxTopicsAlt(topics);
		System.out.println(topics + " " + classifier.averageTopicLength());
		return classifier;
	}

	public static void main(String[] args) throws IOException {
		//new ReutersTGNaiveBayesExperiment().run();
		new ReutersTGIGMixClassificationExperiment(
				ReutersTGNaiveBayesExperiment.class).run(false);
	}
}
