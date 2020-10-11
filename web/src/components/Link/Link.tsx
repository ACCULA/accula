import React from 'react'
import { Link as RelativeLink } from 'react-router-dom'
import isExternal from 'is-url-external'
import clsx from 'clsx'
import { useStyles } from './styles'

export interface LinkProps {
  to: string
  className?: string
  children: React.ReactNode
  [x: string]: any
}

const Link = ({ to, children, className, props }: LinkProps) => {
  const classes = useStyles()
  return isExternal(to) ? (
    <a
      href={to} //
      target="_blank"
      rel="noopener noreferrer"
      className={clsx(classes.root, className)}
    >
      {children}
    </a>
  ) : (
    <RelativeLink to={to} {...props}>
      {children}
    </RelativeLink>
  )
}

export default Link
