import React from 'react'
import { Link as RelativeLink } from 'react-router-dom'
import isExternal from 'is-url-external'
import cx from 'classnames'

export interface LinkProps {
  to: string
  black?: boolean

  [x: string]: any
}

export const Link = ({ to, black, props, children }: LinkProps) => {
  const cls = cx({
    black
  })
  return isExternal(to) ? (
    <a
      href={to} //
      target="_blank"
      rel="noopener noreferrer"
      className={cls}
      {...props}
    >
      {children}
    </a>
  ) : (
    <RelativeLink
      to={to} //
      {...props}
    >
      {children}
    </RelativeLink>
  )
}
