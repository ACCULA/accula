package org.accula.api.code.util;

import lombok.SneakyThrows;
import org.eclipse.jgit.lib.ObjectLoader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringJoiner;

/**
 * @author Vadim Dyachkov
 */
public final class FileContentCutter {
    private FileContentCutter() {
    }

    @SneakyThrows
    public static String cutFileContent(final ObjectLoader loader, final int fromLine, final int toLine) {
        final StringJoiner joiner = new StringJoiner(System.lineSeparator());
        try (InputStream is = loader.openStream();
             InputStreamReader isr = new InputStreamReader(is);
             BufferedReader br = new BufferedReader(isr)) {
            int skip = fromLine - 1;
            int take = toLine - skip;
            String line = br.readLine();
            while (line != null) {
                if (skip > 0) {
                    skip--;
                    line = br.readLine();
                    continue;
                }
                joiner.add(line);
                take--;
                if (take == 0) {
                    break;
                }
                line = br.readLine();
            }
        }
        return joiner.toString();
    }
}
