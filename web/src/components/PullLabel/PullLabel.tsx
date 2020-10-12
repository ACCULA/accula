import React from 'react'
import clsx from 'clsx'
import { Tooltip } from '@material-ui/core'
import { useStyles } from './styles'

interface PullLabel {
  text: string
  className?: string
}

const PullLabel = ({ text, className }: PullLabel) => {
  const classes = useStyles()

  return (
    <Tooltip placement="top" title={text}>
      <code className={clsx(className, classes.label)}>{text}</code>
    </Tooltip>
  )
}

export default PullLabel
