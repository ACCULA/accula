import React from 'react'
import clsx from 'clsx'
import Tooltip from '@material-ui/core/Tooltip'
import { format, formatDistanceToNow } from 'date-fns'
import { DATE_TITLE_FORMAT } from 'utils'
import { useStyles } from './styles'

interface PullDateProps {
  date: string
  className?: string
}

const PullDate = ({ date, className }: PullDateProps) => {
  const classes = useStyles()

  return (
    <Tooltip title={`${format(new Date(date), DATE_TITLE_FORMAT)}`}>
      <span className={clsx(className, classes.dataText)}>
        {formatDistanceToNow(new Date(date), { addSuffix: true })}
      </span>
    </Tooltip>
  )
}

export default PullDate
