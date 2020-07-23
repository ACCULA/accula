import { IClone, IProject, IPull } from 'types'

export const projects: IProject[] = [
  {
    id: 1,
    repoUrl: 'https://github.com/polis-mail-ru/2019-highload-dht',
    creatorId: 1,
    repoOwner: 'polis-mail-ru',
    repoName: '2019-highload-dht',
    repoDescription: 'Курсовой проект 2019 года курса "Highload системы"',
    repoOwnerAvatar: 'https://avatars2.githubusercontent.com/u/31819365?s=200&v=4',
    repoOpenPullCount: 10
  },
  {
    id: 2,
    repoUrl: 'https://github.com/ACCULA/accula',
    creatorId: 1,
    repoOwner: 'ACCULA',
    repoName: 'accula',
    repoDescription: 'Advanced Code Clones UniversaL Analyzer',
    repoOwnerAvatar: 'https://avatars0.githubusercontent.com/u/61988411?s=200&v=4',
    repoOpenPullCount: 0
  },
  {
    id: 3,
    repoUrl: 'https://github.com/polis-mail-ru/2020-db-lsm',
    creatorId: 1,
    repoOwner: 'polis-mail-ru',
    repoName: '2020-db-lsm',
    repoDescription: 'NoSQL course project',
    repoOwnerAvatar: 'https://avatars2.githubusercontent.com/u/31819365?s=200&v=4',
    repoOpenPullCount: 19
  }
]

export const pulls: IPull[] = [
  {
    number: 3,
    projectId: 1,
    title: 'Stage 1',
    url: 'https://github.com/polis-mail-ru/2019-highload-dht/pull/3',
    base: {
      url: 'https://github.com/polis-mail-ru/2019-highload-dht',
      label: 'polis-mail-ru:master'
    },
    head: {
      url: 'https://github.com/kilinochi/2019-highload-dht',
      label: 'kilinochi:master'
    },
    author: {
      url: 'https://github.com/kilinochi',
      login: 'kilinochi',
      avatar: 'https://avatars2.githubusercontent.com/u/34065879?v=4'
    },
    open: false,
    createdAt: '2019-09-30T06:09:57Z',
    updatedAt: '2019-10-06T09:00:40Z',
    status: 'Read',
    cloneCount: 100,
    previousPulls: []
  },
  {
    number: 5,
    projectId: 1,
    title: 'Single Node | Vadim Dyachkov',
    url: 'https://github.com/polis-mail-ru/2019-highload-dht/pull/5',
    base: {
      url: 'https://github.com/polis-mail-ru/2019-highload-dht',
      label: 'polis-mail-ru:master'
    },
    head: {
      url: 'https://github.com/vaddya/2019-highload-dht',
      label: 'vaddya:master'
    },
    author: {
      url: 'https://github.com/vaddya',
      login: 'vaddya',
      avatar: 'https://avatars3.githubusercontent.com/u/15687094?v=4'
    },
    open: true,
    createdAt: '2020-05-06T13:02:18Z',
    updatedAt: '2020-05-11T04:02:18Z',
    status: 'Processing',
    cloneCount: 0,
    previousPulls: []
  }
]

const oldCode = `
package org.accula.api.db;

import org.accula.api.db.dto.RefreshToken;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
@Repository
public interface RefreshTokenRepository extends ReactiveCrudRepository<RefreshToken, Long> {
    //@formatter:off
    @Modifying
    @Query("UPDATE refresh_token " +
           "SET token = :newToken, expiration_date = :newExpirationDate " +
           "WHERE user_id = :userId AND token = :oldToken")
    Mono<Void> replaceRefreshToken(final Long userId,
                                   final String oldToken,
                                   final String newToken,
                                   final Instant newExpirationDate);
    //@formatter:on
}
`

const newCode = `
package org.accula.api.db;

import org.accula.api.db.dto.RefreshToken;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * @author Anton Lamtev
 * @author Vadim Dyachkov
 */
@Repository
public interface RefreshTokenRepository extends ReactiveCrudRepository<RefreshToken, Long> {
    @Modifying
    @Query("UPDATE refresh_token " +
           "SET token = :newToken, expiration_date = :newExpirationDate " +
           "WHERE token = :oldToken AND user_id = :userId")
    Mono<Void> replaceRefreshToken(final Long userId,
                                   final String oldToken,
                                   final String newToken,
                                   final Instant newExpirationDate);
}
`

export const files = { oldCode, newCode }

const oldCode2 = `
@Modifying
@Query("UPDATE refresh_token " +
       "SET token = :newToken, expiration_date = :newExpirationDate " +
       "WHERE user_id = :userId AND token = :oldToken")
Mono<Void> replaceRefreshToken(final Long userId,
                               final String oldToken,
                               final String newToken,
                               final Instant newExpirationDate);
`

const newCode2 = `
@Modifying
@Query("UPDATE refresh_token " +
       "SET token = :newToken, expiration_date = :newExpirationDate " +
       "WHERE token = :oldToken AND user_id = :userId")
Mono<Void> replaceRefreshToken(final Long userId,
                               final String oldToken,
                               final String newToken,
                               final Instant newExpirationDate);
`

export const clones: IClone[] = [
  {
    id: 1,
    target: {
      projectId: 1,
      pullNumber: 6,
      owner: 'vaddya',
      repo: '2019-highload',
      sha: 'somesha',
      file: 'src/main/java/ru/mail/polis/Main.java',
      fromLine: 2,
      toLine: 2,
      content: btoa(oldCode2)
    },
    source: {
      projectId: 2,
      pullNumber: 4,
      owner: 'lamtev',
      repo: '2019-highload',
      sha: 'somesha',
      file: 'src/main/java/ru/mail/polis/MyMain.java',
      fromLine: 1,
      toLine: 1,
      content: btoa(newCode2)
    }
  },
  {
    id: 2,
    target: {
      projectId: 2,
      pullNumber: 1,
      owner: 'lamtev',
      repo: '2019-highload',
      sha: 'somesha',
      file: 'src/main/java/ru/mail/polis/Database.java',
      fromLine: 3,
      toLine: 3,
      content: btoa(oldCode2)
    },
    source: {
      projectId: 2,
      pullNumber: 4,
      owner: 'vaddya',
      repo: '2019-highload',
      sha: 'somesha',
      file: 'src/main/java/ru/mail/polis/Database.java',
      fromLine: 4,
      toLine: 4,
      content: btoa(newCode2)
    }
  }
]
