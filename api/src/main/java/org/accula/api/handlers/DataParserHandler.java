package org.accula.api.handlers;

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

@Component
public class DataParserHandler {

    public Flux<FileModel> getRepo(final String repoName, final List<String> excludeFiles,
                                   final String excludeAuthor, final Integer prNumber)
 {
     final File localPath = new File("/accula/github/" + repoName.replace("/","_"));
     Git git = null;
     final String remoteUrl = "https://github.com/" + repoName;
     final String prRef = (prNumber > 0) ? "refs/pull/"+ prNumber+ "/head":"refs/pull/*/head";
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
         for (Map.Entry<String, Ref> entry : refs.entrySet()) {
             if(entry.getKey().contains("/pull/")){
                 final String prUrl = remoteUrl + entry.getKey().replaceFirst("refs","");
                 files = getFiles(entry.getValue().getObjectId(),repo,prUrl,excludeFiles,excludeAuthor)
                         .concatWith(files);
             }
         }
     }
     catch (InvalidRemoteException e) {
         e.printStackTrace();
     } catch (TransportException e) {
         e.printStackTrace();
     } catch (GitAPIException e) {
         e.printStackTrace();
     } finally {
         if (git != null) {
             git.close();
             try {
                 FileUtils.delete(localPath, FileUtils.RECURSIVE);
             } catch (IOException e) {
                 e.printStackTrace();
             }
         }
     }
     return files;
 }

    public Flux<FileModel> getFiles (final ObjectId id, final Repository repository, final String prUrl,
                                     final List<String> excludeFiles, final String excludeAuthor)
    {
        List<FileModel> files = new ArrayList<>();
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
            e.printStackTrace();
        }
        return Flux.fromIterable(files).filter(f -> (!excludeFiles.contains(f.getFilename())));
    }

    public Flux<FileModel> getStudentFiles(final GitPullRequest pr, final ArrayList<String> excludeFiles)
    {
        final String apiPrefix = "https://api.github.com/repos/";
        final String[] splitedUrl = pr.getUrl()
                            .replace(apiPrefix,"").split("/");
        final String repo = splitedUrl[0] + "/" + splitedUrl[1];
        return getRepo(repo,excludeFiles,"",pr.getNumber());
    }

    public Flux<FileModel> getOtherFiles(final GitPullRequest pr,
                                         final ArrayList<String> excludeFiles, final String repo)
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
     Mono<GitPullRequest> pullRequestInfo = request.bodyToMono(WebHookModel.class).flatMap(studentPR -> {
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
        ArrayList<String> excludeFiles = new ArrayList<>();
        excludeFiles.add("TestBase.java");
        excludeFiles.add("StartStopTest.java");
     //TODO: get repos from db
        ArrayList<String> repos = new ArrayList<>();
        repos.add("polis-mail-ru/2017-highload-kv");
        repos.add("ACCULA/accula");

        pullRequestInfo.subscribe(pr -> {
            Flux<FileModel> studentFiles = getStudentFiles(pr,excludeFiles);
            Flux<FileModel> otherFiles = Flux.fromIterable(repos)
                    .flatMap(repo -> getOtherFiles(pr,excludeFiles,repo));
            //TODO: start clone analyzing
            //detectClones(studentFiles,otherFiles);
        });

        return ok().build();
    }
}
