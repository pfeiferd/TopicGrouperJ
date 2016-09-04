package org.hhn.topicgrouper.tgimpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hhn.topicgrouper.base.DocumentProvider;

public class JMLRTopicGrouper4<T> extends JMLRTopicGrouper<T> {
	public JMLRTopicGrouper4(int minWordFrequency,
			DocumentProvider<T> documentProvider, int minTopics) {
		super(minWordFrequency, documentProvider, minTopics);
	}

	protected void createJoinCandidateList(int maxTopics) {
		joinCandidates = new ArrayList<JMLRTopicGrouper.JoinCandidate>();
	}

	protected void addAllToJoinCandiates(JoinCandidate[] joinCandidates) {
		Collections.addAll(this.joinCandidates, joinCandidates);
		Collections.sort(this.joinCandidates);
	}

	private List<JoinCandidate> joinCandidates;
	private Collection<JoinCandidate> addLater = new ArrayList<JMLRTopicGrouper.JoinCandidate>();
	private Iterator<JoinCandidate> it;

	@Override
	protected JoinCandidate getBestJoinCandidate() {
		return joinCandidates.remove(joinCandidates.size() - 1);
	}

	@Override
	protected void addJoinCandidate(JoinCandidate jc) {
		int insertionPoint = Collections.binarySearch(joinCandidates, jc);
		joinCandidates.add((insertionPoint > -1) ? insertionPoint
				: (-insertionPoint) - 1, jc);
	}

	@Override
	protected void prepareRemoveJoinCandidate(JoinCandidate jc) {
		it.remove();
	}

	@Override
	protected void prepareRemoveJCPartner(
			org.hhn.topicgrouper.tgimpl.JMLRTopicGrouper.JoinCandidate jc) {
		it.remove();
	}

	@Override
	protected void addJoinCandidateLater(JoinCandidate jc) {
		addLater.add(jc);
	}

	@Override
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

	@Override
	protected void iterateOverJCsForUpdate(JoinCandidate jc, int j) {
		it = joinCandidates.iterator();
		addLater.clear();

		while (it.hasNext()) {
			handleJoinCandidateUpdate(jc, it.next(), j);
		}
//		joinCandidates.addAll(addLater);
//		Collections.sort(this.joinCandidates);
		for (JoinCandidate jc2 : addLater) {
			addJoinCandidate(jc2);
		}
	}
}
