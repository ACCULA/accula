package org.accula.core.checkers;

import lombok.extern.slf4j.Slf4j;
import org.accula.parser.Java9Lexer;
import org.accula.parser.Java9Parser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@DisplayName("Clone checker tests")
class CloneCheckerImplTest {

    @NotNull
    private ParseTree getTree(@NotNull final String path) {
        CharStream charStream = null;
        try {
            charStream = CharStreams.fromFileName(path);
        } catch (IOException e) {
            log.error("Error parsing files {}", e.getMessage());
        }
        var lexer = new Java9Lexer(charStream);
        var tokens = new CommonTokenStream(lexer);
        var parser = new Java9Parser(tokens);
        return parser.compilationUnit();
    }
    @Test
    @DisplayName("Test two simple clones")
    void checkClones1() {
        var tree1 = getTree("src/test/resources/SimpleClone_0.java");
        var tree2 = getTree("src/test/resources/SimpleClone_1.java");
        var checker = new CloneCheckerImpl();
        var f = checker.checkClones(tree1, tree2, 0.7f);
        assertEquals(1, f.getLinesFromSecondFile().size());
    }

    @Test
    @DisplayName("Test two different files 1")
    void checkClones2() {
        var tree1 = getTree("src/test/resources/Unique.java");
        var tree2 = getTree("src/test/resources/SimpleClone_0.java");
        var checker = new CloneCheckerImpl();
        var f = checker.checkClones(tree1, tree2, 0.9f);
        assertEquals(0, f.getLinesFromSecondFile().size());
    }

    @Test
    @DisplayName("Test two different files 2")
    void checkClones3() {
        var tree1 = getTree("src/test/resources/Unique.java");
        var tree2 = getTree("src/test/resources/SimpleClone_1.java");
        var checker = new CloneCheckerImpl();
        var f = checker.checkClones(tree1, tree2, 0.9f);
        assertEquals(0, f.getLinesFromSecondFile().size());
    }

    @Test
    @DisplayName("Test on the same file")
    void checkClones4() {
        var tree = getTree("src/test/resources/Unique.java");
        var checker = new CloneCheckerImpl();
        var f = checker.checkClones(tree, tree, 0.9f);
        assertEquals(1.0f, f.getNormalizedMetric());
        assertEquals(1, f.getLinesFromSecondFile().size());
    }

    @Test
    @DisplayName("Test on the real files")
    void checkClones5() {
        var tree1 = getTree("src/test/resources/Test_1.java");
        var tree2 = getTree("src/test/resources/Test_2.java");
        var checker = new CloneCheckerImpl();
        var f = checker.checkClones(tree1, tree2, 0.9f);
        assertEquals(0, f.getLinesFromSecondFile().size());
    }

    @Test
    @DisplayName("Test files vice versa")
    void checkClones6() {
        var tree1 = getTree("src/test/resources/Test_3.java");
        var tree2 = getTree("src/test/resources/Test_4.java");
        var checker = new CloneCheckerImpl();
        var f = checker.checkClones(tree2, tree1, 0.9f);
        assertEquals(0, f.getLinesFromSecondFile().size());

        f = checker.checkClones(tree1, tree2, 0.9f);
        assertEquals(0, f.getLinesFromSecondFile().size());
    }

    @Test
    @DisplayName("Test on the real files 2")
    void checkClones7() {
        var tree1 = getTree("src/test/resources/Test_5.java");
        var tree2 = getTree("src/test/resources/Test_6.java");
        var checker = new CloneCheckerImpl();
        var f = checker.checkClones(tree1, tree2, 0.9f);
        assertEquals(1, f.getLinesFromSecondFile().size());
    }

//    Looking like real clones :)
    @Test
    @DisplayName("Test on the real files 3")
    void checkClones8() {
        var tree1 = getTree("src/test/resources/Test_7.java");
        var tree2 = getTree("src/test/resources/Test_8.java");
        var checker = new CloneCheckerImpl();
        var f = checker.checkClones(tree1, tree2, 0.9f);
        assertEquals(1, f.getLinesFromSecondFile().size());
    }
}
