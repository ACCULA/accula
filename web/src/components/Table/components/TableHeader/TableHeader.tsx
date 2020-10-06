import React from 'react'
import { TableHead, TableRow } from '@material-ui/core'
import { StyledTableCell } from './styles'

export interface HeadCell {
  disablePadding: boolean
  id: string
  label: string
  numeric: boolean
}

interface TableHeaderProps<DataItem> {
  headCells: HeadCell[]
}

const TableHeader = <DataItem extends object>({ headCells }: TableHeaderProps<DataItem>) => {
  return (
    <TableHead>
      <TableRow>
        {headCells.map(headCell => (
          <StyledTableCell
            key={headCell.label}
            align={headCell.numeric ? 'right' : 'left'}
            padding={headCell.disablePadding ? 'none' : 'default'}
          >
            {headCell.label}
          </StyledTableCell>
        ))}
      </TableRow>
    </TableHead>
  )
}

export default TableHeader
