import { ComponentType } from 'react'

export interface RouteInfo {
  path: string
  exact?: boolean
  name: string
  icon: string
  component: ComponentType<any>
  hidden?: boolean
}

export interface Project {
  id: number
  url: string
  owner: string
  name: string
  description?: string
  avatarUrl: string
  openPullRequestCount: number
}

export interface ProjectSettings {
  
}

export interface PullRequest {
  
}