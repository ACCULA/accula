package org.accula.api.token.kotlin.psi;

import org.accula.api.code.lines.LineRange;
import org.accula.api.util.Checks;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiRecursiveElementVisitor;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet;
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens;
import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

//FIXME: get rid of duplication by replacing kotlin-compiler-embeddable with kotlin-compiler
// and reuse code around psi once kotlin-compiler developers are stopped including unshadowed libraries into jar
/**
 * @author Anton Lamtev
 */
public final class KotlinPsiUtils {
    private static final TokenSet OPTIONAL_TOKENS = TokenSet.orSet(
        TokenSet.create(
            KtTokens.FINAL_KEYWORD,
            KtTokens.OPEN_KEYWORD,
            KDocElementTypes.KDOC_SECTION,
            KDocElementTypes.KDOC_TAG,
            KDocElementTypes.KDOC_NAME
        ),
        KDocTokens.KDOC_HIGHLIGHT_TOKENS
    );
    private static final TokenSet TOKENS_TO_EXCLUDE = TokenSet.orSet(
        OPTIONAL_TOKENS,
        KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET,
        TokenSet.create(
            KtTokens.LBRACE,
            KtTokens.RBRACE,
            KtTokens.SEMICOLON,
            KtTokens.DOUBLE_SEMICOLON,
            KtTokens.LPAR,
            KtTokens.RPAR
        )
    );
    private static final TokenSet LITERALS = TokenSet.create(
        KtTokens.TRUE_KEYWORD,
        KtTokens.FALSE_KEYWORD,
        KtTokens.CHARACTER_LITERAL,
        KtTokens.INTEGER_LITERAL,
        KtTokens.FLOAT_LITERAL,
        KtTokens.REGULAR_STRING_PART,
        KtTokens.NULL_KEYWORD
    );

    private KotlinPsiUtils() {
    }

    public static List<KtNamedFunction> methods(final PsiElement root) {
        return methods(root, null);
    }

    public static List<KtNamedFunction> methods(final PsiElement root, @Nullable final Predicate<LineRange> lineRangeFilter) {
        final var methods = new ArrayList<KtNamedFunction>();
        forEachDescendantOfType(root, KtNamedFunction.class, method -> {
            if (method.hasBody() && matchesLineRangeFilter(method, lineRangeFilter)) {
                methods.add(method);
            }
        });
        return methods;
    }

    public static LineRange lineRange(final PsiElement element) {
        final var file = element.getContainingFile();
        final var doc = Checks.notNull(file.getViewProvider().getDocument(), "Document");
        final var relativeRange = element.getTextRange();
        return LineRange.of(
            doc.getLineNumber(relativeRange.getStartOffset()) + 1,
            doc.getLineNumber(relativeRange.getEndOffset()) + 1
        );
    }

    public static boolean isValuableToken(final PsiElement token) {
        return !TOKENS_TO_EXCLUDE.contains(token.getNode().getElementType());
    }

    public static String optimizeTokenString(final IElementType token) {
        if (LITERALS.contains(token)) {
            return "_LITERAL";
        }
        return token.toString();
    }

    private static boolean matchesLineRangeFilter(final KtNamedFunction method, @Nullable final Predicate<LineRange> lineRangeFilter) {
        return lineRangeFilter == null || lineRangeFilter.test(lineRange(method));
    }

    private static <T extends PsiElement> void forEachDescendantOfType(final PsiElement root,
                                                                       final Class<T> type,
                                                                       final Consumer<? super T> onEach) {
        root.accept(new PsiRecursiveElementVisitor() {
            @SuppressWarnings("unchecked")
            @Override
            public void visitElement(final PsiElement element) {
                super.visitElement(element);
                if (type.isAssignableFrom(element.getClass())) {
                    onEach.accept((T) element);
                }
            }
        });
    }
}
