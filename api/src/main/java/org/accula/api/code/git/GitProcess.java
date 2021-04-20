package org.accula.api.code.git;

import com.zaxxer.nuprocess.NuProcess;
import com.zaxxer.nuprocess.NuProcessBuilder;
import com.zaxxer.nuprocess.NuProcessHandler;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import lombok.SneakyThrows;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author Anton Lamtev
 */
final class GitProcess implements NuProcessHandler {
    private static final byte[] EMPTY = new byte[0];
    @Nullable
    private final ByteArrayList stdout;
    @Nullable
    private final CompletableFuture<Ret> stdoutFuture;
    @Nullable
    private final ByteArrayList stderr;
    @Nullable
    private final CompletableFuture<Ret> stderrFuture;
    @Nullable
    private final Consumer<ByteBuffer> stdinConsumer;
    @Nullable
    private NuProcess process;

    private GitProcess(@Nullable final ByteArrayList stdout,
                       @Nullable final ByteArrayList stderr,
                       @Nullable final Consumer<ByteBuffer> stdinConsumer) {
        this.stdout = stdout;
        this.stdoutFuture = stdout == null ? null : new CompletableFuture<>();
        this.stderr = stderr;
        this.stderrFuture = stderr == null ? null : new CompletableFuture<>();
        this.stdinConsumer = stdinConsumer;
    }

    public static Builder git(final String... cmd) {
        final var command = new ArrayList<String>(cmd.length + 1);
        command.add("git");
        Collections.addAll(command, cmd);
        return new Builder(command);
    }

    public Builder git(final List<String> cmd) {
        final var command = new ArrayList<String>(cmd.size() + 1);
        command.add("git");
        command.addAll(cmd);
        return new Builder(command);
    }

    public CompletableFuture<Ret> stdout() {
        if (stdoutFuture == null) {
            throw new IllegalStateException("GitProcess has no stdout");
        }
        return stdoutFuture;
    }

    public CompletableFuture<Ret> stderr() {
        if (stderrFuture == null) {
            throw new IllegalStateException("GitProcess has no stderr");
        }
        return stderrFuture;
    }

    @Override
    public void onPreStart(final NuProcess nuProcess) {
    }

    @Override
    public void onStart(final NuProcess nuProcess) {
        process = nuProcess;
    }

    @Override
    public void onExit(final int exitCode) {
        if (exitCode != 0) {
            if (stdoutFuture != null) {
                stdoutFuture.complete(Ret.of(exitCode, EMPTY, 0));
            }
            if (stderrFuture != null) {
                stderrFuture.complete(Ret.of(exitCode, stderr.elements(), stderr.size()));
            }
        } else {
            if (stdoutFuture != null) {
                stdoutFuture.complete(Ret.of(exitCode, stdout.elements(), stdout.size()));
            }
            if (stderrFuture != null) {
                stderrFuture.complete(Ret.of(exitCode, EMPTY, 0));
            }
        }

        process = null;
    }

    @Override
    public void onStdout(final ByteBuffer buffer, final boolean closed) {
        onStdStream(stdout, buffer, closed);
    }

    @Override
    public void onStderr(final ByteBuffer buffer, final boolean closed) {
        onStdStream(stderr, buffer, closed);
    }

    @Override
    public boolean onStdinReady(final ByteBuffer buffer) {
        if (stdinConsumer != null) {
            stdinConsumer.accept(buffer);
        }
        return false;
    }

    @SneakyThrows
    private static void onStdStream(@Nullable final ByteArrayList stream, final ByteBuffer buffer, final boolean closed) {
        if (stream == null) {
            buffer.position(buffer.limit());
            return;
        }

        final var remaining = buffer.remaining();
        if (remaining == 0) {
            return;
        }

        final var newSize = stream.size() + remaining;
        stream.ensureCapacity(newSize);
        buffer.get(stream.elements(), stream.size(), remaining);
        final var size = stream.getClass().getDeclaredField("size");
        size.setAccessible(true);
        size.setInt(stream, newSize);
    }

    @Value(staticConstructor = "of")
    static class Ret {
        int code;
        byte[] bytes;
        int size;
    }

    static final class Builder {
        private final List<String> cmd;
        private Path directory;
        private Consumer<ByteBuffer> stdinConsumer;
        private boolean stderrEnabled;
        private boolean stdoutDisabled;

        Builder(final List<String> cmd) {
            this.cmd = cmd;
        }

        Builder directory(final Path dir) {
            directory = dir;
            return this;
        }

        Builder enableStdin(final Consumer<ByteBuffer> stdinConsumer) {
            this.stdinConsumer = stdinConsumer;
            return this;
        }

        Builder enableStderr() {
            stderrEnabled = true;
            return this;
        }

        Builder disableStdout() {
            stdoutDisabled = true;
            return this;
        }

        GitProcess start() {
            if (directory == null) {
                throw new IllegalStateException("Directory is not specified");
            }
            final var process = new GitProcess(
                stdoutDisabled ? null : new ByteArrayList(NuProcess.BUFFER_CAPACITY),
                stderrEnabled ? new ByteArrayList(128) : null,
                stdinConsumer
            );
            final var pb = new NuProcessBuilder(cmd);
            pb.setCwd(directory);
            pb.setProcessListener(process);
            final var nu = pb.start();
            if (stdinConsumer != null) {
                nu.wantWrite();
            }
            return process;
        }
    }
}
