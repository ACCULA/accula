package org.accula.suffixtree.util;

import com.suhininalex.clones.core.structures.Token;
import lombok.Value;

import java.util.List;

@Value
public class TokenizedMethod {

    long sequenceId;
    List<Token> tokenizedContent;
}
