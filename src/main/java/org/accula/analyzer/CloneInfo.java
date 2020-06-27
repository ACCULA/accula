package org.accula.analyzer;

import lombok.Value;

@Value
public class CloneInfo {
    Clone firstClone;
    Clone otherClone;

    @Override
    public String toString() {
        return "\tfirstClone=" + firstClone.getOwner() + "/" +
                firstClone.getFileName() + "/" +
                firstClone.getFrom().getLine() + "/" +
                firstClone.getTo().getLine() +
                "\n" +
                "\totherClone=" + otherClone.getOwner() + "/" +
                otherClone.getFileName() + "/" +
                otherClone.getFrom().getLine() + "/" +
                otherClone.getTo().getLine() + "\n";
    }
}
