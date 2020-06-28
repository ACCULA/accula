package org.accula.postprocessing;

import com.suhininalex.clones.core.structures.Token;
import lombok.Value;

import java.util.List;

@Value
public class TokenizedMethod {

    long sequenceId;
    List<Token> tokenizedContent;
}
