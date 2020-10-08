import React, { useState } from 'react'
import {
  Paper,
  TableContainer,
  Table as MuiTable,
  TableBody,
  TablePagination
} from '@material-ui/core'
import { useStyles } from './styles'
import TableToolbar, { ToolBarButton } from './TableToolbar/TableToolbar'
import TableHeader, { HeadCell } from './TableHeader/TableHeader'

interface TableProps<DataItem> {
  headCells: HeadCell<DataItem>[]
  toolBarTitle: string
  count: number
  withPagination?: boolean
  startPage?: number
  toolBarButtons?: ToolBarButton[]
  children?(data: { page: number; rowsPerPage: number } | undefined): React.ReactNode
}

const Table = <DataItem extends object>({
  headCells,
  toolBarTitle,
  toolBarButtons,
  count,
  withPagination,
  startPage,
  children
}: TableProps<DataItem>) => {
  const classes = useStyles()
  const [page, setPage] = useState(startPage || 0)
  const [rowsPerPage, setRowsPerPage] = useState(10)

  const handleChangePage = (event: unknown, newPage: number) => {
    setPage(newPage)
  }

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10))
    setPage(0)
  }

  const childrenData = withPagination ? { page, rowsPerPage } : undefined

  return (
    <div className={classes.root}>
      {toolBarTitle && toolBarButtons && (
        <TableToolbar title={toolBarTitle} toolBarButtons={toolBarButtons} />
      )}
      <Paper className={classes.paper}>
        <TableContainer>
          <MuiTable className={classes.table} aria-label={toolBarTitle}>
            <TableHeader headCells={headCells} />
            {children && <TableBody>{children(childrenData)}</TableBody>}
          </MuiTable>
        </TableContainer>
        {withPagination && (
          <TablePagination
            className={classes.pagination}
            rowsPerPageOptions={[5, 10, 25]}
            component="div"
            count={count}
            rowsPerPage={rowsPerPage}
            page={page}
            onChangePage={handleChangePage}
            onChangeRowsPerPage={handleChangeRowsPerPage}
          />
        )}
      </Paper>
    </div>
  )
}

export default Table
