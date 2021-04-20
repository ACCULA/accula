package org.accula.api.code.git;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author Anton Lamtev
 */
public interface Git {
    CompletableFuture<Git.Repo> repo(Path directory);

    CompletableFuture<Git.Repo> clone(String url, String subdirectory);

    interface Repo {
        CompletableFuture<Repo> fetch();

        CompletableFuture<List<GitDiffEntry>> diff(String baseRef, String headRef, int findRenamesMinSimilarityIndex);

        <I extends Identifiable> CompletableFuture<Map<I, String>> catFiles(Iterable<I> identifiableObjects);

        CompletableFuture<List<GitFile>> show(String commitSha);

        CompletableFuture<Map<GitFile, String>> show(Iterable<String> commitsSha);

        CompletableFuture<List<GitFileChanges>> fileChanges(String commitSha);

        CompletableFuture<Map<GitFileChanges, String>> fileChanges(Iterable<String> commitsSha);

        CompletableFuture<List<GitFile>> lsTree(String commitSha);

        CompletableFuture<Set<String>> remote();

        CompletableFuture<GitBlockingImpl.Repo> remoteAdd(String url, String uniqueName);

        CompletableFuture<GitBlockingImpl.Repo> remoteUpdate(String name);

        CompletableFuture<List<GitCommit>> log(String ref);

        CompletableFuture<List<GitCommit>> log(String fromRefExclusive, String toRefInclusive);

        CompletableFuture<List<GitCommit>> revListAllPretty();
    }
}
