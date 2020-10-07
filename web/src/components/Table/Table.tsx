import React from 'react'
import { Paper, TableContainer, Table as MuiTable, TableBody } from '@material-ui/core'
import { useStyles } from './styles'
import TableToolbar, { ToolBarButton } from './TableToolbar/TableToolbar'
import TableHeader, { HeadCell } from './TableHeader/TableHeader'

interface TableProps<DataItem> {
  headCells: HeadCell<DataItem>[]
  toolBarTitle: string
  toolBarButtons?: ToolBarButton[]
  children?: React.ReactNode
}

const Table = <DataItem extends object>({
  headCells,
  toolBarTitle,
  toolBarButtons,
  children
}: TableProps<DataItem>) => {
  const classes = useStyles()
  return (
    <div className={classes.root}>
      <TableToolbar title={toolBarTitle} toolBarButtons={toolBarButtons} />
      <Paper className={classes.paper}>
        <TableContainer>
          <MuiTable className={classes.table} aria-label={toolBarTitle}>
            <TableHeader headCells={headCells} />
            <TableBody>{children}</TableBody>
          </MuiTable>
        </TableContainer>
      </Paper>
    </div>
  )
}

export default Table
