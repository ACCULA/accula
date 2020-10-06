import { IconButton, Toolbar, Tooltip, Typography } from '@material-ui/core'
import React from 'react'
import { useStyles } from './styles'

export interface ToolBarButton {
  onClick: () => void
  toolTip?: string
  iconButton: React.ReactNode
}

interface TableToolbarProps {
  title: string
  toolBarButtons?: ToolBarButton[]
}

const TableToolbar = ({ title, toolBarButtons }: TableToolbarProps) => {
  const classes = useStyles()

  return (
    <Toolbar className={classes.root}>
      <Typography className={classes.title} variant="h6" id="tableTitle" component="div">
        {title}
      </Typography>
      {toolBarButtons.map(({ onClick, toolTip, iconButton }, index) => {
        const button = (
          <IconButton onClick={() => onClick()} aria-label={toolTip}>
            {iconButton}
          </IconButton>
        )
        return (
          <div key={index}>
            {toolTip ? <Tooltip title={toolTip}>{button}</Tooltip> : <>{button}</>}
          </div>
        )
      })}
    </Toolbar>
  )
}

export default TableToolbar
