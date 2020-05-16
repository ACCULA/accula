import { ComponentType } from 'react'

export interface Token {
  accessToken: string
}

export interface User {
  id: number
  login: string
  name: string
}

export interface RouteInfo {
  path: string
  exact?: boolean
  name: string
  icon?: string
  component: ComponentType<any>
  hidden?: boolean
}

export interface Project {
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

export interface ProjectSettings {}

export interface PullRequest {
  id: number
  projectId: number
  pullUrl: string
  base: {
    url: string
    label: string
    sha: string
  }
  fork: {
    url: string
    label: string
    sha: string
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
}
