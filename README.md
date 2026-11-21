# TopicGrouperJ
We offer a Java implementation and library for Topic Grouper, a complementary approach in the field of
probabilistic topic modeling.  Topic Grouper creates a disjoint
partitioning of the training vocabulary in a stepwise manner such that
resulting partitions represent topics.  It is governed by a simple
generative model, where the likelihood to generate the training
documents via topics is optimized.  The algorithm starts with one-word
topics and joins two topics at every step. It therefore generates a
solution for every desired number of topics ranging between the size
of the training vocabulary and one. The process represents an
agglomerative clustering that corresponds to a binary tree of topics. A
resulting tree may act as a containment hierarchy, typically with more
general topics towards the root of tree and more specific topics
towards the leaves. Topic Grouper is not governed by a background
distribution such as the Dirichlet and avoids hyper parameter
optimizations.

This is the publication behind the approach:
https://link.springer.com/chapter/10.1007/978-3-030-15712-8_38

Also, there is an extended version of this publication available on ArXiv:
http://arxiv.org/abs/1904.06483

The related conference slides can be found here:
https://github.com/pfeiferd/TopicGrouperJ/blob/master/TopicGrouperECIR2019.pdf

## Building

The project requires Java 8 or newer and Maven:

    mvn clean install

Run the unit tests with `mvn test`. The original experiment code of the paper lives in
`src/test/java/org/hhn/topicgrouper/paper`; it needs libraries that are not on Maven Central and is only
compiled with `-Plegacy-experiments`. Use the module `evaluation` (below) to reproduce the results.

## Usage

```java
// 1. Collect the training documents (a bag of words per document).
DefaultDocumentProvider<String> provider = new DefaultDocumentProvider<String>();
DefaultDocumentProvider<String>.DefaultDocument d = provider.newDocument();
d.addWord("topic");
d.addWord("model", 2); // word with frequency 2

// 2. Group the vocabulary. Only words occurring at least twice are used,
//    the algorithm runs down to a single topic.
TGSolver<String> solver = new EHACTopicGrouper<String>(2, provider, 1);
// Use LowMemTopicGrouper for large vocabularies.

// 3. Listen to the solution after every join of two topics.
solver.solve(new TGSolutionListener<String>() {
    public void updatedSolution(int newTopicIndex, int oldTopicIndex,
            double improvement, int t1Size, int t2Size, TGSolution<String> solution) {
        System.out.println(solution.getNumberOfTopics() + " topics, log likelihood "
                + solution.getTotalLogLikelhood());
    }
    // ... remaining callbacks (beforeInitialization, initalizing, initialized, done)
});
```

Overview of the packages (below `org.hhn.topicgrouper`):

* `doc` - documents, vocabularies, document providers and splitters.
* `tg` - Topic Grouper (`impl`), result reporting (`report`), perplexity (`validation`) and a hierarchy browser (`viz`).
* `lda`, `plsa` - LDA (Gibbs sampling) and PLSA for comparison.
* `validation` - perplexity computation (also left-to-right).
* `classify` - topic based document classifiers (naive Bayes, TF-IDF).

## Notes

* `LDAGibbsSampler` writes a log file `alphabetalog.log` to the working directory.
* `EHACTopicGrouper` needs memory quadratic in the vocabulary size; `LowMemTopicGrouper` does not.

## Reproducing the evaluation

The module [`evaluation`](evaluation) re-runs the experiments of the paper on public data (synthetic data, NIPS,
Reuters-21578, 20 Newsgroups), writes CSV files and contains a TikZ/pgfplots template for the figures:

    evaluation/scripts/run-all.sh --quick

See [evaluation/README.md](evaluation/README.md) for details.
