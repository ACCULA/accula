import React, { useEffect, useState } from 'react'
import { useSnackbar } from 'notistack'
import { historyPush } from 'utils'
import { useHistory } from 'react-router'
import { AppDispatch, AppState } from 'store'
import { bindActionCreators } from 'redux'
import { connect, ConnectedProps } from 'react-redux'
import { Helmet } from 'react-helmet'
import { AddBoxOutlined, CloseRounded } from '@material-ui/icons'
import { getProjectsAction } from 'store/projects/actions'
import Button from '@material-ui/core/Button'
import BreadCrumbs from 'components/BreadCrumbs'
import Tabs from 'components/Tabs'
import Table from 'components/Table'
import { IProject } from 'types'
import { HeadCell } from 'components/Table/TableHeader/TableHeader'
import layersImg from 'images/layers.svg'
import GitHubIcon from '@material-ui/icons/GitHub'
import { Avatar, IconButton, TableCell } from '@material-ui/core'
import { StyledTableRow } from 'components/Table/styles'
import AddProjectDialog from './AddProjectDialog'
import { useStyles } from './styles'

type ProjectsProps = PropsFromRedux

const headCells: HeadCell<IProject>[] = [
  { id: 'repoName', numeric: false, disablePadding: false, label: 'Name' },
  { id: 'repoOpenPullCount', numeric: true, disablePadding: false, label: 'Open pr' },
  { id: 'repoUrl', numeric: true, disablePadding: false, label: '' }
]

const Projects = ({ user, projects, getProjects }: ProjectsProps) => {
  const history = useHistory()
  const { enqueueSnackbar, closeSnackbar } = useSnackbar()
  const classes = useStyles()
  const [isCreateProjectDialogOpen, setCreateProjectDialogOpen] = useState(false)

  useEffect(() => {
    getProjects(msg =>
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

  if (!projects) {
    return <></>
  }

  const addProjectDialog = (
    <AddProjectDialog
      open={isCreateProjectDialogOpen}
      onClose={() => setCreateProjectDialogOpen(false)}
    />
  )

  if (projects.length === 0) {
    return (
      <div className={classes.emptyContent}>
        <img className={classes.layersImg} src={layersImg} alt="Projects" />
        <span className={classes.projectsText}>Projects</span>
        {user && (
          <>
            <Button
              className={classes.addProjectBtn}
              variant="contained"
              color="secondary"
              onClick={() => setCreateProjectDialogOpen(true)}
            >
              Add project
            </Button>
            {addProjectDialog}
          </>
        )}
      </div>
    )
  }

  return (
    <div>
      <Helmet>
        <title>Projects - ACCULA</title>
      </Helmet>
      <BreadCrumbs breadcrumbs={[{ text: 'Projects' }]} />
      <Tabs />
      <Table<IProject>
        headCells={headCells}
        toolBarTitle=""
        toolBarButtons={[
          {
            toolTip: 'Add project',
            iconButton: <AddBoxOutlined />,
            onClick: () => setCreateProjectDialogOpen(true)
          }
        ]}
      >
        {projects.map(project => (
          <StyledTableRow
            hover
            onClick={() => historyPush(history, `projects/${project.id}/pulls`)}
            tabIndex={-1}
            key={project.id}
          >
            <TableCell align="left">
              <div className={classes.repoInfo}>
                <Avatar
                  className={classes.repoAvatarImg}
                  src={project.repoOwnerAvatar}
                  alt={project.repoOwner}
                />
                <div className={classes.repoFullName}>
                  <span
                    className={classes.cellText}
                  >{`${project.repoOwner}/${project.repoName}`}</span>
                  {project.repoDescription !== '' && (
                    <span className={classes.repoDescription}>{project.repoDescription}</span>
                  )}
                </div>
              </div>
            </TableCell>
            <TableCell align="right">
              <span className={classes.cellText}>{project.repoOpenPullCount}</span>
            </TableCell>
            <TableCell align="right">
              <a
                href={project.repoUrl}
                target="_blank"
                rel="noopener noreferrer"
                className={classes.repoUrlImg}
              >
                <IconButton color="default" aria-label="Project Url">
                  <GitHubIcon />
                </IconButton>
              </a>
            </TableCell>
          </StyledTableRow>
        ))}
      </Table>
      {addProjectDialog}
    </div>
  )
}

const mapStateToProps = (state: AppState) => ({
  user: state.users.user.value,
  projects: state.projects.projects.value
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getProjects: bindActionCreators(getProjectsAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(Projects)
