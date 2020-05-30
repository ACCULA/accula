import React from 'react'
import { Link, LinkProps } from './Link'

export const GitHubLink = (props: LinkProps) => {
  const { children, ...linkProps } = props
  return (
    <Link {...linkProps} black>
      <i className="fab fa-fw fa-github" /> {children}
    </Link>
  )
}
