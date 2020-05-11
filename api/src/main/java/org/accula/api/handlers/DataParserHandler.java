package org.accula.api.handlers;

import lombok.extern.slf4j.Slf4j;
import org.accula.api.model.FileModel;
import org.accula.api.model.GitPullRequest;
import org.accula.api.model.GitUserModel;
import org.accula.api.model.WebHookModel;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.util.FileUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Slf4j
@Component
public class DataParserHandler {

    public Flux<FileModel> getRepo(final String repoName, final List<String> excludeFiles,
                                   final String excludeAuthor, final Integer prNumber)
 {
     final File localPath = new File("/accula/github/" + repoName.replace("/","_"));
     Git git = null;
     final String remoteUrl = "https://github.com/" + repoName;
     final String prRef = prNumber > 0 ? "refs/pull/"+ prNumber+ "/head":"refs/pull/*/head";
     Flux<FileModel> files = Flux.empty();
     try {
         git = Git.cloneRepository()
                 .setURI(remoteUrl)
                 .setDirectory(localPath)
                 .call();
         git.fetch()
                 .setRemote(remoteUrl)
                 .setRefSpecs(new RefSpec(prRef+":"+prRef))
                 .call();
         final Repository repo = git.getRepository();
         final Map<String, Ref> refs = repo.getAllRefs();
         for (final Map.Entry<String, Ref> entry : refs.entrySet()) {
             if(entry.getKey().contains("/pull/")){
                 final String prUrl = remoteUrl + entry.getKey().replaceFirst("refs","");
                 files = getFiles(entry.getValue().getObjectId(),repo,prUrl,excludeFiles,excludeAuthor)
                         .concatWith(files);
             }
         }
     }
     catch (GitAPIException e) {
         log.error("Git error", e);
     } finally {
         if (git != null) {
             git.close();
             try {
                 FileUtils.delete(localPath, FileUtils.RECURSIVE);
             } catch (IOException e) {
                 log.error("Git error", e);
             }
         }
     }
     return files;
 }

    public Flux<FileModel> getFiles (final ObjectId id, final Repository repository, final String prUrl,
                                     final List<String> excludeFiles, final String excludeAuthor)
    {
        final List<FileModel> files = new ArrayList<>();
        try (RevWalk revWalk = new RevWalk(repository)) {
            final RevCommit commit = revWalk.parseCommit(id);
            final String authorName = commit.getAuthorIdent().getName();
            if(authorName.equals(excludeAuthor))
            {
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
                    files.add(new FileModel(treeWalk.getNameString(),treeWalk.getPathString(), prUrl,
                            authorName,changedAt,content));
                }
            }
            revWalk.dispose();
        } catch (IOException e) {
            log.error("Git error", e);
        }
        return Flux.fromIterable(files).filter(f -> !excludeFiles.contains(f.getFilename()));
    }

    public Flux<FileModel> getStudentFiles(final GitPullRequest pr, final List<String> excludeFiles)
    {
        final String[] splitedUrl = pr.getUrl()
                            .replace("https://api.github.com/repos/","").split("/");
        final String repo = splitedUrl[0] + "/" + splitedUrl[1];
        return getRepo(repo,excludeFiles,"",pr.getNumber());
    }

    public Flux<FileModel> getOtherFiles(final GitPullRequest pr,
                                         final List<String> excludeFiles, final String repo)
    {
        String userName;
        if (pr.getUser().getName().isEmpty()) {
            userName = pr.getUser().getLogin();
        } else {
            userName = pr.getUser().getName();
        }
        return getRepo(repo, excludeFiles, userName, 0);
    }

    public Mono<ServerResponse> getWebHookInformation(final ServerRequest request) {
        final Mono<GitPullRequest> pullRequestInfo = request.bodyToMono(WebHookModel.class).flatMap(studentPR -> {
            final String userUrl = studentPR.getPull_request().getUser().getUrl();
            return WebClient.create(userUrl)
                 .get()
                 .retrieve()
                 .bodyToMono(GitUserModel.class)
                 .flatMap(st -> {
                     GitPullRequest pr = studentPR.getPull_request();
                     pr.setUser(st);
                     return Mono.just(pr);
                 });
        });
     //TODO: get files from db;
        final ArrayList<String> excludeFiles = new ArrayList<>();
        excludeFiles.add("TestBase.java");
        excludeFiles.add("StartStopTest.java");
     //TODO: get repos from db
        final ArrayList<String> repos = new ArrayList<>();
        repos.add("polis-mail-ru/2017-highload-kv");
        repos.add("ACCULA/accula");

        pullRequestInfo.subscribe(pr -> {
            final Flux<FileModel> studentFiles = getStudentFiles(pr,excludeFiles);
            final Flux<FileModel> otherFiles = Flux.fromIterable(repos)
                    .flatMap(repo -> getOtherFiles(pr,excludeFiles,repo));
            //TODO: start clone analyzing
        });

        return ok().build();
    }
}
