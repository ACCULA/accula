import React from 'react'
import { format, formatDistanceToNow } from 'date-fns'
import { useHistory } from 'react-router'

import { IProject, IShortPull } from 'types'
import { Avatar, TableCell, Tooltip } from '@material-ui/core'
import { HeadCell } from 'components/Table/TableHeader/TableHeader'
import Table from 'components/Table/Table'
import { StyledTableRow } from 'components/Table/styles'
import { historyPush } from 'utils'
import { ReactComponent as PrLogo } from 'images/pull_request.svg'
import { useStyles } from './styles'

const DATE_TITLE_FORMAT = "d MMMM yyyy 'at' HH:mm"

const headCells: HeadCell<IShortPull>[] = [
  { id: 'number', numeric: true, disablePadding: false, label: '#' },
  { id: 'title', numeric: false, disablePadding: false, label: 'Name' },
  { id: 'open', numeric: false, disablePadding: false, label: 'Status' },
  { id: 'createdAt', numeric: false, disablePadding: false, label: 'Created' },
  { id: 'updatedAt', numeric: false, disablePadding: false, label: 'Last Updated' },
  { id: 'author', numeric: false, disablePadding: false, label: 'Author' }
]
interface ProjectPullsTabProps {
  project: IProject
  pulls: IShortPull[]
}

const ProjectPullsTab = ({ project, pulls }: ProjectPullsTabProps) => {
  const history = useHistory()
  const classes = useStyles()

  if (!pulls) {
    return <></>
  }

  if (pulls.length === 0) {
    return (
      <div className={classes.emptyContent}>
        <PrLogo className={classes.prImage} />
        <span className={classes.prText}>No pull requests</span>
      </div>
    )
  }

  return (
    <Table<IShortPull> count={pulls.length} headCells={headCells} toolBarTitle="" withPagination>
      {({ page, rowsPerPage }) => (
        <>
          {pulls.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage).map(pull => (
            <StyledTableRow
              hover
              onClick={() => historyPush(history, `/projects/${project.id}/pulls/${pull.number}`)}
              tabIndex={-1}
              key={pull.number}
            >
              <TableCell align="right">
                <span className={classes.dataText}>{pull.number}</span>
              </TableCell>
              <TableCell align="left">
                <span className={classes.dataText}>{pull.title}</span>
              </TableCell>
              <TableCell align="left">
                {pull.open ? (
                  <Tooltip title="Open">
                    <div className={`${classes.blob} ${classes.blobGreen}`}></div>
                  </Tooltip>
                ) : (
                  <Tooltip title="Close">
                    <div className={`${classes.blob} ${classes.blobRed}`}></div>
                  </Tooltip>
                )}
              </TableCell>
              <TableCell align="left">
                <Tooltip title={`${format(new Date(pull.createdAt), DATE_TITLE_FORMAT)}`}>
                  <span className={classes.dataText}>
                    {formatDistanceToNow(new Date(pull.createdAt), { addSuffix: true })}
                  </span>
                </Tooltip>
              </TableCell>
              <TableCell align="left">
                <Tooltip title={`${format(new Date(pull.updatedAt), DATE_TITLE_FORMAT)}`}>
                  <span className={classes.dataText}>
                    {formatDistanceToNow(new Date(pull.updatedAt), { addSuffix: true })}
                  </span>
                </Tooltip>
              </TableCell>
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
            </StyledTableRow>
          ))}
        </>
      )}
    </Table>
  )
}

export default ProjectPullsTab
