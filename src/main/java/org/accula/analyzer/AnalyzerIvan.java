package org.accula.analyzer;

import com.suhininalex.clones.core.CloneIndexer;
import com.suhininalex.clones.core.structures.Token;
import com.suhininalex.clones.core.structures.TreeCloneClass;
import com.suhininalex.suffixtree.EndToken;
import com.suhininalex.suffixtree.SuffixTree;
import org.accula.parser.Parser;
import org.accula.suffixtree.util.Clone;
import org.accula.suffixtree.util.ReferenceClone;
import org.accula.suffixtree.util.TokenizedFile;
import org.accula.suffixtree.util.TokenizedMethod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class AnalyzerIvan {

    public static void main(String[] args) throws IOException {

        CloneIndexer cloneFinder = CloneIndexer.INSTANCE;
        SuffixTree<Token> suffixTree = cloneFinder.getTree();
        List<TokenizedFile> tokenizedFiles = new ArrayList<>();
        Map<ReferenceClone, List<Clone>> clones = new HashMap<>();
        int minCloneLength = 3;

        DataProvider.getFiles("testData", "java")
                .forEach(file -> {
                    ArrayList<TokenizedMethod> methods = new ArrayList<>();
                    List<List<Token>> treeInputTokenizedMethod = Parser
                            .getFunctionsAsTokens(file)
                            .collect(Collectors.toList());
                    treeInputTokenizedMethod.forEach(tokens -> {
                        methods.add(new TokenizedMethod(suffixTree.addSequence(tokens),
                                                        convertToOutputTokens(tokens)));
                    });
                    tokenizedFiles.add(new TokenizedFile(file.getName(), file.getOwner(), methods));
                });

        tokenizedFiles.forEach(tokenizedFile -> {
            findClones(tokenizedFile, clones, cloneFinder, minCloneLength);
        });

        clones.keySet().forEach(referenceClone -> {
            List<Clone> clones1 = clones.get(referenceClone);
            System.out.println("Key: " + referenceClone + " value: ");
            clones1.forEach(clone -> {
                printCloneInfo(clone.getFromToken(), clone.getToToken());
            });
            System.out.println("\t ________________________________ \t");
        });

        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        for (int i = 2; i < 20; i++) { //22 for all
            cloneFinder.getAllSequenceCloneClasses(i, minCloneLength)
                    .stream()
                    .findFirst()
                    .ifPresent(treeCloneClass -> {
                        for (int j = 0; j <= treeCloneClass.getSize(); j++) {
                            Token begin = treeCloneClass.getClones().iterator().next().getFirstElement();
                            Token end = treeCloneClass.getClones().iterator().next().getLastElement();
                            printCloneInfo(begin, end);
                        }
                    });
        }
    }

    private static void findClones(TokenizedFile file, Map<ReferenceClone, List<Clone>> clones,
                                   CloneIndexer cloneFinder, int minCloneLength) {

        file.getMethods().forEach(tokenizedMethod -> {
            cloneFinder.getAllSequenceCloneClasses(tokenizedMethod.getSequenceId(), minCloneLength)
                    .stream()
                    .findFirst()
                    .ifPresent(treeCloneClass -> {
//                        treeCloneClass.getTreeNode().getEdges().forEach(edge ->
//                                System.out.println(
//                                        edge.getSequence().get(edge.getBegin()) instanceof EndToken e
//                                                ? e.getIdSequence()
//                                                : "pff" )); //TODO extract end token, calculate begin token by substracting the clone length

                        org.accula.suffixtree.util.Token fromToken = copyToken(extractBeginToken(treeCloneClass));
                        org.accula.suffixtree.util.Token toToken = copyToken(extractEndToken(treeCloneClass));

                        int cloneLength = treeCloneClass.getLength();

                        ReferenceClone referenceClone = new ReferenceClone(fromToken.getFilename(),
                                fromToken.getOwner(),
                                fromToken.getLine(),
                                toToken.getLine());

                        //if (!clones.containsKey(referenceClone)) {
                        treeCloneClass.getTreeNode().getEdges()
                                .stream()
                                .filter(edge -> {
                                    int lastElementIndex = edge.getSequence().size() - 1;
                                    final Object element = edge.getSequence().get(lastElementIndex);
                                    if (element instanceof EndToken endToken) {
                                        return endToken.getIdSequence() == tokenizedMethod.getSequenceId();
                                    } else {
                                        return false;
                                    }
                                })
                                .forEach(edge -> {
                                    int hahActuallyEnd = edge.getBegin();
                                    Token end = (Token) edge.getSequence().get(hahActuallyEnd - 1);
                                    Token begin = (Token) edge.getSequence().get(hahActuallyEnd - cloneLength);
                                    Clone clone = new Clone(end.getFilename(),
                                            end.getOwner(), copyToken(begin),
                                            copyToken(end), cloneLength);
                                    clones.computeIfAbsent(referenceClone, __ -> new ArrayList<>()).add(clone);
                                });
                        //}

//                        Clone clone = new Clone(fromToken.getFilename(),
//                                fromToken.getOwner(),
//                                fromToken,
//                                toToken,
//                                cloneLength);
//
//                        try {
//                            clones.computeIfAbsent(referenceClone, k -> new ArrayList<>())
//                                    .add( (file.getName().equalsIgnoreCase(fromToken.getFilename()) && //TODO check for clone 2nd appearance in same file (see Main.java 13-16 18-21)
//                                            file.getOwner().equalsIgnoreCase(fromToken.getOwner()))
//                                            ? clone
//                                            : mapToMethod(clone.setCloneLength(cloneLength), //clones.get(referenceClone).get(0).getCloneLength()),
//                                            tokenizedMethod,
//                                            file.getName(),
//                                            file.getOwner()));
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
                    });
        });
    }

    private static List<org.accula.suffixtree.util.Token> convertToOutputTokens(List<Token> from) {
        List<org.accula.suffixtree.util.Token> to = new ArrayList<>(from.size());
        from.forEach(token -> to.add(copyToken(token)));
        return to;
    }

    private static org.accula.suffixtree.util.Token copyToken(Token from) {
        return org.accula.suffixtree.util.Token.of(
                from.getType(),
                from.getText(),
                from.getLine(),
                from.getFilename(),
                from.getOwner(),
                from.getPath());
    }

    public static void printCloneInfo(org.accula.suffixtree.util.Token begin, org.accula.suffixtree.util.Token end) {
        System.out.print("Begin: " + begin + " | line: " + begin.getLine() + " | file: " + begin.getFilename() + "\t");
        System.out.println(" || End: " + end + " | line: " + end.getLine() + " | file: " + end.getFilename());
    }

    public static void printCloneInfo(Token begin, Token end) {
        System.out.print("Begin: " + begin + " | line: " + begin.getLine() + " | file: " + begin.getFilename() + "\t");
        System.out.println(" || End: " + end + " | line: " + end.getLine() + " | file: " + end.getFilename());
    }

    private static Token extractBeginToken(TreeCloneClass treeCloneClass) {
        return treeCloneClass.getClones().iterator().next().getFirstElement();
    }

    private static Token extractEndToken(TreeCloneClass treeCloneClass) {
        return treeCloneClass.getClones().iterator().next().getLastElement();
    }
}