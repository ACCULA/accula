import React from 'react'
import { Box, TableCell as Cell, TableCellProps as CellProps } from '@material-ui/core'

interface TableCellProps extends CellProps {}

const TableCell = ({ component, ...props }: TableCellProps) => {
  if (component) {
    return (
      <Cell component={component} {...props}>
        {props.children}
      </Cell>
    )
  }

  return (
    <Cell component={Box} {...props}>
      {props.children}
    </Cell>
  )
}

export default TableCell
