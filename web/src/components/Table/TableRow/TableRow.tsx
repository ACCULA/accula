import React from 'react'
import clsx from 'clsx'
import { Link } from 'react-router-dom'
import { Box, TableRow as Row, TableRowProps as RowProps } from '@material-ui/core'
import { useStyles } from '../styles'

interface TableRowProps extends RowProps {
  to?: string
}

const TableRow = ({ to, ...props }: TableRowProps) => {
  const classes = useStyles()

  if (to) {
    return (
      <Row
        hover
        className={clsx(props.className, classes.tableRow)}
        tabIndex={-1}
        component={Link}
        to={to}
        // Todo: error if pass here {...props}
      >
        {props.children}
      </Row>
    )
  }

  return (
    <Row
      hover
      className={clsx(props.className, classes.tableRow)}
      tabIndex={-1}
      component={Box}
      {...props}
    >
      {props.children}
    </Row>
  )
}

export default TableRow
