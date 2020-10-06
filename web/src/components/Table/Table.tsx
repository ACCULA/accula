import React from 'react'
import { Paper, TableContainer, Table as MuiTable, TableBody } from '@material-ui/core'
import TableHeader, { HeadCell } from './components/TableHeader/TableHeader'
import TableToolbar, { ToolBarButton } from './components/TableToolbar/TableToolbar'
import { useStyles } from './styles'

interface TableProps<DataItem> {
  headCells: HeadCell[]
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
