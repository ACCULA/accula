package org.accula.suffixtree.util;

import lombok.Value;

import java.util.List;

@Value
public class TokenizedMethod {

    long sequenceId;
    List<Token> tokenizedContent;

    public int getRangeBetweenTokens(Token from, Token to) {
        return tokenizedContent.indexOf(to) - tokenizedContent.indexOf(from) + 1;
    }
}
