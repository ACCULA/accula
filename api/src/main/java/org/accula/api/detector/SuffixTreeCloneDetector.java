package org.accula.api.detector;

import com.suhininalex.clones.core.CloneIndexer;
import com.suhininalex.clones.core.structures.Token;
import com.suhininalex.clones.core.structures.TreeCloneClass;
import com.suhininalex.suffixtree.EndToken;
import com.suhininalex.suffixtree.SuffixTree;
import lombok.NonNull;
import lombok.Value;
import org.accula.api.code.FileEntity;
import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.detector.parser.Parser;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;
import java.util.stream.Stream;


public class SuffixTreeCloneDetector implements CloneDetector {

    private static final CloneIndexer CLONE_DETECTOR_INSTANCE = CloneIndexer.INSTANCE;

    private final int minCloneLength;

    /**
     * Creates an instance of the class with the specified minimal clone length
     * @param minCloneLength - determines the minimal Token sequence length to be considered a clone
     */
    public SuffixTreeCloneDetector(final int minCloneLength) {
        this.minCloneLength = minCloneLength;
    }

    @Override
    public Flux<Tuple2<CodeSnippet, CodeSnippet>> findClones(final Flux<FileEntity> targetFiles,
                                                             final Flux<FileEntity> sourceFiles) {
        return targetFiles.collectList()
                .zipWith(sourceFiles.collectList(), this::clones)
                .flatMapMany(Flux::fromIterable);
    }

    private List<Tuple2<CodeSnippet, CodeSnippet>> clones(final List<FileEntity> targetFiles,
                                                          final List<FileEntity> sourceFiles) {
        final Map<CloneClass, List<CodeSnippet>> cloneClassCodeSnippetsMap = new HashMap<>();
        final List<Tuple2<CodeSnippet, CodeSnippet>> result = new ArrayList<>();
        final SuffixTree<Token> suffixTree = CLONE_DETECTOR_INSTANCE.getTree();
        final long sourceFilesFirstSequenceId = 2;
        final long sourceFilesLastSequenceId = addFilesIntoTree(sourceFiles, suffixTree);
        final long targetFilesFirstSequenceId = sourceFilesLastSequenceId + 1;
        final long targetFilesLastSequenceId = addFilesIntoTree(targetFiles, suffixTree);

        LongStream.rangeClosed(sourceFilesFirstSequenceId, sourceFilesLastSequenceId).forEach(sequenceId ->
            CLONE_DETECTOR_INSTANCE.getAllSequenceCloneClasses(sequenceId, minCloneLength)
                    .stream()
                    .findFirst()
                    .ifPresent(treeCloneClass -> {
                        int cloneLength = treeCloneClass.getLength();
                        CloneClass cloneClass = new CloneClass(extractBeginToken(treeCloneClass),
                                                               extractEndToken(treeCloneClass));

                        treeCloneClass.getTreeNode().getEdges()
                                .stream()
                                .filter(edge -> {
                                    int lastElementIndex = edge.getSequence().size() - 1;
                                    final Object element = edge.getSequence().get(lastElementIndex);
                                    if (element instanceof EndToken endToken) {
                                        return endToken.getIdSequence() == sequenceId;
                                    } else {
                                        return false;
                                    }
                                })
                                .forEach(edge -> {
                                    int hahActuallyEnd = edge.getBegin();
                                    Token begin = (Token) edge.getSequence().get(hahActuallyEnd - cloneLength);
                                    Token end   = (Token) edge.getSequence().get(hahActuallyEnd - 1);
                                    CodeSnippet codeSnippet =
                                            new CodeSnippet((CommitSnapshot) begin.getCommitSnapshot(),
                                                            begin.getFilename(),
                                                            begin.getLine(),
                                                            end.getLine());
                                    cloneClassCodeSnippetsMap
                                            .computeIfAbsent(cloneClass, __ -> new ArrayList<>())
                                            .add(codeSnippet);
                                });
                    }));

        LongStream.rangeClosed(targetFilesFirstSequenceId, targetFilesLastSequenceId)
                .forEach(targetMethodId -> CLONE_DETECTOR_INSTANCE
                        .getAllSequenceCloneClasses(targetMethodId, minCloneLength)
                        .stream()
                        .findFirst()
                        .ifPresent(treeCloneClass -> {
                            int cloneLength = treeCloneClass.getLength();
                            CloneClass cloneClass = new CloneClass(extractBeginToken(treeCloneClass),
                                                                   extractEndToken(treeCloneClass));

                            treeCloneClass.getTreeNode().getEdges()
                                    .stream()
                                    .filter(edge -> {
                                        int lastElementIndex = edge.getSequence().size() - 1;
                                        final Object element = edge.getSequence().get(lastElementIndex);
                                        if (element instanceof EndToken endToken) {
                                            return endToken.getIdSequence() == targetMethodId;
                                        } else {
                                            return false;
                                        }
                                    })
                                    .forEach(edge -> {
                                        int hahActuallyEnd = edge.getBegin();
                                        Token begin = (Token) edge.getSequence().get(hahActuallyEnd - cloneLength);
                                        Token end = (Token) edge.getSequence().get(hahActuallyEnd - 1);
                                        CodeSnippet codeSnippetTarget =
                                                new CodeSnippet((CommitSnapshot) begin.getCommitSnapshot(),
                                                                begin.getFilename(),
                                                                begin.getLine(),
                                                                end.getLine());
                                        cloneClassCodeSnippetsMap
                                                .get(cloneClass)
                                                .forEach(codeSnippetSource ->
                                                        result.add(Tuples.of(codeSnippetTarget, codeSnippetSource)));
                                    });
                        }));
        CLONE_DETECTOR_INSTANCE.clear();
        return result;
    }

    /**
     * Utility method to insert list of FileEntities into SuffixTree
     * @param files - list of FileEntities
     * @param suffixTree - tree object reference
     * @return index of the last sequence (last tokenized method of the last FileEntity) inserted into the tree
     */
    private static long addFilesIntoTree(@NonNull final List<FileEntity> files, @NonNull final SuffixTree<Token> suffixTree) {
        return files.stream()
                .map(file -> addFileIntoTree(file, suffixTree))
                .max(Long::compareTo).orElseThrow();
    }

    /**
     * Utility method to insert parsed FileEntity's methods into SuffixTree
     * @param file - FileEntity object to parse into tokenized methods and then insert into tree
     * @param suffixTree - tree object reference
     * @return - index of the last sequence (tokenized method) inserted into the tree
     */
    private static long addFileIntoTree(@NonNull final FileEntity file, @NonNull final SuffixTree<Token> suffixTree) {
        return Parser.tokenizedFunctions(file)
                .map(suffixTree::addSequence)
                .max(Long::compareTo).orElseThrow();
    }

    private static Token extractBeginToken(TreeCloneClass treeCloneClass) {
        return treeCloneClass.getClones().iterator().next().getFirstElement();
    }

    private static Token extractEndToken(TreeCloneClass treeCloneClass) {
        return treeCloneClass.getClones().iterator().next().getLastElement();
    }

    @Value
    private static class CloneClass {
        Token from;
        Token to;
    }
}
