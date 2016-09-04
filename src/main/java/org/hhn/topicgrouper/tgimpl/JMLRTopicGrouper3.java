package org.hhn.topicgrouper.tgimpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.hhn.topicgrouper.base.DocumentProvider;


public class JMLRTopicGrouper3<T> extends JMLRTopicGrouper<T> {
	public JMLRTopicGrouper3(int minWordFrequency,
			DocumentProvider<T> documentProvider, int minTopics) {
		super(minWordFrequency, documentProvider, minTopics);
	}
	
	protected void createJoinCandidateList(int maxTopics) {
		joinCandidates = new ArrayList<JMLRTopicGrouper.JoinCandidate>();
	}
	
	protected void addAllToJoinCandiates(JoinCandidate[] joinCandidates) {
		Collections.addAll(this.joinCandidates, joinCandidates);
	}

	private Collection<JoinCandidate> joinCandidates;
	private Collection<JoinCandidate> addLater = new ArrayList<JMLRTopicGrouper.JoinCandidate>();
	private Iterator<JoinCandidate> it;
	
	@Override
	protected JoinCandidate getBestJoinCandidate() {
		double maxImprovement = Double.NEGATIVE_INFINITY;
		JoinCandidate bestJC = null;
		for (JoinCandidate jc : joinCandidates) {
			if (jc != null) {
				if (jc.improvement > maxImprovement) {
					maxImprovement = jc.improvement;
					bestJC = jc;
				}
			}			
		}
		joinCandidates.remove(bestJC);

		return bestJC;
	}
	
	@Override
	protected void addJoinCandidate(JoinCandidate jc) {
		joinCandidates.add(jc);
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
		joinCandidates.addAll(addLater);		
	}
}
