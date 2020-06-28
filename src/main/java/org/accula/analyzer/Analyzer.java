package org.accula.analyzer;

import com.suhininalex.clones.core.CloneIndexer;
import com.suhininalex.clones.core.structures.Token;
import com.suhininalex.clones.core.structures.TreeCloneClass;
import com.suhininalex.suffixtree.SuffixTree;
import org.accula.parser.Parser;
import org.accula.suffixtree.util.Clone;
import org.accula.suffixtree.util.ReferenceClone;
import org.accula.suffixtree.util.TokenizedFile;
import org.accula.suffixtree.util.TokenizedMethod;

import java.io.IOException;
import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.accula.suffixtree.postprocessing.CloneToFileMapper.mapToMethod;


public class Analyzer {
    public static void main(String[] args) throws IOException {

        CloneIndexer cloneFinder = CloneIndexer.INSTANCE;
        SuffixTree<Token> suffixTree = cloneFinder.getTree();
        List<TokenizedFile> tokenizedFiles = new ArrayList<>();
        Map<ReferenceClone, List<Clone>> clones = new HashMap<>();
        int minCloneLength = 10;

        DataProvider.getFiles("testData", "java")
                .forEach(file -> {
                    ArrayList<TokenizedMethod> methods = new ArrayList<>();
                    List<List<Token>> tokenizedMethods = Parser.getFunctionsAsTokens(file);
                    tokenizedMethods.forEach(tokens -> {
                        methods.add(new TokenizedMethod(suffixTree.addSequence(tokens), tokens));
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


        final int sequenceId = 3;
        //tokenizedFiles.forEach(System.out::println);

//        CloneIndexer.INSTANCE.getTree().getSequence(sequenceId).forEach(token ->
//                System.out.println(token + ":" +
//                        token.getType() + ":" +
//                        Java9Lexer.VOCABULARY.getSymbolicName(token.getType()))
//        );

        //System.out.println(suffixTree.getSequence(sequenceId));

        for (int i = 2; i < 15; i++) {
            cloneFinder.getAllSequenceCloneClasses(i, minCloneLength).forEach(treeCloneClass -> {
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
            cloneFinder.getAllSequenceCloneClasses(tokenizedMethod.getSequenceId(), minCloneLength).forEach(
                    treeCloneClass -> {
                        Token fromToken = extractBeginToken(treeCloneClass);
                        Token toToken = extractEndToken(treeCloneClass);
                        //TODO add check for general token info equality (owner, file)
                        ReferenceClone referenceClone = new ReferenceClone(fromToken.getFilename(),
                                                         fromToken.getOwner(),
                                                         fromToken.getLine(),
                                                         toToken.getLine());
                        Clone clone = new Clone(fromToken.getFilename(),
                                fromToken.getOwner(),
                                fromToken,
                                toToken);
                        try {
                            clones.computeIfAbsent(referenceClone, k -> new ArrayList<>()) //TODO add mapping logic and
                                  .add( (file.getName().equalsIgnoreCase(fromToken.getFilename()) &&
                                          file.getOwner().equalsIgnoreCase(fromToken.getOwner()))
                                          ? clone
                                          : mapToMethod(clone, tokenizedMethod, file.getName(), file.getOwner()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        });
    }
}
