package org.accula.api.detector;

import com.suhininalex.clones.core.CloneIndexer;
import com.suhininalex.clones.core.structures.Token;
import com.suhininalex.clones.core.structures.TreeCloneClass;
import com.suhininalex.suffixtree.SuffixTree;
import lombok.NonNull;
import lombok.Value;
import org.accula.api.code.FileEntity;
import org.accula.api.detector.parser.Parser;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.stream.LongStream;

import static org.accula.api.detector.util.SuffixTreeUtils.*;


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
        final List<Tuple2<CodeSnippet, CodeSnippet>> resultList = new ArrayList<>();
        final SuffixTree<Token> suffixTree = CLONE_DETECTOR_INSTANCE.getTree();
        final long sourceFilesFirstSequenceId = 2;
        //NB! Source files must be added into suffixTree BEFORE target files
        final long sourceFilesLastSequenceId = addFilesIntoTree(sourceFiles, suffixTree);
        final long targetFilesFirstSequenceId = sourceFilesLastSequenceId + 1;
        final long targetFilesLastSequenceId = addFilesIntoTree(targetFiles, suffixTree);

        LongStream.rangeClosed(sourceFilesFirstSequenceId, sourceFilesLastSequenceId)
                .forEach(methodId ->
                        extractClonesIntoMapForSourceMethod(methodId, cloneClassCodeSnippetsMap));

        LongStream.rangeClosed(targetFilesFirstSequenceId, targetFilesLastSequenceId)
                .forEach(targetMethodId ->
                        addClonesToListForTargetMethod(targetMethodId, resultList, cloneClassCodeSnippetsMap));

        CLONE_DETECTOR_INSTANCE.clear();
        return resultList;
    }

    private void extractClonesIntoMapForSourceMethod(@NonNull final Long methodId,
                                                     @NonNull final Map<CloneClass,
                                                             List<CodeSnippet>> cloneClassCodeSnippetsMap) {
        getTreeCloneClassForMethod(methodId)
                .ifPresent(treeCloneClass -> {
                    final int cloneLength = treeCloneClass.getLength();
                    CloneClass cloneClass = new CloneClass(extractBeginToken(treeCloneClass),
                                                           extractEndToken(treeCloneClass));

                    edgesFromTreeCloneClassForMethod(treeCloneClass, methodId)
                            .forEach(edge -> {
                                CodeSnippet codeSnippet = getCodeSnippetFromEdge(edge, cloneLength);
                                putCodeSnippetIntoCloneClassCodeSnippetsMap(codeSnippet,
                                                                            cloneClass,
                                                                            cloneClassCodeSnippetsMap);
                            });
                });
    }

    public void addClonesToListForTargetMethod(@NonNull final Long methodId,
                                               @NonNull final List<Tuple2<CodeSnippet, CodeSnippet>> clones,
                                               @NonNull final Map<CloneClass,
                                                       List<CodeSnippet>> cloneClassCodeSnippetsMap) {
       getTreeCloneClassForMethod(methodId)
                .ifPresent(treeCloneClass -> {
                    final int cloneLength = treeCloneClass.getLength();
                    CloneClass cloneClass = new CloneClass(extractBeginToken(treeCloneClass),
                                                           extractEndToken(treeCloneClass));

                    edgesFromTreeCloneClassForMethod(treeCloneClass, methodId)
                            .forEach(edge -> {
                                CodeSnippet codeSnippetTarget = getCodeSnippetFromEdge(edge, cloneLength);
                                getClonesForCloneClassIntoList(codeSnippetTarget,
                                                               cloneClass,
                                                               clones,
                                                               cloneClassCodeSnippetsMap);
                            });
                });
    }

    private Optional<TreeCloneClass> getTreeCloneClassForMethod(@NonNull final Long methodId) {
        return CLONE_DETECTOR_INSTANCE
                .getAllSequenceCloneClasses(methodId, minCloneLength)
                .stream()
                .findFirst();
    }

    private static void putCodeSnippetIntoCloneClassCodeSnippetsMap(@NonNull final CodeSnippet codeSnippetValue,
                                                                    @NonNull final CloneClass cloneClassKey,
                                                                    @NonNull final Map<CloneClass,
                                                                            List<CodeSnippet>> cloneClassCodeSnippetsMap) {
        cloneClassCodeSnippetsMap
                .computeIfAbsent(cloneClassKey, __ -> new ArrayList<>())
                .add(codeSnippetValue);
    }

    private static void getClonesForCloneClassIntoList(@NonNull final CodeSnippet codeSnippetTarget,
                                                       @NonNull final CloneClass cloneClassKey,
                                                       @NonNull final List<Tuple2<CodeSnippet, CodeSnippet>> clones,
                                                       @NonNull final Map<CloneClass,
                                                               List<CodeSnippet>> cloneClassCodeSnippetsMap) {
        cloneClassCodeSnippetsMap.get(cloneClassKey)
                .forEach(codeSnippetSource ->
                        clones.add(Tuples.of(codeSnippetTarget, codeSnippetSource)));
    }

    /**
     * Utility method to insert list of FileEntities into SuffixTree
     * @param files - list of FileEntities
     * @param suffixTree - tree object reference
     * @return index of the last sequence (last tokenized method of the last FileEntity) inserted into the tree
     */
    private static long addFilesIntoTree(@NonNull final List<FileEntity> files,
                                         @NonNull final SuffixTree<Token> suffixTree) {
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
    private static long addFileIntoTree(@NonNull final FileEntity file,
                                        @NonNull final SuffixTree<Token> suffixTree) {
        return Parser.tokenizedFunctions(file)
                .map(suffixTree::addSequence)
                .max(Long::compareTo).orElseThrow();
    }

    @Value
    private static class CloneClass {
        Token from;
        Token to;
    }
}
