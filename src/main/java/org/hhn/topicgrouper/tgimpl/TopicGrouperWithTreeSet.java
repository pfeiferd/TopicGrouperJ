package org.hhn.topicgrouper.tgimpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import org.hhn.topicgrouper.base.DocumentProvider;

public class TopicGrouperWithTreeSet<T> extends AbstractTopicGrouper<T> {
	private final Collection<JoinCandidate> addLaterCache;
	private Iterator<JoinCandidate> it;
	private TreeSet<JoinCandidate> joinCandidates;
	
	public TopicGrouperWithTreeSet(int minWordFrequency,
			DocumentProvider<T> documentProvider, int minTopics, double hEpsilon) {
		super(minWordFrequency, documentProvider, minTopics, hEpsilon);
		addLaterCache = new ArrayList<TopicGrouperWithTreeSet.JoinCandidate>();
	}

	public TopicGrouperWithTreeSet(int minWordFrequency,
			DocumentProvider<T> documentProvider, int minTopics) {
		this(minWordFrequency, documentProvider, minTopics, 0);
	}

	protected void addAllToJoinCandiates(JoinCandidate[] joinCandidates) {
		for (int i = 0; i < joinCandidates.length; i++) {
			addJoinCandidate(joinCandidates[i]);
		}
	}

	protected void createJoinCandidateList(int maxTopics) {
		joinCandidates = new TreeSet<JoinCandidate>();
	}

	protected JoinCandidate getBestJoinCandidate() {
		JoinCandidate jc = joinCandidates.last();
		joinCandidates.remove(jc);
		return jc;
	}

	protected void addJoinCandidate(JoinCandidate jc) {
		joinCandidates.add(jc);
	}

	protected void prepareRemoveJoinCandidate(JoinCandidate jc) {
		it.remove();
	}

	protected void prepareRemoveJCPartner(JoinCandidate jc) {
		it.remove();
	}

	protected void addJoinCandidateLater(JoinCandidate jc) {
		addLaterCache.add(jc);
	}

	protected void updateJoinCandidates(JoinCandidate jc) {
		// Save old j-index of jc, cause the join candidate with jc.i == j must
		// be deleted still.
		// jc.i does not need to be saved cause it does not change under the
		// following method call.
		int j = jc.j;
		// Recompute the best join partner for joined topic
		updateJoinCandidateForTopic(jc);
		// Add the new best join partner for topic[jc.i]
		addJoinCandidate(jc);

		iterateOverJCsForUpdate(jc, j);
	}

	protected void iterateOverJCsForUpdate(JoinCandidate jc, int j) {
		it = joinCandidates.iterator();
		addLaterCache.clear();
		while (it.hasNext()) {
			JoinCandidate jc2 = it.next();
			handleJoinCandidateUpdate(jc, jc2, j);
		}
		joinCandidates.addAll(addLaterCache);
	}
}
