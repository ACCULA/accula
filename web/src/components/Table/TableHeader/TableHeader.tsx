import React from 'react'
import { Box, TableHead } from '@material-ui/core'
import TableRow from '../TableRow'
import TableCell from '../TableCell'
import { useStyles } from './styles'

export interface HeadCell<DataItem> {
  disablePadding: boolean
  id: keyof DataItem
  label: string
  numeric: boolean
}

interface TableHeaderProps<DataItem> {
  headCells: HeadCell<DataItem>[]
}

const TableHeader = <DataItem extends object>({ headCells }: TableHeaderProps<DataItem>) => {
  const classes = useStyles()
  return (
    <TableHead component={Box}>
      <TableRow className={classes.tableHeadRow} hover={false}>
        {headCells.map(headCell => (
          <TableCell
            key={headCell.label}
            align={headCell.numeric ? 'right' : 'left'}
            padding={headCell.disablePadding ? 'none' : 'default'}
            classes={{ head: classes.tableHeadCell }}
          >
            {headCell.label}
          </TableCell>
        ))}
      </TableRow>
    </TableHead>
  )
}

export default TableHeader
