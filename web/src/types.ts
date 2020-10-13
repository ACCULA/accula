import { ComponentType } from 'react'

export interface IToken {
  accessToken: string
}

export interface IUser {
  id: number
  login: string
  name: string
  avatar: string
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

export interface IProject {
  id: number
  repoUrl: string
  creatorId: number
  repoOwner: string
  repoName: string
  repoDescription?: string
  repoOwnerAvatar: string
  repoOpenPullCount: number
  adminIds: number[]
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
