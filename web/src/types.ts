import { ComponentType } from 'react'

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
  url: string
  owner: string
  name: string
  description?: string
  avatar: string
  openPullRequestCount: number
}

export interface ProjectSettings {}

export interface PullRequest {
  id: number
  url: string
  title: string
  body: string
  open: boolean
  createdAt: string
  updatedAt: string
  author: {
    url: string
    login: string
    avatar: string
  }
}
