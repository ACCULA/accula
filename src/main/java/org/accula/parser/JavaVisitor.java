package org.accula.parser;

import com.github.javaparser.JavaToken;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JavaVisitor extends VoidVisitorAdapter<List<List<JavaToken>>> {
    private final Set<JavaToken> typeArgs = new HashSet<>();

    @Override
    public void visit(final MethodDeclaration methodDeclaration, final List<List<JavaToken>> arg) {
        methodDeclaration
                .getBody()
                .flatMap(Node::getTokenRange)
                .ifPresent(tokenRange -> {
                    final var list = new ArrayList<JavaToken>();
                    tokenRange.forEach(list::add);
                    arg.add(list);
                });
        super.visit(methodDeclaration, arg);
    }

    @Override
    public void visit(final ClassOrInterfaceType classOrInterfaceType, final List<List<JavaToken>> arg) {
        classOrInterfaceType
                .getTypeArguments()
                .ifPresent(nodeList -> nodeList
                        .stream()
                        .map(Node::getTokenRange)
                        .forEach(tr -> tr.ifPresent(r -> r.forEach(typeArgs::add)))
                );
        super.visit(classOrInterfaceType, arg);
    }

    public Set<JavaToken> getTypeArgs() {
        return typeArgs;
    }
}
