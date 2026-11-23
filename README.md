# TopicGrouperJ

A Java implementation and library for Topic Grouper, a complementary approach in the field of probabilistic topic
modeling. Topic Grouper creates a disjoint partitioning of the training vocabulary in a stepwise manner such that
the resulting partitions represent topics. It is governed by a simple generative model, where the likelihood to
generate the training documents via topics is optimized. The algorithm starts with one-word topics and joins two
topics at every step; it therefore generates a solution for every number of topics between the size of the
training vocabulary and one. The process is an agglomerative clustering that corresponds to a binary tree of
topics, which acts as a containment hierarchy with general topics towards the root and specific topics towards
the leaves. Topic Grouper is not governed by a background distribution such as the Dirichlet and avoids hyper
parameter optimizations.

## Documentation

* [doc/topic-grouper.md](doc/topic-grouper.md): the model, the algorithm, the evaluation with all figures of the
  papers, the extension for homonyms (words with several senses) and the library's hierarchy files, drawings
  and browser.
* [doc/feature-reduction.md](doc/feature-reduction.md): topics as features for text classification, the
  experiments of the paper *Feature reduction using topic modeling*.
* [eval/README.md](eval/README.md): how to run the experiments that produce the figures.

## How to cite

* D. Pfeifer, J. L. Leidner: *Topic Grouper: An Agglomerative Clustering Approach to Topic Modeling.*
  ECIR 2019. [Springer](https://link.springer.com/chapter/10.1007/978-3-030-15712-8_38); extended version on
  arXiv: [arXiv:1904.06483](http://arxiv.org/abs/1904.06483). The conference slides:
  [TopicGrouperECIR2019.pdf](https://github.com/pfeiferd/TopicGrouperJ/blob/master/TopicGrouperECIR2019.pdf).
* D. Pfeifer: *Feature reduction using topic modeling.* See [doc/feature-reduction.md](doc/feature-reduction.md).

## Building

The project requires Java 8 or newer and Maven and consists of two modules:

* `api`: the library (Topic Grouper, LDA, PLSA, perplexity estimators, topic based classifiers, hierarchy files,
  SVG drawings and the hierarchy browser), artifact `topic-grouper-api`;
* `eval`: the experiments of the papers, artifact `topic-grouper-eval` (depends on `api`).

    mvn clean install

builds both and runs the unit tests; the functional tests of the library are in `api/src/test`.

## Usage

```java
// 1. Collect the training documents (a bag of words per document).
DefaultDocumentProvider<String> provider = new DefaultDocumentProvider<String>();
DefaultDocumentProvider<String>.DefaultDocument d = provider.newDocument();
d.addWord("topic");
d.addWord("model", 2); // word with frequency 2

// 2. Group the vocabulary. Only words occurring at least twice are used, the
//    algorithm runs down to a single topic, 4 threads are used.
TGSolver<String> solver = new EHACTopicGrouper<String>(2, provider, 1, 4);
// Use LowMemTopicGrouper for large vocabularies, SenseSplittingTopicGrouper for word senses.

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

Packages below `org.hhn.topicgrouper`: `doc` (documents, vocabularies, splitters), `tg` (Topic Grouper in `impl`,
reporting and hierarchy files in `report`, perplexity in `validation`, drawings in `paint`, the browser in `viz`),
`lda` and `plsa` (for comparison), `validation` (perplexity estimators) and `data` (parsers of the corpora used
in the papers). The topic based classifiers of the feature reduction study live in the `eval` module.
The hierarchy files, the drawings and the browser are described in
[doc/topic-grouper.md](doc/topic-grouper.md#7-library-notes-hierarchy-files-drawings-and-the-browser).

## Reproducing the evaluation

    eval/scripts/run-all.sh --quick

downloads the public data sets and runs all experiments (`--quick` for a smoke test); the CSV files and the SVG
figures end up in `eval/results/` and `eval/results/figs/`. See [eval/README.md](eval/README.md).
