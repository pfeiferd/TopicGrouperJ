# Feature reduction using topic modeling

This document collects the experiments of the paper *Feature reduction using topic modeling* (D. Pfeifer), which
uses the topics of Topic Grouper as features for text classification, as they are reproduced by the module
[`eval`](../eval/README.md) (experiment `classification`, package `org.hhn.topicgrouper.eval.featurereduction`).
The method is explained in [`topic-grouper.md`](topic-grouper.md); the figures are drawn into `results/figs/`
and referenced here from `figs/`.

## 1. Idea

A bag-of-words classifier sees a document as a vector over the vocabulary $V$, typically tens of thousands of
features. A topic model with $\lvert T\rvert$ topics maps the document to the much shorter vector of its topic
frequencies $f_d(t)$ (Section 2 of `topic-grouper.md`). Topic Grouper is suited for this because every word is in
exactly one topic, so the topic frequencies of a document are counted, not inferred, and because its hierarchy
contains a solution for *every* number of topics, so the number of features can be chosen freely after a single
unsupervised run. The classifier then works on $\lvert T\rvert$ features instead of $\lvert V\rvert$.

## 2. Classifiers compared

| Series | Features | Classifier |
|---|---|---|
| `tg-nb` | Topic Grouper topics at $n$ topics | multinomial naive Bayes over the topic counts (Laplace smoothing) |
| `tg-nb-smoothed` | Topic Grouper topics | naive Bayes, class conditional topic probabilities interpolated with the topic prior ($\lambda = 0.3$) |
| `tg-tfidf` | Topic Grouper topics | Rocchio style: tf-idf vectors over topics, one normalized centroid per class, cosine similarity |
| `lda-nb` | LDA topics ($\alpha = 50/K$, $\beta = 0.1$) | naive Bayes over the topic counts of the Gibbs sampler, topics of test documents by folding in |
| `lda-nb-opt` | LDA topics with $\alpha$ and $\beta$ re-estimated | naive Bayes |
| `vocab-ig` | the $k$ words with the highest information gain | naive Bayes over the words |
| `vocab-df` | the $k$ most frequent words (document frequency) | naive Bayes over the words |

The two word based baselines are the classical feature selection methods; with $k = \lvert V\rvert$ they are the
plain bag-of-words classifier. The number of features runs over a grid from 1 to several thousands; the LDA
series stop at 1000 topics (a dense Gibbs sampler is linear in the number of topics, and the accuracy is flat
from a few hundred topics on).

## 3. Corpora

| Corpus | Documents | Classes | Split |
|---|---|---|---|
| Reuters-21578 | ModApte, the ten largest categories, one category per document | 10 | the ModApte training / test split |
| 20 Newsgroups | the 18828 duplicate-free version | 20 | random hold-out |
| OHSUMED | `ohscal` (medical abstracts, bag of words distribution) | 10 | random hold-out |

Texts are lower cased, tokenized, freed from stop words and Porter stemmed (Reuters, 20 Newsgroups);
`ohscal` is distributed as word counts.

## 4. Results

Accuracy over the number of features, micro averaged (every test document counts once) and macro averaged
(every class counts once).

![Figure 1a](figs/classification-reuters-micro.svg)
![Figure 1b](figs/classification-reuters-macro.svg)

*Figure 1: Reuters-21578 (`figs/classification-reuters-micro.svg`, `figs/classification-reuters-macro.svg`).*

![Figure 2a](figs/classification-20ng-micro.svg)
![Figure 2b](figs/classification-20ng-macro.svg)

*Figure 2: 20 Newsgroups (`figs/classification-20ng-micro.svg`, `figs/classification-20ng-macro.svg`).*

![Figure 3a](figs/classification-ohsumed-micro.svg)
![Figure 3b](figs/classification-ohsumed-macro.svg)

*Figure 3: OHSUMED (`figs/classification-ohsumed-micro.svg`, `figs/classification-ohsumed-macro.svg`).*

## 5. Reproducing

```
eval/scripts/download-data.sh                      # Reuters-21578, 20 Newsgroups, OHSUMED
eval/scripts/run-evaluation.sh classification      # writes results/classification-<corpus>.csv and results/figs/
```

The CSV files have the columns `method;topics;microAvg;macroAvg;lambda`; `eval/scripts/draw-figures.sh` redraws
the figures from them. Copy `results/figs/` to `doc/figs/` to update this document. The classifiers are in
`org.hhn.topicgrouper.eval.classify` of the `eval` module (`TGBasicProbClassifier`, `AbstractTopicBasedTfIdfClassifier`,
`LDABasicProbClassifier`, `VocabIGBasicProbClassifier`, `VocabDFBasicProbClassifier`).
