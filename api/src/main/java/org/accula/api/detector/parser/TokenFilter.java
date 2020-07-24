package org.accula.api.detector.parser;

import generated.Java9Lexer;

import java.util.Set;

final class TokenFilter {
    static final Set<Integer> EXCLUDE_TOKENS = Set.of(
            Java9Lexer.LPAREN,
            Java9Lexer.RPAREN,
            Java9Lexer.LBRACE,
            Java9Lexer.RBRACE,
            Java9Lexer.LBRACK,
            Java9Lexer.RBRACK,
            Java9Lexer.SEMI,
            Java9Lexer.WS,
            Java9Lexer.COMMENT,
            Java9Lexer.LINE_COMMENT,
            Java9Lexer.FINAL
    );

    static final Set<Integer> PRIMITIVE_TYPES = Set.of(
            Java9Lexer.INT,
            Java9Lexer.SHORT,
            Java9Lexer.LONG,
            Java9Lexer.BYTE,
            Java9Lexer.CHAR,
            Java9Lexer.BOOLEAN,
            Java9Lexer.FLOAT,
            Java9Lexer.DOUBLE
    );

    private TokenFilter() {
    }
}
