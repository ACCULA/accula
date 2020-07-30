package org.accula.api.detector.parser;

import generated.Java9BaseListener;
import generated.Java9Parser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@RequiredArgsConstructor
final class JavaListener extends Java9BaseListener {
    private final CommonTokenStream tokenStream;
    private final List<List<Token>> functionsList = new ArrayList<>();
    @Getter
    private final Set<Token> typeArgs = new HashSet<>();

    Stream<List<Token>> functions() {
        return functionsList.stream();
    }

    @Override
    public void enterMethodBody(final Java9Parser.MethodBodyContext ctx) {
        final var tokens = new ArrayList<Token>();
        var tok = ctx.getStart();
        var idx = tok.getTokenIndex();
        while (tok != ctx.getStop()) {
            tok = tokenStream.get(idx++);
            tokens.add(tok);
        }
        functionsList.add(tokens);
    }

    @Override
    public void enterClassOrInterfaceType(final Java9Parser.ClassOrInterfaceTypeContext ctx) {
        for (int i = ctx.getStart().getTokenIndex() - 1; i <= ctx.getStop().getTokenIndex() + 1; i++) {
            typeArgs.add(tokenStream.get(i));
        }
    }
}
