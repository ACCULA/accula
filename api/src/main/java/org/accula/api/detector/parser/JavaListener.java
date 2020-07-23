package org.accula.api.detector.parser;

import generated.Java9BaseListener;
import generated.Java9Parser;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class JavaListener extends Java9BaseListener {
    private final CommonTokenStream tokenStream;
    private final List<List<Token>> functions = new LinkedList<>();
    private final Set<Token> typeArgs = new HashSet<>();

    public JavaListener(final CommonTokenStream tokens) {
        tokenStream = tokens;
    }

    public Stream<List<Token>> getFunctions() {
        return functions.stream();
    }

    public Set<Token> getTypeArgs() {
        return typeArgs;
    }

    @Override
    public void enterMethodBody(final Java9Parser.MethodBodyContext ctx) {
        final var tokens = new LinkedList<Token>();
        var tok = ctx.getStart();
        var idx = tok.getTokenIndex();
        while (tok != ctx.getStop()) {
            tok = tokenStream.get(idx++);
            tokens.add(tok);
        }
        functions.add(tokens);
    }

    @Override
    public void enterClassOrInterfaceType(final Java9Parser.ClassOrInterfaceTypeContext ctx) {
        for (int i = ctx.getStart().getTokenIndex() - 1; i <= ctx.getStop().getTokenIndex() + 1; i++) {
            typeArgs.add(tokenStream.get(i));
        }
    }
}
