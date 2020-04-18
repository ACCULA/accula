import { ComponentType } from 'react'

export interface RouteInfo {
  path: string
  exact?: boolean
  name: string
  icon: string
  component: ComponentType<any>
  hide?: boolean
}
