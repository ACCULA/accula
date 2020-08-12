package org.accula.parser;

import com.github.javaparser.JavaToken;

import java.util.Set;

public class TokenFilter {
    public static final Set<Integer> EXCLUDE_TOKENS = Set.of(
            JavaToken.Kind.EOF.getKind(),
            JavaToken.Kind.COMMENT_CONTENT.getKind(),
            JavaToken.Kind.MULTI_LINE_COMMENT.getKind(),
            JavaToken.Kind.SINGLE_LINE_COMMENT.getKind(),
            JavaToken.Kind.JAVADOC_COMMENT.getKind(),
            JavaToken.Kind.FINAL.getKind(),
            JavaToken.Kind.LPAREN.getKind(),
            JavaToken.Kind.RPAREN.getKind(),
            JavaToken.Kind.LBRACE.getKind(),
            JavaToken.Kind.RBRACE.getKind(),
            JavaToken.Kind.LBRACKET.getKind(),
            JavaToken.Kind.RBRACKET.getKind(),
            JavaToken.Kind.SEMICOLON.getKind(),
            JavaToken.Kind.COMMA.getKind(),
            JavaToken.Kind.SPACE.getKind(),
            JavaToken.Kind.WINDOWS_EOL.getKind(),
            JavaToken.Kind.UNIX_EOL.getKind(),
            JavaToken.Kind.OLD_MAC_EOL.getKind()
    );

    public static final Set<Integer> PRIMITIVE_TYPES = Set.of(
            JavaToken.Kind.INT.getKind(),
            JavaToken.Kind.SHORT.getKind(),
            JavaToken.Kind.LONG.getKind(),
            JavaToken.Kind.BYTE.getKind(),
            JavaToken.Kind.FLOAT.getKind(),
            JavaToken.Kind.DOUBLE.getKind(),
            JavaToken.Kind.CHAR.getKind(),
            JavaToken.Kind.BOOLEAN.getKind()
    );
}
