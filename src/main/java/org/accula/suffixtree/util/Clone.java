package org.accula.suffixtree.util;

import com.suhininalex.clones.core.structures.Token;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Clone extends ReferenceClone{

    private final Token fromToken;
    private final Token toToken;

    public Clone(String name, String owner, Token fromToken, Token toToken) {
        super(name, owner, fromToken.getLine(), toToken.getLine());
        this.fromToken = fromToken;
        this.toToken = toToken;
    }
}
