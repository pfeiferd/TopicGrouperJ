package org.hhn.topicgrouper.tgimpl;

import org.hhn.topicgrouper.base.DocumentProvider;

public class JMLRTopicGrouper2<T> extends JMLRTopicGrouper<T> {
	public JMLRTopicGrouper2(int minWordFrequency,
			DocumentProvider<T> documentProvider, int minTopics) {
		super(minWordFrequency, documentProvider, minTopics);
	}
	
	protected void createJoinCandidateList(int maxTopics) {
	}
	
	protected void addAllToJoinCandiates(JoinCandidate[] joinCandidates) {
		this.joinCandidates = joinCandidates;
	}

	private JoinCandidate[] joinCandidates;
	
	@Override
	protected JoinCandidate getBestJoinCandidate() {
		double maxImprovement = Double.NEGATIVE_INFINITY;
		int bestI = -1;
		for (int i = 0; i < joinCandidates.length; i++) {
			if (joinCandidates[i] != null) {
				if (joinCandidates[i].improvement >= maxImprovement) {
					maxImprovement = joinCandidates[i].improvement;
					bestI = i;					
				}
			}
		}
		JoinCandidate jc = joinCandidates[bestI];
		joinCandidates[bestI] = null;
		return jc;
	}
	
	@Override
	protected void addJoinCandidate(JoinCandidate jc) {
		joinCandidates[jc.i] = jc;
	}
	
	@Override
	protected void prepareRemoveJoinCandidate(JoinCandidate jc) {
	}
	
	@Override
	protected void prepareRemoveJCPartner(
			org.hhn.topicgrouper.tgimpl.JMLRTopicGrouper.JoinCandidate jc) {
		joinCandidates[jc.i] = null;
	}

	@Override
	protected void addJoinCandidateLater(JoinCandidate jc) {
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
		for (int i = 0; i < joinCandidates.length; i++) {
			if (joinCandidates[i] != null) {
				handleJoinCandidateUpdate(jc, joinCandidates[i], j);
			}			
		}
	}
}
