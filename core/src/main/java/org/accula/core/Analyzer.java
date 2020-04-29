package org.accula.core;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.core.checkers.CloneChecker;
import org.accula.core.checkers.CloneCheckerImpl;
import org.accula.core.checkers.structures.CloneInfo;
import org.accula.data.model.GFile;
import org.accula.data.provider.GitHubClient;
import org.accula.data.provider.filter.GitFileFilter;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
@RequiredArgsConstructor
public class Analyzer {
    @NotNull
    private final String source;
    @NotNull
    private final String token;
    @NotNull
    private final List<String> include;
    @NotNull
    private final List<String> exclude;

    private float THRESHOLD = 0.95f;

    private final List<CloneChecker<ParseTree, CloneInfo>> checkers = List.of(
            new CloneCheckerImpl()
    );

    private final List<GFile<ParseTree>> processedFiles = new ArrayList<>();

    public void analyze() {
        var fileFilter = GitFileFilter.builder()
                .includeFilter(include)
                .excludeFilter(exclude)
                .build();
        var dataProvider = new GitHubClient(source, token);
        dataProvider.fetchPullRequests()
                .subscribeOn(Schedulers.single())
                .concatMap(pr -> dataProvider.fetchRepoContent(pr.getNumber(), pr.getUserName(), fileFilter))
                .map(files -> DataTransformer.transformContent(files, ParserUtils::getAST))
                .map(file -> processFile(file).subscribeOn(Schedulers.single()).subscribe())
                .doOnError(e -> log.error("Something went wrong: {}", e.getMessage()))
                .subscribe();
    }

    private Mono<Void> processFile(@NotNull final GFile<ParseTree> file) {
        return Mono.fromRunnable(() -> {
            detectClones(file);
            processedFiles.add(file);
        });
    }

    private void detectClones(@NotNull final GFile<ParseTree> file1) {
        System.err.printf("****** PROCESSED: %d, NOW: %s ********%n", processedFiles.size(), file1.fileName());
        checkers.forEach(cloneChecker ->
                Flux.fromIterable(processedFiles)
                        .filter(f -> !f.userName().equals(file1.userName()))
                        .doOnNext(file -> System.err.printf("%s vs %s: %s%n",
                                String.format("%s : %s", file.userName(), file.fileName()),
                                String.format("%s : %s", file1.userName(), file1.fileName()),
                                cloneChecker.checkClones(file1.content(), file.content(), THRESHOLD).getNormalizedMetric()))
                        .subscribe());
    }
}
