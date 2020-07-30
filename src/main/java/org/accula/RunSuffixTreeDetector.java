package org.accula;

import org.accula.analyzer.research.SuffixTreeDetector;
import org.accula.data.github.GitHubApiClient;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.TimeUnit;

public class RunSuffixTreeDetector {
    public static void main(String[] args) {
        final String SOURCE = "https://github.com/polis-mail-ru/2020-db-lsm";
        final String TOKEN = "YOUR_TOKEN";
        final var dataProvider = new GitHubApiClient(SOURCE, TOKEN);
        final var detector = new SuffixTreeDetector(5);
        final var start = System.nanoTime();
        final var clones = detector
                .findClones(dataProvider.getFiles())
                .subscribeOn(Schedulers.newSingle("STD"))
                .doFinally(endType -> System.err.println(
                        "Time taken : " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + " ms."
                ))
                .publish()
                .autoConnect(2);

        clones.count().subscribe(x -> System.err.println("Found " + x + " clones!"));

        clones.subscribe();
    }
}
