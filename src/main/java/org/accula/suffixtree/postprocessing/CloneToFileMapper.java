package org.accula.suffixtree.postprocessing;

import org.accula.suffixtree.util.Clone;
import org.accula.suffixtree.util.Token;
import org.accula.suffixtree.util.TokenizedMethod;

import java.util.List;

public class CloneToFileMapper {

    private CloneToFileMapper() {}

    public static Clone mapToMethod(Clone sourceClone, TokenizedMethod method, String fileName, String owner) throws Exception {
        List<Token> tokens = method.getTokenizedContent();
        Token from = sourceClone.getFromToken();
        Token to  = sourceClone.getToToken();
        Token currentToken;
        Token endToken;

        for (int i = 0; i < tokens.size(); i++) {
            if (i + sourceClone.getCloneLength() > tokens.size())
                throw new Exception(tokens.toString());
                //throw new Exception("token size: " + tokens.size() + "cloneLength: " + sourceClone.getCloneLenght() + " - " + tokens.get(i) + " file: " + fileName + " line: " + tokens.get(i).getLine() + " for " + sourceClone);

            currentToken = tokens.get(i);
            if (currentToken.getType().equals(from.getType())) {
                endToken = tokens.get(i + sourceClone.getCloneLength() - 1);   //TODO BUG!!! getCloneLength() now counts lines, but must count tokens
                if (endToken.getType().equals(to.getType()))
                    return new Clone(fileName, owner, currentToken, endToken, method.getRangeBetweenTokens(currentToken, endToken));
            }
        }
        return null;
    }

}
