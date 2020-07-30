package org.accula.analyzer.research;

import lombok.Value;
import org.accula.analyzer.research.Clone;

@Value
public class CloneInfo {
    Clone clone;
    Clone real;

    @Override
    public String toString() {
        return clone.getOwner() + " | " + real.getOwner() + "\n" +
                clone.getFileName() + " | " + real.getFileName() + "\n" +
                clone.getFrom().getLine() + " (" + clone.getFrom().getText() + ") | " +
                real.getFrom().getLine() + " (" + real.getFrom().getText() + ")\n" +
                clone.getTo().getLine() + " (" + clone.getTo().getText() + ") | " +
                real.getTo().getLine() + " (" + real.getTo().getText() + ")\n";
    }
}
