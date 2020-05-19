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
  exact?: boolean
  name: string
  icon?: string
  component: ComponentType<any>
  hidden?: boolean
}

export interface IProject {
  id: number
  repoUrl: string
  creatorId: number
  admins?: number[]
  repoOwner: string
  repoName: string
  repoDescription?: string
  repoOwnerAvatar: string
  repoOpenPullCount: number
}

export interface IProjectSettings {}

export interface IPullShort {
  id: number
  projectId: number
  title: string
  open: boolean
  cloneCount: number
}

export interface IPull {
  id: number
  projectId: number
  url: string
  source: {
    url: string
    label: string
  }
  target: {
    url: string
    label: string
  }
  author: {
    url: string
    login: string
    name: string
    avatar: string
  }
  title: string
  open: boolean
  createdAt: string
  updatedAt: string
  status: string
  cloneCount: number
  previousPulls: IPullShort[]
}

interface IClone {
  
}
