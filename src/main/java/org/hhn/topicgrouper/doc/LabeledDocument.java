package org.hhn.topicgrouper.doc;


public interface LabeledDocument<T,L> extends Document<T> {
	L getLabel();
}
