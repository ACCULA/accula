import React from 'react'
import clsx from 'clsx'
import { useStyles } from './styles'

interface EmptyContentProps {
  Icon: React.FunctionComponent<React.SVGProps<SVGSVGElement>>
  info: string
  className?: string
  children?: React.ReactNode
}

const EmptyContent = ({ className, Icon, info, children }: EmptyContentProps) => {
  const classes = useStyles()

  return (
    <div className={clsx(className, classes.emptyContent)}>
      <Icon className={classes.image} />
      <span className={classes.info}>{info}</span>
      {children}
    </div>
  )
}

export default EmptyContent
