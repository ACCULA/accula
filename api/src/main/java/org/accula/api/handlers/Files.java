package org.accula.api.handlers;

import lombok.extern.slf4j.Slf4j;
import org.accula.api.model.FileModel;
import org.accula.api.model.GitPullRequest;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.*;
import org.eclipse.jgit.util.FileUtils;
import org.springframework.lang.Nullable;
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
                                          final String userLogin, final Integer prNumber, final Date prDate) {
        final File localPath = new File("/accula/github/" + repoName.replace("/", "_"));
        Git git = null;
        final String remoteUrl = "https://github.com/" + repoName;
        final String prRef = "refs/pull/" + prNumber + "/head";
        Flux<FileModel> files = Flux.empty();
        try {
            git = Git.open(localPath);
        } catch (IOException e) {
            try {
                git = Git.cloneRepository()
                        .setURI(remoteUrl)
                        .setDirectory(localPath)
                        .call();
            } catch (GitAPIException ex) {
                log.error("Git clone error", ex);
                return files;
            }
        }
        try {
            final Repository repo = git.getRepository();
            final String prUrl = remoteUrl + prRef.replaceFirst("refs", "");
            git.fetch()
                    .setRemote(remoteUrl)
                    .setRefSpecs(new RefSpec(prRef + ":" + prRef))
                    .call();

            final AnyObjectId baseCommit  = findBaseCommit(repo, prRef,"HEAD");
            if (baseCommit == null) {
                return files;
            }
            final AbstractTreeIterator oldTreeParser = prepareTreeParser(repo, baseCommit);
            final AbstractTreeIterator newTreeParser = prepareTreeParser(repo, repo.resolve(prRef));
            List<DiffEntry> diff = git.diff()
                    .setOldTree(oldTreeParser)
                    .setNewTree(newTreeParser)
                    .setShowNameAndStatusOnly(true)
                    .call();

            final List<TreeFilter> treeFilter = new ArrayList<>();
            for (final DiffEntry entry : diff) {
                treeFilter.add(PathFilter.create(entry.getNewPath()));
            }
            if (treeFilter.size() > 0) {
                files = getFiles(repo.resolve(prRef), repo, prUrl, userLogin, prDate, treeFilter)
                        .filter(f -> !excludeFiles.contains(f.getFilename()));
            }
        } catch (IOException | GitAPIException e) {
            log.error("Git error", e);
        } finally {
            if (git != null) {
                git.close();
            }
        }
        return files;
    }

    @Nullable
    private static AnyObjectId findBaseCommit(final Repository repo, final String prRef, final String masterRef) throws IOException {
        try (final RevWalk walk = new RevWalk(repo)) {
            walk.setRevFilter(RevFilter.MERGE_BASE);
            final RevCommit prCommit = walk.parseCommit(repo.resolve(prRef));
            final RevCommit masterCommit = walk.parseCommit(repo.resolve(masterRef));
            walk.markStart(prCommit);
            walk.markStart(masterCommit);
            final RevCommit base = walk.next();
            walk.dispose();
            if (base == null) {
                return null;
            }
            return base.getId();
        }
    }

    private static AbstractTreeIterator prepareTreeParser(final Repository repository, final AnyObjectId commitId) throws IOException {
        // from the commit we can build the tree which allows us to construct the TreeParser
        try (final RevWalk walk = new RevWalk(repository)) {
            final RevCommit commit = walk.parseCommit(commitId);
            final RevTree tree = walk.parseTree(commit.getTree().getId());
            final CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (final ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }

            walk.dispose();

            return treeParser;
        }
    }

    public static void deleteRepo(final String repoName){
        final File localPath = new File("/accula/github/" + repoName.replace("/", "_"));
        try {
            FileUtils.delete(localPath, FileUtils.RECURSIVE);
        } catch (IOException e) {
            log.error("Can not delete repository", e);
        }
    }

    public static Flux<FileModel> getFiles(final ObjectId id, final Repository repository, final String prUrl,
                                           final String userLogin, final Date prDate, List<TreeFilter> treeFilter) {
        final List<FileModel> files = new ArrayList<>();
        try (RevWalk revWalk = new RevWalk(repository)) {
            final RevCommit commit = revWalk.parseCommit(id);
            final RevTree tree = commit.getTree();
            final TreeFilter filter;
            if(treeFilter.size() > 1) {
                filter = AndTreeFilter.create(PathSuffixFilter.create(".java"), OrTreeFilter.create(treeFilter));
            }
            else {
                filter = AndTreeFilter.create(PathSuffixFilter.create(".java"), treeFilter.get(0));
            }
            try (final TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(filter);
                while (treeWalk.next()) {
                    final ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
                    final String content = new String(loader.getBytes());
                    files.add(new FileModel(treeWalk.getNameString(), treeWalk.getPathString(), prUrl,
                            userLogin, prDate, content));
                }
            }
            revWalk.dispose();
        } catch (IOException e) {
            log.error("Parsing PR error", e);
        }
        return Flux.fromIterable(files);
    }

    public static Flux<FileModel> getRepoFiles(final GitPullRequest pr, final Set<String> excludeFiles) {
        // get repository in format "owner/repo" from pull request url
        final Pattern pattern = Pattern.compile("api.github.com/repos/(.*?)/pulls/");
        final Matcher matcher = pattern.matcher(pr.getUrl());
        return matcher.find() ?
                getRepo(matcher.group(1), excludeFiles, pr.getUser().getLogin(), pr.getNumber(), pr.getCreatedAt()) : Flux.empty();
    }
}
