import React from 'react'
import clsx from 'clsx'
import { useStyles } from './styles'

interface PullLabel {
  text: string
  className?: string
}

const PullLabel = ({ text, className }: PullLabel) => {
  const classes = useStyles()

  return <code className={clsx(className, classes.label)}>{text}</code>
}

export default PullLabel
