package org.accula.suffixtree.postprocessing;

import com.suhininalex.clones.core.structures.Token;
import lombok.Value;
import lombok.experimental.UtilityClass;
import org.accula.suffixtree.util.Clone;
import org.accula.suffixtree.util.TokenizedFile;
import org.accula.suffixtree.util.TokenizedMethod;

import java.util.List;

import static org.accula.analyzer.Analyzer.printCloneInfo;

public class CloneToFileMapper {

    private CloneToFileMapper() {}

    public static Clone mapToMethod(Clone sourceClone, TokenizedMethod method, String fileName, String owner) throws Exception {
        List<Token> tokens = method.getTokenizedContent();
        Token from = sourceClone.getFromToken();
        Token to  = sourceClone.getToToken();
        Token currentToken;
        Token endToken;

        for (int i = 0; i < tokens.size(); i++) {
            if (i + sourceClone.getCloneLenght() - 1 >= tokens.size())
                throw new Exception(tokens.toString());
                //throw new Exception("token size: " + tokens.size() + "cloneLength: " + sourceClone.getCloneLenght() + " - " + tokens.get(i) + " file: " + fileName + " line: " + tokens.get(i).getLine() + " for " + sourceClone);

            currentToken = tokens.get(i);
            if (currentToken.getText().equals(from.getText()) && currentToken.getType().equals(from.getType())) {
                endToken = tokens.get(i + sourceClone.getCloneLenght() - 1); //TODO BUG!!! getCloneLength() now counts lines, but must count tokens
                if (endToken.getType().equals(to.getType()) && endToken.getText().equals(to.getText()))
                    return new Clone(fileName, owner, currentToken, endToken);
            }
        }
        return null;
    }


    /**
     *  Standard equals() method for Token is defined to compare only by types.
     *  It is made so for suffix tree to work correct. Thus, this method is being
     *  defined to compare tokens by full list of attributes
     * @param t1 Token with general info
     * @param t2 Another token to compare
     * @return result of full attributes compare
     */
    private static boolean equalsTokens(Token t1, Token t2) {
        return t1.getOwner().equals(t2.getOwner());
    }
}
