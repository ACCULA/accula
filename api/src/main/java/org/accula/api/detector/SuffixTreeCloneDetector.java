package org.accula.api.detector;

import com.suhininalex.clones.core.CloneIndexer;
import com.suhininalex.clones.core.structures.CloneClass;
import com.suhininalex.clones.core.structures.Token;
import com.suhininalex.clones.core.structures.Token.InfoKey;
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

/**
 *
 */
public class SuffixTreeCloneDetector implements CloneDetector {

    private static final CloneIndexer CLONE_DETECTOR_INSTANCE = CloneIndexer.INSTANCE;
    private static final SuffixTree<Token> SUFFIX_TREE = CLONE_DETECTOR_INSTANCE.getTree();

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
        final List<Long> targetFilesMethodsIds = new ArrayList<>();
        final List<Tuple2<CodeSnippet, CodeSnippet>> result = new ArrayList<>();
        final Map<InfoKey, CommitSnapshot> tokenCommitSnapshotMap =
                new HashMap<>(sourceFiles.size() + targetFiles.size());

        sourceFiles.forEach(file -> {
            tokenCommitSnapshotMap.put(new InfoKey(extractOwnerIdFromFileEntity(file),
                                                   extractRepoIdFromFileEntity(file)),
                                       file.getCommitSnapshot());
            Parser.getFunctionsAsTokens(file).forEach(SUFFIX_TREE::addSequence);
        });

        targetFiles.forEach(file -> {
            tokenCommitSnapshotMap.put(new InfoKey(extractOwnerIdFromFileEntity(file),
                                                   extractRepoIdFromFileEntity(file)),
                                       file.getCommitSnapshot());
            Parser.getFunctionsAsTokens(file)
                    .forEach(tokens ->
                            targetFilesMethodsIds.add(SUFFIX_TREE.addSequence(tokens)));
        });

        final long lastSourceFilesSequenceId = targetFilesMethodsIds.get(0) - 1;

        for (long sequenceId = 2; sequenceId <= lastSourceFilesSequenceId; sequenceId++) {
            long finalSequenceId = sequenceId;
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
                                        return endToken.getIdSequence() == finalSequenceId;
                                    } else {
                                        return false;
                                    }
                                })
                                .forEach(edge -> {
                                    int hahActuallyEnd = edge.getBegin();
                                    Token begin = (Token) edge.getSequence().get(hahActuallyEnd - cloneLength);
                                    Token end = (Token) edge.getSequence().get(hahActuallyEnd - 1);
                                    CodeSnippet codeSnippet =
                                            new CodeSnippet(tokenCommitSnapshotMap.get(begin.getInfoKey()),
                                                            begin.getFilename(),
                                                            begin.getLine(),
                                                            end.getLine());
                                    cloneClassCodeSnippetsMap
                                            .computeIfAbsent(cloneClass, __ -> new ArrayList<>())
                                            .add(codeSnippet);
                                });
                    });
        }

        targetFilesMethodsIds
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
                                                new CodeSnippet(tokenCommitSnapshotMap.get(begin.getInfoKey()),
                                                        begin.getFilename(),
                                                        begin.getLine(),
                                                        end.getLine());
                                        cloneClassCodeSnippetsMap
                                                .get(cloneClass)
                                                .forEach(codeSnippetSource ->
                                                        result.add(Tuples.of(codeSnippetTarget, codeSnippetSource)));
                                    });
                        }));

        // TODO: add clone mapper
        return result;
    }

    private static long extractOwnerIdFromFileEntity(@NonNull FileEntity fileEntity) {
        return fileEntity.getCommitSnapshot().getRepo().getOwner().getId();
    }

    private static long extractRepoIdFromFileEntity(@NonNull FileEntity fileEntity) {
        return fileEntity.getCommitSnapshot().getRepo().getId();
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
