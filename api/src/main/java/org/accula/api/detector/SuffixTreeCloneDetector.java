package org.accula.api.detector;

import com.suhininalex.clones.core.CloneIndexer;
import com.suhininalex.clones.core.structures.Token;
import com.suhininalex.clones.core.structures.TreeCloneClass;
import com.suhininalex.suffixtree.SuffixTree;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.code.FileEntity;
import org.accula.api.detector.parser.Parser;
import org.accula.api.util.RLambda;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.LongStream;

import static java.util.function.Predicate.not;
import static org.accula.api.detector.util.SuffixTreeUtils.edgesFromTreeCloneClassForMethod;
import static org.accula.api.detector.util.SuffixTreeUtils.extractBeginToken;
import static org.accula.api.detector.util.SuffixTreeUtils.extractEndToken;
import static org.accula.api.detector.util.SuffixTreeUtils.getCodeSnippetFromEdge;

/**
 * @author Vanya Krylov
 */
@Slf4j
@RequiredArgsConstructor
public final class SuffixTreeCloneDetector implements CloneDetector {
    private final ConfigProvider configProvider;
    private static final long SRC_FIRST_METHOD_ID = 2;

    @Override
    public Flux<Tuple2<CodeSnippet, CodeSnippet>> findClones(final Flux<FileEntity> targetFiles, final Flux<FileEntity> sourceFiles) {
        return RLambda
                .zip(targetFiles.collectList(), sourceFiles.collectList(), configProvider.get(), this::clones)
                .flatMapMany(Flux::fromIterable);
    }

    private List<Tuple2<CodeSnippet, CodeSnippet>> clones(final List<FileEntity> targetFiles,
                                                          final List<FileEntity> sourceFiles,
                                                          final Config config) {
        final var cloneDetectorInstance = new CloneIndexer();
        final var cloneClassCodeSnippetsMap = new HashMap<CloneClass, List<CodeSnippet>>();
        final var resultList = new ArrayList<Tuple2<CodeSnippet, CodeSnippet>>();
        final var suffixTree = cloneDetectorInstance.getTree();
        try {
            //NB! Source files must be added into suffixTree BEFORE target files
            final long srcLastMethodId = addFilesIntoTree(sourceFiles, suffixTree);
            final long targetFirstMethodId = srcLastMethodId + 1;
            final long targetLastMethodId = addFilesIntoTree(targetFiles, suffixTree);

            LongStream.rangeClosed(SRC_FIRST_METHOD_ID, srcLastMethodId).forEach(methodId ->
                    extractClonesIntoMapForSourceMethod(methodId, cloneDetectorInstance, cloneClassCodeSnippetsMap, config));
            LongStream.rangeClosed(targetFirstMethodId, targetLastMethodId).forEach(targetMethodId ->
                    addClonesToListForTargetMethod(targetMethodId, cloneDetectorInstance, resultList, cloneClassCodeSnippetsMap, config));
        } catch (NoSuchElementException e) {
            log.error("Invalid data from parser! Failed to get last index with: " + e.getMessage());
        }

        return resultList;
    }

    private void extractClonesIntoMapForSourceMethod(final Long methodId,
                                                     final CloneIndexer cloneDetectorInstance,
                                                     final Map<CloneClass, List<CodeSnippet>> cloneClassCodeSnippetsMap,
                                                     final Config config) {
        getTreeCloneClassForMethod(methodId, cloneDetectorInstance, config)
                .ifPresent(treeCloneClass -> {
                    final var cloneClass = new CloneClass(extractBeginToken(treeCloneClass), extractEndToken(treeCloneClass));
                    edgesFromTreeCloneClassForMethod(treeCloneClass, methodId).forEach(edge -> {
                        final var codeSnippet = getCodeSnippetFromEdge(edge, treeCloneClass.getLength());
                        putCodeSnippetIntoCloneClassCodeSnippetsMap(codeSnippet, cloneClass, cloneClassCodeSnippetsMap);
                    });
                });
    }

    private void addClonesToListForTargetMethod(final Long methodId,
                                                final CloneIndexer cloneDetectorInstance,
                                                final List<Tuple2<CodeSnippet, CodeSnippet>> clones,
                                                final Map<CloneClass, List<CodeSnippet>> cloneClassCodeSnippetsMap,
                                                final Config config) {
       getTreeCloneClassForMethod(methodId, cloneDetectorInstance, config)
               .ifPresent(treeCloneClass -> {
                   final var cloneClass = new CloneClass(extractBeginToken(treeCloneClass), extractEndToken(treeCloneClass));
                   edgesFromTreeCloneClassForMethod(treeCloneClass, methodId).forEach(edge -> {
                       final var codeSnippetTarget = getCodeSnippetFromEdge(edge, treeCloneClass.getLength());
                       insertCloneClassClonesIntoList(codeSnippetTarget, cloneClass, clones, cloneClassCodeSnippetsMap);
                   });
               });
    }

    private Optional<TreeCloneClass> getTreeCloneClassForMethod(final Long methodId,
                                                                final CloneIndexer cloneDetectorInstance,
                                                                final Config config) {
        return cloneDetectorInstance.getAllSequenceCloneClasses(methodId, config.getMinCloneLength()).stream().findFirst();
    }

    private static void putCodeSnippetIntoCloneClassCodeSnippetsMap(final CodeSnippet codeSnippetValue,
                                                                    final CloneClass cloneClassKey,
                                                                    final Map<CloneClass, List<CodeSnippet>> cloneClassCodeSnippetsMap) {
        cloneClassCodeSnippetsMap
                .computeIfAbsent(cloneClassKey, __ -> new ArrayList<>())
                .add(codeSnippetValue);
    }

    private static void insertCloneClassClonesIntoList(final CodeSnippet codeSnippetTarget,
                                                       final CloneClass cloneClassKey,
                                                       final List<Tuple2<CodeSnippet, CodeSnippet>> clones,
                                                       final Map<CloneClass, List<CodeSnippet>> cloneClassCodeSnippetsMap) {
        cloneClassCodeSnippetsMap
                .get(cloneClassKey)
                .forEach(codeSnippetSource -> clones.add(Tuples.of(codeSnippetTarget, codeSnippetSource)));
    }

    /**
     * Utility method to insert list of FileEntities into SuffixTree
     *
     * @param files      - list of FileEntities
     * @param suffixTree - tree object reference
     * @return index of the last sequence (last tokenized method of the last FileEntity) inserted into the tree
     */
    private static long addFilesIntoTree(final List<FileEntity> files, final SuffixTree<Token> suffixTree) {
        return files.stream()
                .map(file -> addFileIntoTree(file, suffixTree))
                .filter(OptionalLong::isPresent)
                .mapToLong(OptionalLong::getAsLong)
                .max()
                .orElseThrow();
    }

    /**
     * Utility method to insert parsed FileEntity's methods into SuffixTree
     *
     * @param file       - FileEntity object to parse into tokenized methods and then insert into tree
     * @param suffixTree - tree object reference
     * @return - Optional of the index of the last sequence (tokenized method) inserted into the tree
     */
    private static OptionalLong addFileIntoTree(final FileEntity file, final SuffixTree<Token> suffixTree) {
        return Parser.tokenizedFunctions(file)
                .filter(not(List::isEmpty))
                .mapToLong(suffixTree::addSequence)
                .max();
    }

    @Value
    private static class CloneClass {
        Token from;
        Token to;
    }
}
