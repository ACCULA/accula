package org.accula.api.handlers;

import lombok.extern.slf4j.Slf4j;
import org.accula.api.model.FileModel;
import org.accula.api.model.GitPullRequest;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.util.FileUtils;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public final class Files {
    private Files(){
    }

    public static Flux<FileModel> getRepo(final String repoName, final Set<String> excludeFiles,
                                          final String excludeAuthor, final Integer prNumber) {
        final File localPath = new File("/accula/github/" + repoName.replace("/", "_"));
        Git git = null;
        final String remoteUrl = "https://github.com/" + repoName;
        // if prNumber = 0, take all pull requests of repository, else take pull request with number prNumber
        final String prRef = prNumber > 0 ? "refs/pull/" + prNumber + "/head" : "refs/pull/*/head";
        Flux<FileModel> files = Flux.empty();
        try {
            git = Git.cloneRepository()
                    .setURI(remoteUrl)
                    .setDirectory(localPath)
                    .call();
            git.fetch()
                    .setRemote(remoteUrl)
                    .setRefSpecs(new RefSpec(prRef + ":" + prRef))
                    .call();
            final Repository repo = git.getRepository();
            final List<Ref> refs = repo.getRefDatabase().getRefsByPrefix("refs/pull/");
            for (final Ref ref : refs) {
                final String prUrl = remoteUrl + ref.getName().replaceFirst("refs", "");
                files = getFiles(ref.getObjectId(), repo, prUrl, excludeFiles, excludeAuthor)
                        .concatWith(files);
            }
        } catch (GitAPIException | IOException e) {
            log.error("Git error", e);
        } finally {
            if (git != null) {
                git.close();
                try {
                    FileUtils.delete(localPath, FileUtils.RECURSIVE);
                } catch (IOException e) {
                    log.error("Can not delete repository", e);
                }
            }
        }
        return files;
    }

    public static Flux<FileModel> getFiles(final ObjectId id, final Repository repository, final String prUrl,
                                           final Set<String> excludeFiles, final String excludeAuthor) {
        final List<FileModel> files = new ArrayList<>();
        try (RevWalk revWalk = new RevWalk(repository)) {
            final RevCommit commit = revWalk.parseCommit(id);
            final String authorName = commit.getAuthorIdent().getName();
            if (authorName.equals(excludeAuthor)) {
                return Flux.fromIterable(files);
            }
            final Date changedAt = commit.getAuthorIdent().getWhen();
            final RevTree tree = commit.getTree();
            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(PathSuffixFilter.create(".java"));
                while (treeWalk.next()) {
                    final ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
                    final String content = new String(loader.getBytes());
                    files.add(new FileModel(treeWalk.getNameString(), treeWalk.getPathString(), prUrl,
                            authorName, changedAt, content));
                }
            }
            revWalk.dispose();
        } catch (IOException e) {
            log.error("Parsing PR error", e);
        }
        return Flux.fromIterable(files).filter(f -> !excludeFiles.contains(f.getFilename()));
    }

    public static Flux<FileModel> getStudentFiles(final GitPullRequest pr, final Set<String> excludeFiles) {
        // get repository in format "owner/repo" from pull request url
        final Pattern pattern = Pattern.compile("api.github.com/repos/(.*?)/pulls/");
        final Matcher matcher = pattern.matcher(pr.getUrl());
        return matcher.find() ?
                getRepo(matcher.group(1), excludeFiles, "", pr.getNumber()) : Flux.empty();
    }

    public static Flux<FileModel> getOtherFiles(final GitPullRequest pr,
                                                final Set<String> excludeFiles, final String repo) {
        String userName;
        if (pr.getUser().getName() != null) {
            if (pr.getUser().getName().isEmpty()) {
                userName = pr.getUser().getLogin();
            } else {
                userName = pr.getUser().getName();
            }
        } else {
            userName = "";
        }
        return getRepo(repo, excludeFiles, userName, 0);
    }
}
