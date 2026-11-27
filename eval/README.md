# Topic Grouper evaluation

The experiments of the papers, runnable from one jar and producing CSV files and the SVG figures of
the papers. The module depends on the library module `api`.

| Paper | Experiments | Package |
|---|---|---|
| Topic Grouper (ECIR 2019 and its extended version) | `perplexity`, `twc-series`, `topics`, `performance` | `org.hhn.topicgrouper.eval.ecir2019` |
| Feature reduction using topic modeling | `classification` | `org.hhn.topicgrouper.eval.featurereduction` |
| Homonym extension (`doc/topic-grouper.md`, Section 6) | `homonyms` | `org.hhn.topicgrouper.eval.homonyms` |

## Quick start

```
mvn install                                   # builds api and eval, runs the tests
scripts/download-data.sh                      # the publicly available data sets (see below)
java -jar eval/target/topic-grouper-eval.jar ecir2019 --quick    # a small run of all ECIR experiments
java -jar eval/target/topic-grouper-eval.jar all                 # the full runs (days)
```

`java -jar topic-grouper-eval.jar [experiments] [--data DIR] [--out DIR] [--quick] [--lda-burn-in N] [--lda-iterations N] [--threads N]`

* `experiments`: a comma separated list of experiment names (`perplexity`, `twc-series`,
  `topics`, `classification`, `homonyms`, `performance`), paper names (`ecir2019`, `feature-reduction`,
  `homonyms`) or `all` (the default). They run in the given order (`all`: the papers in the order of
  the table above), except that the run time measurements (`performance`) always run last.
* `--data DIR`: the data directory (default `data`); experiments whose data set is missing are skipped
  with a message.
* `--out DIR`: the result directory (default `results`).
* `--quick`: small sizes (few topics, iterations, repetitions, real corpora reduced to 2000 random
  documents) to check that everything runs; the numbers are not meaningful.
* `--lda-burn-in N`, `--lda-iterations N`: burn-in and sampling iterations of the LDA models in the
  perplexity, `twc-series` and `homonyms` experiments (defaults 500 and 200, see below; the LDA
  training dominates the run time on the text corpora).
* `--threads N`: threads for independent model runs, i.e. the LDA models of the different topic
  numbers in the perplexity experiment (default: the available processors; the results do not depend
  on it).

An experiment whose CSV file already exists in the result directory is skipped, so an interrupted run is
resumed by starting it again. While an experiment runs, its rows go to `<name>.csv.part`; the file is
renamed when the experiment has finished, so a `.part` file is always an unfinished one. Delete a CSV file
to repeat its experiment.

The sizes of the full and the quick runs are in `Settings`.

The hierarchies recorded by the `topics` experiment (`results/<corpus>.json.gz`) can be drawn again
with other settings and browsed:

```
scripts/draw-hierarchy.sh results/reuters.json.gz reuters-20.svg --topics 20 --cost --indent --dashed 0.5
scripts/browse-hierarchy.sh results/reuters.json.gz
```

`draw-hierarchy.sh` takes `--topics N` (stop at the solution with N topics), `--min-size N`,
`--max-depth N`, `--top-words N`, `--font-size N`, `--cost`, `--likelihood`, `--no-ids`,
`--no-word-count`, `--indent` (by join cost) and `--dashed COST`; `browse-hierarchy.sh` starts the Swing browser of the `api` module
(FlatLaf), which also exports SVG drawings of a selected subtree.

## Data

Put the data sets into `data/` (or `--data`). `scripts/download-data.sh` fetches the public ones.

| Data set | Used by | Expected files | Source |
|---|---|---|---|
| synthetic corpus ("TWC") | perplexity, twc-series, performance, topics, homonyms | generated | `data/TWCLDAPaperDocumentGenerator` (after Tan and Ou) |
| NIPS | perplexity, topics | `nips/docword.nips.txt`, `nips/vocab.nips.txt` | UCI bag of words |
| New York Times | topics | `nytimes/docword.nytimes.txt`, `nytimes/vocab.nytimes.txt` | UCI bag of words (large) |
| AP extract (2246 articles) | perplexity, performance, topics | `ap/ap.txt` | Blei's lda-c `ap.tgz` |
| AP (TREC) | perplexity, topics | `ap-full/ap*` (the SGML files) | TREC / LDC, not public |
| Reuters-21578 | perplexity, classification, performance, topics | `reuters21578/*.sgm`, or `reuters-nltk/reuters.zip` | UCI KDD, or the ApteMod version distributed with NLTK |
| 20 Newsgroups | classification, performance | `20news-18828/<group>/<file>` | qwone.com (duplicates removed) |
| OHSUMED (ohscal) | classification | `ohsumed/ohscal.wc.arff` | Forman's 19 text data sets (sparse ARFF) |
| Online Retail | perplexity, topics | `online-retail/Online Retail.csv` | UCI (ships as `.xlsx`; export as `;` separated CSV) |
| Ta-Feng grocery | perplexity | `tafeng/D01_D02_D11_D12` (the four files concatenated with one header) | mirrors of the Ta-Feng data set |
| US supermarket | perplexity | `us-supermarket/vocab.uss.csv`, `us-supermarket/docword.uss.csv` | not public |

The parsers are in `org.hhn.topicgrouper.data` of the `api` module; their Javadoc describes the formats.

## Experiments and output files

All CSV files are `;` separated with a header line; `method` (or `solver`) names the series.

| File | Columns | Content |
|---|---|---|
| `perplexity-<corpus>.csv` | `method;topics;perplexity;alphaConc;splits` | hold-out perplexity over the number of topics (`splits`: the number of word senses created by the sense splitting solver, only for `tg-sense`): `tg` (Topic Grouper, Dirichlet concentration optimized on a part of the training documents; the document probabilities are computed in closed form, see below), `tg-sense` (Topic Grouper with word senses, sense splitting solver with the default minimum lift 2, left-to-right estimate; only for vocabularies up to `Settings.senseMaxWords` words), `lda` / `lda-avg` (LDA, left-to-right and averaging estimate), `lda-opt` / `lda-opt-avg` (LDA with re-estimated alpha and beta), `plsa` (small corpora), `unigram` |
| `twc-alpha.csv` | `alpha1;method;perplexity;perplexityStdDev;errorRate;errorRateStdDev;tries` | synthetic corpus at 4 topics over the prior alpha_1 given to LDA (concentration 6.5, beta 4 re-estimated, `tries` runs); the references `tg` (concentration 6.5), `plsa`, `unigram` and `perfect` (the true topics) are constant and repeated for every alpha_1 |
| `twc-docs.csv` | `docs;method;perplexity;perplexityStdDev;errorRate;errorRateStdDev;tries` | synthetic corpus at 4 topics over the number of documents (LDA with the true prior) |
| `performance-<corpus>.csv` | `solver;docs;avgSeconds;stdDevSeconds;avgWords` | run time of the solvers `ehac` and `lowmem` (single threaded) and `ehac-mt`, `lowmem-mt` (one thread per processor) over the number of documents (random subsets, several tries) |
| `topics-<corpus>.csv`, `<corpus>.mm`, `<corpus>.json.gz`, `figs/hierarchy-<corpus>.svg` | `topic;size;rank;word;frequency` | the ten most frequent words of every topic at the example number of topics (40), and the whole hierarchy as a FreeMind mind map, as a JSON file for the hierarchy browser (`org.hhn.topicgrouper.tg.viz.HierarchyBrowser` in `api`) and as an SVG drawing down to the example number of topics; `reuters-sense.*` (plus `reuters-sense-full.svg`, the whole hierarchy) is the same for the sense splitting solver on the words occurring at least 5 times (its memory is quadratic in the vocabulary) |
| `classification-<corpus>.csv` | `method;topics;microAvg;macroAvg;lambda` | accuracy of naive Bayes on topics from Topic Grouper (`tg-nb`, `tg-nb-smoothed`), tf-idf on topics (`tg-tfidf`), naive Bayes on LDA topics (`lda-nb`, `lda-nb-opt`) and on selected words (`vocab-ig`, `vocab-df`), over the number of topics / words |
| `homonyms.csv` | `homonyms;method;perplexity;errorRate;splits` | synthetic corpus with an increasing number of homonyms of 2 to 4 senses (almost single-topic documents), 4 topics: plain Topic Grouper (`tg`), Topic Grouper with word senses (`tg-sense`, see `doc/topic-grouper.md`, Section 6), LDA with fixed alpha (`lda`) and with an optimized alpha vector (`lda-alpha`); `errorRate` is measured against the true topic-word distributions, `splits` is the number of senses created |

## Figures

Every experiment draws its figures as SVG files into `results/figs/` when it has written a CSV file
(`Figures`, see the table below for the names); `scripts/draw-figures.sh [results]` redraws them from the
CSV files, e.g. after merging the results of several machines. The documents `doc/topic-grouper.md` and
`doc/feature-reduction.md` reference the figures as `figs/<name>.svg`, so copying `results/figs/` to
`doc/figs/` updates them.

| CSV file | Figures in `figs/` |
|---|---|
| `perplexity-<corpus>.csv` | `perplexity-<corpus>.svg` |
| `twc-alpha.csv`, `twc-docs.csv` | `twc-alpha-perplexity.svg`, `twc-alpha-error.svg`, `twc-docs-perplexity.svg`, `twc-docs-error.svg` |
| `performance-<corpus>.csv` | `performance-<corpus>.svg` |
| `classification-<corpus>.csv` | `classification-<corpus>-micro.svg`, `classification-<corpus>-macro.svg` |
| `homonyms.csv` | `homonyms-error.svg`, `homonyms-perplexity.svg` |
| (topics experiment) | `hierarchy-<corpus>.svg`, the hierarchy down to the example number of topics |

## Notes and deviations from the original code

* The original experiment programs (one class per corpus and method, written against LingPipe 3.9.3
  and weka) were merged into the parametrized experiments of this module; the corpora, splits, seeds
  and model parameters are those of the papers (see the Javadoc of the experiment classes and
  `Settings`). LingPipe 4.1.2 is used for tokenizing and stemming (the `PunctuationStopListTokenizer`
  of 3.9.3 no longer exists; the word filters of the parsers remove punctuation tokens anyway),
  Reuters-21578 is read by an own SGML reader (the ten largest topics, one copy per topic) and the
  OHSUMED ARFF file by an own reader instead of weka.
* The original error-rate experiments of the first generation (`TWCPerplexityErrorRate*`,
  `APExtractPerplexity*`) were not functional (the accuracy calculator was fed inconsistent models) and
  were dropped; the series of `twc-series` are the working ones of the paper.
* Timing uses `System.nanoTime` in fractional seconds (the original truncated to whole seconds) and
  stops when the corpus is exhausted instead of averaging in zeros.
* The LDA fold-in counts, the topic grids, the 10 refinement steps of the search for the Dirichlet
  concentration of Topic Grouper and the 20 particles of the left-to-right estimator are the ones of
  the papers in `Settings.full()`. Only the LDA models of the perplexity, `twc-series` and `homonyms`
  experiments run 500 burn-in and 200 sampling iterations instead of the papers' 2000 and 1000, since
  the LDA training dominates the run time on the text corpora (`--lda-burn-in`, `--lda-iterations`
  restore any other value); the LDA models of the different topic numbers are trained in parallel
  (`--threads`). The left-to-right estimator is quadratic in the document length and linear in the
  particles and the topics a word can belong to; for LDA it is the second large cost.
* The LDA classifiers of `classification` are evaluated up to 1000 topics (the Topic Grouper and word
  series up to 9000): the Gibbs sampler is linear in the topics and needs about 5 hours for a model of
  1000 topics on Reuters-21578 (2000 iterations), 15 hours at 2000, while the accuracy is flat from a few
  hundred topics on. These models are trained in parallel too.
* The perplexity of Topic Grouper solutions is computed in closed form (`TGExactPerplexityCalculator`):
  every word has exactly one topic, so the resampling step of the papers' left-to-right estimator is a
  point mass and the estimate is exact, the product of the topic-word probabilities times the
  Dirichlet-multinomial likelihood of the document's topic counts. The numbers equal those of the
  left-to-right sampler up to rounding (checked by a unit test and on NIPS at 200 topics), at a cost
  linear instead of quadratic in the document length. Solutions with word senses (`tg-sense`) still
  need the sampler, which now only considers the topics in which a word has a non-zero probability
  (one or a few for Topic Grouper models instead of all; the samples have the same distribution). The `tg-smoothed` series of earlier versions
  (p(t|d) interpolated with a fixed weight) is gone: without a reference document it measured the
  unigram model, and it was never part of the papers.
* A review of the library against the standard formulations fixed several bugs that change numbers compared
  with the original experiment code: the averaging perplexity estimator normalizes p(t|d) with
  the model's word marginal, the naive Bayes classifier normalizes P(t|c) over the topic words of a class (it
  divided by all words), PLSA uses the standard M-step for p(z|d), and the Dirichlet(1) sampler of the synthetic
  corpus generator draws uniformly from the simplex (it normalized uniform variates), and the left-to-right
  calculator for Topic Grouper solutions (`TGLRPerplexityCalculator`) now honours the document splitter it is
  given (it always evaluated whole documents, which is what the papers' experiments used anyway).
  LDA's topic-word estimate is the maximum likelihood estimate `n_kw / n_k` averaged over the sampling
  iterations, as in the papers' experiments (`LDAGibbsSampler.setSmoothedPhi(true)` selects the smoothed
  `(n_kw + beta) / (n_k + V beta)` instead). The synthetic corpora
  therefore differ from those of the paper's runs; the parameters are the same.
