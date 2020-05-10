package org.accula.analyzer.checkers;

import org.accula.analyzer.checkers.utils.ClonePair;
import org.antlr.v4.runtime.Token;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SuffixTreeCloneChecker implements CloneChecker<Flux<Token>, ClonePair> {
    @Override
    public Mono<ClonePair> checkClones(final Flux<Token> file1,
                                       final Flux<Token> file2,
                                       final ClonePair cloneInfo) {
//        TODO: implement Suffix Tree, compute metric

        cloneInfo.incCounter();
        cloneInfo.setMetric(1.0f);
        return Mono.just(cloneInfo);
    }
}
