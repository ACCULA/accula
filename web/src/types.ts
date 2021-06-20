import { ComponentType } from 'react'

export interface IToken {
  accessToken: string
}

export type UserRole = 'USER' | 'ADMIN' | 'ROOT'

export interface IUser {
  id: number
  login: string
  name: string
  avatar: string
  role: UserRole
}

export const hasAtLeastAdminRole = (user: IUser) => {
  if (user === undefined) {
    return false
  }
  if (!user) {
    return false
  }
  const { role } = user
  return role === 'ADMIN' || role === 'ROOT'
}

export interface IDiffColors {
  diffViewerBackground: string
  diffViewerColor: string
  addedBackground: string
  addedColor: string
  removedBackground: string
  removedColor: string
  wordAddedBackground: string
  wordRemovedBackground: string
  addedGutterBackground: string
  removedGutterBackground: string
  gutterBackground: string
  gutterBackgroundDark: string
  highlightBackground: string
  highlightGutterBackground: string
  codeFoldGutterBackground: string
  codeFoldBackground: string
  emptyLineBackground: string
  gutterColor: string
  addedGutterColor: string
  removedGutterColor: string
  codeFoldContentColor: string
  diffViewerTitleBackground: string
  diffViewerTitleColor: string
  diffViewerTitleBorderColor: string
}

export interface IColors {
  bgLight: string
  bgDark: string
  primaryLight: string
  primaryDark: string
  secondaryLight: string
  secondaryDark: string
  codeDiff: {
    light: IDiffColors
    dark: IDiffColors
  }
}

export type ThemeMode = 'dark' | 'light'
export type SplitCodeView = 'unified' | 'split'

export interface ISettings {
  themeMode: ThemeMode
  isDrawerOpen: boolean
  splitCodeView: SplitCodeView
}

export interface IAppSettings {
  users?: IUser[]
  roots?: number[]
  admins: number[]
}

export interface IRouteInfo {
  path: string
  name: string
  component: ComponentType<any>
  exact?: boolean
  icon?: string
  hidden?: boolean
  authRequired?: boolean
  sidebar?: boolean
}

export interface IShortProject {
  id: number
  owner: string
  name: string
}

export interface IProject {
  id: number
  state: string
  repoUrl: string
  creatorId: number
  repoOwner: string
  repoName: string
  repoDescription?: string
  repoOwnerAvatar: string
  repoOpenPullCount: number
  adminIds: number[]
  secondaryRepos: IShortProject[]
}

export interface ICloneStatistics {
  user: {
    url: string
    login: string
    avatar: string
  }
  cloneCount: number
  lineCount: number
}

export interface IProjectConf {
  admins: number[]
  cloneMinTokenCount: number
  fileMinSimilarityIndex: number
  excludedFiles: string[]
}

export interface IShortPull {
  number: number
  projectId: number
  url: string
  title: string
  open: boolean
  createdAt: string
  updatedAt: string
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
  status: string
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
  pullNumber: number
  owner: string
  repo: string
  sha: string
  file: string
  fromLine: number
  toLine: number
  content: string
  pullUrl: string
  commitUrl: string
  fileUrl: string
}

export interface IClone {
  id: number
  projectId: number
  target: ICodeSnippet
  source: ICodeSnippet
}

export interface IDiff {
  baseFilename?: string
  baseContent?: string
  headFilename?: string
  headContent?: string
}
