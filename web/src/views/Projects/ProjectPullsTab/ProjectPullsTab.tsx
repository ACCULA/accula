import React, { useEffect } from 'react'
import { format, formatDistanceToNow } from 'date-fns'
import { useHistory } from 'react-router'
import { AppDispatch, AppState } from 'store'
import { bindActionCreators } from 'redux'
import { connect, ConnectedProps } from 'react-redux'
import { getPullsAction } from 'store/pulls/actions'
import { IProject, IShortPull } from 'types'
import { useSnackbar } from 'notistack'
import { Avatar, IconButton, TableCell, Tooltip } from '@material-ui/core'
import { CloseRounded } from '@material-ui/icons'
import { HeadCell } from 'components/Table/TableHeader/TableHeader'
import Table from 'components/Table/Table'
import { StyledTableRow } from 'components/Table/styles'
import { historyPush } from 'utils'
import prImage from 'images/pull_request.svg'
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
interface ProjectPullsTabProps extends PropsFromRedux {
  project: IProject
  pulls: IShortPull[]
}

const ProjectPullsTab = ({ project, pulls, getPulls }: ProjectPullsTabProps) => {
  const history = useHistory()
  const classes = useStyles()
  const { enqueueSnackbar, closeSnackbar } = useSnackbar()
  useEffect(() => {
    getPulls(project.id, msg =>
      enqueueSnackbar(msg, {
        variant: 'error',
        action: key => (
          <IconButton onClick={() => closeSnackbar(key)} aria-label="Close notification">
            <CloseRounded />
          </IconButton>
        )
      })
    )
    // eslint-disable-next-line
  }, [])

  if (!pulls) {
    return <></>
  }

  if (pulls.length === 0) {
    return (
      <div className={classes.emptyContent}>
        <img className={classes.prImage} src={prImage} alt="Pull request" />
        <span className={classes.prText}>No pull requests</span>
      </div>
    )
  }

  return (
    <Table<IShortPull> headCells={headCells} toolBarTitle="">
      {pulls.map(pull => (
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
    </Table>
  )
}

const mapStateToProps = (state: AppState) => ({
  pulls: state.pulls.pulls.value
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getPulls: bindActionCreators(getPullsAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)
type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(ProjectPullsTab)

/* <Table striped bordered hover responsive>
          <thead>
            <tr className="project-pull">
              <th className="id">#</th>
              <th>Pull Request</th>
              <th>Author</th>
            </tr>
          </thead>
          <tbody>
            {pulls.value &&
              pulls.value.map(pull => (
                <LinkContainer
                  to={`/projects/${project.value.id}/pulls/${pull.number}`}
                  key={pull.number}
                >
                  <tr className="project-pull pointer">
                    <td className="id">{pull.number}</td>
                    <td>
                      {pull.open ? (
                        <Badge className="badge-success">Open</Badge> //
                      ) : (
                        <Badge className="badge-danger">Closed</Badge>
                      )}
                      {pull.title}
                    </td>
                    <td className="avatar">
                      <img
                        className="border-gray"
                        src={pull.author.avatar}
                        alt={pull.author.login}
                      />
                      {pull.author.login}
                    </td>
                  </tr>
                </LinkContainer>
              ))}
          </tbody>
        </Table> */
