//package org.accula.parser;
//
//
//import lombok.Value;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.Comparator;
//
//@Value
//public class Token implements Comparable<Token> {
//    Integer type;
//    String text;
//    Integer line;
//    String filename;
//    String owner;
//    String path;
//
//    @Override
//    public int compareTo(@NotNull Token o) {
////        TODO -> compare by text only?
//        return Comparator
//                .comparing(Token::getText)
//                .thenComparingInt(Token::getType)
//                .thenComparingInt(Token::getLine)
//                .thenComparing(Token::getFilename)
//                .compare(this, o);
//    }
//
//    @Override
//    public String toString() {
//        return "T(" + text + ")";
//    }
//}
