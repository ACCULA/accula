package org.accula;

import org.accula.analyzer.current.CurrentDetector;
import org.accula.data.github.GitHubApiClient;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.TimeUnit;

public class RunCurrentDetector {
    public static void main(String[] args) {
        final String SOURCE = "https://github.com/ACCULA/accula";
        final String TOKEN = "YOUR_TOKEN";
        final var dataProvider = new GitHubApiClient(SOURCE, TOKEN);
        final var detector = new CurrentDetector(15);
        final var start = System.nanoTime();
        final var clones = detector
                .findClones(dataProvider.getFiles().take(1), dataProvider.getFiles().skip(1))
                .subscribeOn(Schedulers.newSingle("CD"))
                .doFinally(endType -> System.err.println(
                        "Time taken : " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + " ms."
                ))
                .publish()
                .autoConnect(2);

        clones.count().subscribe(x -> System.err.println("Found " + x + " clones!"));

        clones.subscribe(System.err::println);
    }
}
