import { Project, PullRequest } from 'types'

export const projects: Project[] = [
  {
    id: 1,
    url: 'https://github.com/polis-mail-ru/2019-highload-dht',
    creator: 1,
    repoOwner: 'polis-mail-ru',
    name: '2019-highload-dht',
    description: 'Курсовой проект 2019 года курса "Highload системы"',
    avatar: 'https://avatars2.githubusercontent.com/u/31819365?s=200&v=4',
    openPullCount: 10
  },
  {
    id: 2,
    url: 'https://github.com/ACCULA/accula',
    creator: 1,
    repoOwner: 'ACCULA',
    name: 'accula',
    description: 'Advanced Code Clones UniversaL Analyzer',
    avatar: 'https://avatars0.githubusercontent.com/u/61988411?s=200&v=4',
    openPullCount: 0
  },
  {
    id: 3,
    url: 'https://github.com/polis-mail-ru/2020-db-lsm',
    creator: 1,
    repoOwner: 'polis-mail-ru',
    name: '2020-db-lsm',
    description: 'NoSQL course project',
    avatar: 'https://avatars2.githubusercontent.com/u/31819365?s=200&v=4',
    openPullCount: 19
  }
]

export const pulls: PullRequest[] = [
  {
    id: 3,
    projectId: 1,
    title: 'Stage 1',
    pullUrl: 'https://api.github.com/repos/polis-mail-ru/2019-highload-dht/pulls/3',
    fork: {
      url: '',
      branch: '',
      sha: ''
    },
    author: {
      login: 'kilinochi',
      avatar: 'https://avatars2.githubusercontent.com/u/34065879?v=4'
    },
    open: true,
    createdAt: '2019-09-30T06:09:57Z',
    updatedAt: '2019-10-06T09:00:40Z'
  },
  {
    id: 5,
    projectId: 1,
    title: 'Single Node | Vadim Dyachkov',
    pullUrl: 'https://api.github.com/repos/polis-mail-ru/2019-highload-dht/pulls/5',
    fork: {
      url: '',
      branch: '',
      sha: ''
    },
    author: {
      login: 'vaddya',
      avatar: 'https://avatars3.githubusercontent.com/u/15687094?v=4'
    },
    open: true,
    createdAt: '2019-10-01T12:52:48Z',
    updatedAt: '2019-10-06T13:02:18Z'
  }
]
