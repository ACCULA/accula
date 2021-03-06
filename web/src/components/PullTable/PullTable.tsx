import React, { useEffect, useState } from 'react'
import { IProject, IShortPull } from 'types'
import { Avatar } from '@material-ui/core'
import { HeadCell } from 'components/Table/TableHeader/TableHeader'
import Table from 'components/Table/Table'
import PullStatus from 'components/PullStatus/PullStatus'
import PullDate from 'components/PullDate/PullDate'
import TableRow from 'components/Table/TableRow'
import TableCell from 'components/Table/TableCell'
import { useStyles } from './styles'

type ExcludeType = ('number' | 'title' | 'open' | 'createdAt' | 'updatedAt' | 'author')[]

const headCells: HeadCell<IShortPull>[] = [
  { id: 'number', numeric: true, disablePadding: false, label: '#' },
  { id: 'title', numeric: false, disablePadding: false, label: 'Name' },
  { id: 'open', numeric: false, disablePadding: false, label: 'Status' },
  { id: 'createdAt', numeric: false, disablePadding: false, label: 'Created' },
  { id: 'updatedAt', numeric: false, disablePadding: false, label: 'Last Updated' },
  { id: 'author', numeric: false, disablePadding: false, label: 'Author' }
]

interface PullTableProps {
  project: IProject
  pulls: IShortPull[]
  exclude?: ExcludeType
}

const PullTable = ({ project, pulls, exclude }: PullTableProps) => {
  const classes = useStyles()
  const [headers, setHeaders] = useState<HeadCell<IShortPull>[]>(headCells)

  useEffect(() => {
    if (exclude) {
      setHeaders(headCells.filter((h: any) => !exclude.includes(h.id)))
    }
  }, [exclude])

  return (
    <Table<IShortPull> count={pulls.length} headCells={headers} toolBarTitle="" withPagination>
      {({ page, rowsPerPage }) => (
        <>
          {pulls.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage).map(pull => (
            <TableRow key={pull.number} to={`/projects/${project.id}/pulls/${pull.number}`}>
              {(exclude === undefined || !exclude.includes('number')) && (
                <TableCell align="right">
                  <span className={classes.dataText}>{pull.number}</span>
                </TableCell>
              )}
              {(exclude === undefined || !exclude.includes('title')) && (
                <TableCell align="left">
                  <span className={classes.dataText}>{pull.title}</span>
                </TableCell>
              )}
              {(exclude === undefined || !exclude.includes('open')) && (
                <TableCell align="left">
                  <PullStatus open={pull.open} />
                </TableCell>
              )}
              {(exclude === undefined || !exclude.includes('createdAt')) && (
                <TableCell align="left">
                  <PullDate date={pull.createdAt} />
                </TableCell>
              )}
              {(exclude === undefined || !exclude.includes('updatedAt')) && (
                <TableCell align="left">
                  <PullDate date={pull.updatedAt} />
                </TableCell>
              )}
              {(exclude === undefined || !exclude.includes('author')) && (
                <TableCell align="left">
                  <div className={classes.authorInfo}>
                    <Avatar
                      className={classes.authorAvatar}
                      src={pull.author.avatar}
                      alt={project.repoOwner}
                    />
                    <span className={classes.dataText}>{pull.author.login}</span>
                  </div>
                </TableCell>
              )}
            </TableRow>
          ))}
        </>
      )}
    </Table>
  )
}

export default PullTable
