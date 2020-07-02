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

import static org.accula.suffixtree.postprocessing.CloneToFileMapper.mapToMethod;


public class AnalyzerIvan {

    public static void main(String[] args) throws IOException {

        CloneIndexer cloneFinder = CloneIndexer.INSTANCE;
        SuffixTree<Token> suffixTree = cloneFinder.getTree();
        List<TokenizedFile> tokenizedFiles = new ArrayList<>();
        Map<ReferenceClone, List<Clone>> clones = new HashMap<>();
        int minCloneLength = 10;

        DataProvider.getFiles("testData", "java")
                .forEach(file -> {
                    ArrayList<TokenizedMethod> methods = new ArrayList<>();
                    List<List<Token>> treeInputTokenizedMethod = Parser.getFunctionsAsTokens(file);
                    treeInputTokenizedMethod.forEach(tokens -> {
                        methods.add(new TokenizedMethod(suffixTree.addSequence(tokens), convertToOutputTokens(tokens)));
                    });
                    tokenizedFiles.add(new TokenizedFile(file.getName(), file.getOwner(), methods));
                });

        //System.out.println(suffixTree);

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


        final int sequenceId = 3;
        //tokenizedFiles.forEach(System.out::println);

//        CloneIndexer.INSTANCE.getTree().getSequence(sequenceId).forEach(token ->
//                System.out.println(token + ":" +
//                        token.getType() + ":" +
//                        Java9Lexer.VOCABULARY.getSymbolicName(token.getType()))
//        );

        //System.out.println(suffixTree.getSequence(sequenceId));
//
//        for (int i = 2; i < 4; i++) { //22 for all
//            cloneFinder.getAllSequenceCloneClasses(i, minCloneLength).forEach(treeCloneClass -> {
//                if (treeCloneClass.getSize() > 0) {
//                    Token begin = treeCloneClass.getClones().iterator().next().getFirstElement();
//                    Token end = treeCloneClass.getClones().iterator().next().getLastElement();
//                    printCloneInfo(begin, end);
//                }
//            });
//        }


        for (int i = 2; i < 20; i++) { //22 for all
            cloneFinder.getAllSequenceCloneClasses(i, minCloneLength).stream().findFirst().ifPresent(treeCloneClass -> {
                for (int j = 0; j <= treeCloneClass.getSize(); j++) {
                    Token begin = treeCloneClass.getClones().iterator().next().getFirstElement();
                    Token end = treeCloneClass.getClones().iterator().next().getLastElement();
                    printCloneInfo(begin, end);
                }
            });
        }

//        CloneIndexer.INSTANCE.getAllCloneClasses(5).forEach(treeCloneClass -> {
//            for (int j = 0; j <= treeCloneClass.getSize(); j++) {
//                Token begin = treeCloneClass.getClones().iterator().next().getFirstElement();
//                Token end = treeCloneClass.getClones().iterator().next().getLastElement();
//                System.out.print("Begin: " + begin + " | line: " + begin.getLine() + " | file: " + begin.getFilename() + "\t");
//                System.out.println(" || End: " + end + " | line: " + end.getLine() + " | file: " + end.getFilename());
//            }
//        });
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

    private static void findClones(TokenizedFile file, Map<ReferenceClone, List<Clone>> clones,
                                   CloneIndexer cloneFinder, int minCloneLength) {
        file.getMethods().forEach(tokenizedMethod -> {
            cloneFinder.getAllSequenceCloneClasses(tokenizedMethod.getSequenceId(), minCloneLength).stream().findFirst().ifPresent(treeCloneClass -> {

                treeCloneClass.getTreeNode().getEdges().forEach(edge ->
                        System.out.println(edge.getSequence().get(edge.getBegin()) instanceof EndToken e ? e.getIdSequence() : "pff" ));

                org.accula.suffixtree.util.Token fromToken = copyToken(extractBeginToken(treeCloneClass));
                org.accula.suffixtree.util.Token toToken = copyToken(extractEndToken(treeCloneClass));

                int cloneLength = treeCloneClass.getLength();
                ReferenceClone referenceClone = new ReferenceClone(fromToken.getFilename(),
                        fromToken.getOwner(),
                        fromToken.getLine(),
                        toToken.getLine());
                Clone clone = new Clone(fromToken.getFilename(),
                        fromToken.getOwner(),
                        fromToken,
                        toToken,
                        cloneLength);
                //tokenizedMethod.getRangeBetweenTokens(fromToken, toToken));
                try {
                    clones.computeIfAbsent(referenceClone, k -> new ArrayList<>())
                            .add( (file.getName().equalsIgnoreCase(fromToken.getFilename()) && //TODO check for clone 2nd appereance in same file (see Main.java 13-16 18-21)
                                    file.getOwner().equalsIgnoreCase(fromToken.getOwner()))
                                    ? clone
                                    : mapToMethod(clone.setCloneLength(cloneLength),//clones.get(referenceClone).get(0).getCloneLength()),
                                    tokenizedMethod,
                                    file.getName(),
                                    file.getOwner()));
                } catch (Exception e) {
                    e.printStackTrace();//System.out.println("->" + fromToken.getOwner() + fromToken.getFilename() + "||" + file+ " refClone " + referenceClone + " " + clones);
                }
            });
        });
    }
}