import { ComponentType } from 'react'

export interface IToken {
  accessToken: string
}

export interface IUser {
  id: number
  login: string
  name: string
}

export interface IRouteInfo {
  path: string
  name: string
  component: ComponentType<any>
  exact?: boolean
  icon?: string
  hidden?: boolean
  authRequired?: boolean
}

export interface IProject {
  id: number
  repoUrl: string
  creatorId: number
  repoOwner: string
  repoName: string
  repoDescription?: string
  repoOwnerAvatar: string
  repoOpenPullCount: number
}

export interface IProjectConf {
  admins: number[]
  cloneMinLineCount: number
}

export interface IShortPull {
  number: number
  projectId: number
  url: string
  title: string
  open: boolean
  author: {
    url: string
    login: string
    avatar: string
  }
}

export interface IPull {
  number: number
  projectId: number
  url: string
  head: {
    url: string
    label: string
  }
  base: {
    url: string
    label: string
  }
  author: {
    url: string
    login: string
    avatar: string
  }
  title: string
  open: boolean
  createdAt: string
  updatedAt: string
  cloneDetectionState: string
  cloneCount: number
  previousPulls: IShortPull[]
}

export interface IProjectRef {
  projectId?: number
}

export interface IPullRef {
  projectId?: number
  pullNumber?: number
}

export interface ICompareRef {
  projectId?: number
  target?: number
  source?: number
}

export interface ICodeSnippet {
  projectId: number
  pullNumber: number
  owner: string
  repo: string
  sha: string
  file: string
  fromLine: number
  toLine: number
  content: string
}

export interface IClone {
  id: number
  target: ICodeSnippet
  source: ICodeSnippet
}

export interface IDiff {
  baseFilename?: string
  baseContent?: string
  headFilename?: string
  headContent?: string
}
