# Report №1

### Code downloading

Two approaches to loading code have been investigated:

* [JGit](https://github.com/eclipse/jgit) 
* GitHub raw (e.g. https://raw.githubusercontent.com/ACCULA/accula/develop/README.md).

__JGit__ 
(from [dev branch implementation](https://github.com/ACCULA/accula/blob/develop/api/src/main/java/org/accula/api/code/JGitCodeLoader.java))

   Pros:
   
   * Fast repo downloading
   * Git features
   
   Cons:
   
   * Unnecessary data
   * Problem with deleted users / branches
   
__GitHub raw__

   Pros:
   
   * Downloading files from pull requests only (no problem with deleted branches?)
   * Filtering files before downloading
   
   Cons:
   
   * Poor performance (new request for each file, no compression)
   
   Tested on [2020-db-lsm](https://github.com/polis-mail-ru/2020-db-lsm):
   
   ![image](https://user-images.githubusercontent.com/26203645/88912401-e1cd1600-d267-11ea-9f9c-a42e4ab32340.png)
   
   :exclamation: 
   This graph is not very accurate.
   During testing, the JGit-based loader threw exceptions on some commits 
   and loaded different numbers of files for the same number of pull requests.
   :exclamation:
   
   Since both code loaders use GitHub Api v3, they have limitations:
   * JGit - 5000 rph / without pagination load only 
   [last 100](https://github.com/ACCULA/accula/blob/develop/api/src/main/java/org/accula/api/github/api/GithubClientImpl.java#L97) 
   pull requests.
   * GitHub raw - 5000 rph / without pagination load only last 100 files from last 100 pull requests (max 10_000 files).
   
### Clone detection
   
Three approaches to clone detection have been investigated:

 * [Primitive clone detector](https://github.com/ACCULA/accula/blob/develop/api/src/main/java/org/accula/api/detector/PrimitiveCloneDetector.java) -
 text-based clone detector based on ConcurrentHashMap.
 * [Current detector](https://github.com/ACCULA/accula/blob/develop/api/src/main/java/org/accula/api/detector/SuffixTreeCloneDetector.java) -
 token-based clone detector based on [Idea Clone Plugin](https://github.com/suhininalex/IdeaClonePlugin).
 * Suffix tree detector (research implementation) -
 token-based clone detector based on [Suffix Tree](https://github.com/suhininalex/SmallSuffixTree).
 
 Tested on [2020-db-lsm](https://github.com/polis-mail-ru/2020-db-lsm):
 
 ![image](https://user-images.githubusercontent.com/26203645/88917451-72a7ef80-d270-11ea-97b7-db67b2030c0f.png)

 :trollface:
    This graph is not very accurate too.
    During testing, all detectors show a different number of clones for the same number of pull requests / files on each run.
 :trollface:
 
 ### Future work
 
 __Parser__
 
Compare the performance of preprocessing with other parsers:
 * [JavaParser](https://github.com/javaparser/javaparser) - Java 14 support
 * [Antlr fork](https://github.com/tunnelvisionlabs/antlr4) - optimized version (?)
 
 __Clone Detector__
 
An inverted index looks like a more appropriate data structure for the current approach.
Implement the detector based on a self-written index / lucene / elasticsearch / solr / etc with:
 * incremental update ?
 * persistent storage ?
 * concurrent build ?
 
Run [benchmark](https://github.com/jeffsvajlenko/BigCloneEval).
 