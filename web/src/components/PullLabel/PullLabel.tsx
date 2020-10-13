import React from 'react'
import clsx from 'clsx'
import { Tooltip } from '@material-ui/core'
import { useStyles } from './styles'

interface PullLabel {
  text: string
  className?: string
  type?: 'added' | 'removed'
}

const PullLabel = ({ text, className, type }: PullLabel) => {
  const classes = useStyles()
  let labelClassName: string
  if (type === 'added') {
    labelClassName = classes.addedLabel
  } else if (type === 'removed') {
    labelClassName = classes.removedLabel
  }
  return (
    <Tooltip placement="top" title={text}>
      <code className={clsx(className, classes.label, labelClassName)}>{text}</code>
    </Tooltip>
  )
}

export default PullLabel
