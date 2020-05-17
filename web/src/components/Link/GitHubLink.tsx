import React from 'react'
import { Link, LinkProps } from './Link'

export const GitHubLink = (props: LinkProps) => {
  return (
    <Link {...props} black>
      <i className="fab fa-fw fa-github" /> {props.children}
    </Link>
  )
}
