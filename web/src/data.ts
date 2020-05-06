import { Project, PullRequest } from 'types'

export const projects: Project[] = [
  {
    id: 1,
    url: 'https://github.com/polis-mail-ru/2019-highload-dht',
    owner: 'polis-mail-ru',
    name: '2019-highload-dht',
    description: 'Курсовой проект 2019 года курса "Highload системы"',
    avatar: 'https://avatars2.githubusercontent.com/u/31819365?s=200&v=4',
    openPullRequestCount: 10
  },
  {
    id: 2,
    url: 'https://github.com/ACCULA/accula',
    owner: 'ACCULA',
    name: 'accula',
    description: 'Advanced Code Clones UniversaL Analyzer',
    avatar: 'https://avatars0.githubusercontent.com/u/61988411?s=200&v=4',
    openPullRequestCount: 0
  },
  {
    id: 3,
    url: 'https://github.com/polis-mail-ru/2020-db-lsm',
    owner: 'polis-mail-ru',
    name: '2020-db-lsm',
    description: 'NoSQL course project',
    avatar: 'https://avatars2.githubusercontent.com/u/31819365?s=200&v=4',
    openPullRequestCount: 19
  }
]

export const pulls: PullRequest[] = [
  {
    id: 3,
    title: 'Stage 1',
    url: 'https://api.github.com/repos/polis-mail-ru/2019-highload-dht/pulls/3',
    body: 'by Shamenov Arman',
    open: true,
    createdAt: '2019-09-30T06:09:57Z',
    updatedAt: '2019-10-06T09:00:40Z',
    author: {
      login: 'kilinochi',
      url: 'https://github.com/kilinochi',
      avatar: 'https://avatars2.githubusercontent.com/u/34065879?v=4'
    }
  },
  {
    id: 5,
    title: 'Single Node | Vadim Dyachkov',
    url: 'https://api.github.com/repos/polis-mail-ru/2019-highload-dht/pulls/5',
    body: '',
    open: true,
    createdAt: '2019-10-01T12:52:48Z',
    updatedAt: '2019-10-06T13:02:18Z',
    author: {
      login: 'vaddya',
      url: 'https://github.com/vaddya',
      avatar: 'https://avatars3.githubusercontent.com/u/15687094?v=4'
    }
  }
]
