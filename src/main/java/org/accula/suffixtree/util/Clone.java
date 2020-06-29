package org.accula.suffixtree.util;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Clone extends ReferenceClone{

    private final Token fromToken;
    private final Token toToken;
    private int cloneLength;

    public Clone(String name, String owner, Token fromToken, Token toToken, int cloneLength) {
        super(name, owner, fromToken.getLine(), toToken.getLine());
        this.fromToken = fromToken;
        this.toToken = toToken;
        this.cloneLength = cloneLength;
    }

    public Clone setCloneLength(int cloneLength) {
        this.cloneLength = cloneLength;
        return this;
    }
}
