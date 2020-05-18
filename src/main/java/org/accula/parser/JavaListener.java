package org.accula.parser;

import generated.org.accula.parser.Java9BaseListener;
import generated.org.accula.parser.Java9Parser;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public class JavaListener extends Java9BaseListener {
    private final CommonTokenStream tokenStream;
    private final List<List<Token>> functions = new LinkedList<>();

    public JavaListener(@NotNull final CommonTokenStream tokens) {
        tokenStream =  tokens;
    }

    public List<List<Token>> getFunctions() {
        return functions;
    }

    @Override
    public void enterBlockStatements(Java9Parser.BlockStatementsContext ctx) {
        final var tokens = new LinkedList<Token>();
        var tok = ctx.getStart();
        var idx = tok.getTokenIndex();
        while(tok != ctx.getStop()) {
            tok = tokenStream.get(idx++);
            tokens.add(tok);
        }
        functions.add(tokens);
    }
}
