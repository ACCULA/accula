import React from 'react'
import clsx from 'clsx'
import Tooltip from '@material-ui/core/Tooltip'
import { useStyles } from './styles'

interface PullStatusProps {
  open: boolean
  className?: string
}

const PullStatus = ({ open, className }: PullStatusProps) => {
  const classes = useStyles()
  const title = open ? 'Open' : 'Close'
  const clsn = clsx(className, classes.blob, open ? classes.blobGreen : classes.blobRed)

  return (
    <Tooltip title={title}>
      <div className={clsn}></div>
    </Tooltip>
  )
}

export default PullStatus
