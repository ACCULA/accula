import React from 'react'
import clsx from 'clsx'
import { useStyles } from './styles'

export interface LinkProps {
  to: string
  className?: string
  children: React.ReactNode
}

const Link = ({ to, children, className }: LinkProps) => {
  const classes = useStyles()
  return (
    <a
      href={to} //
      target="_blank"
      rel="noopener noreferrer"
      className={clsx(classes.root, className)}
    >
      {children}
    </a>
  )
}

export default Link
