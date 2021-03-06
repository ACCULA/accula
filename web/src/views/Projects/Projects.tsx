import React, { useEffect, useState } from 'react'
import { useSnackbar } from 'notistack'
import { AppDispatch, AppState } from 'store'
import { bindActionCreators } from 'redux'
import { connect, ConnectedProps } from 'react-redux'
import { AddBoxOutlined } from '@material-ui/icons'
import { getProjectsAction, resetProjectsAction } from 'store/projects/actions'
import Button from '@material-ui/core/Button'
import BreadCrumbs from 'components/BreadCrumbs'
import Tabs from 'components/Tabs'
import Table from 'components/Table'
import { IProject, hasAtLeastAdminRole } from 'types'
import { HeadCell } from 'components/Table/TableHeader/TableHeader'
import { ReactComponent as LayersImg } from 'images/layers.svg'
import { PageTitle } from 'components/PageTitle'
import GitHubIcon from '@material-ui/icons/GitHub'
import { Avatar, IconButton } from '@material-ui/core'
import { getNotifier } from 'App'
import EmptyContent from 'components/EmptyContent'
import TableRow from 'components/Table/TableRow'
import TableCell from 'components/Table/TableCell'
import AddProjectDialog from './AddProjectDialog'
import { useStyles } from './styles'

type ProjectsProps = PropsFromRedux

const headCells: HeadCell<IProject>[] = [
  { id: 'repoName', numeric: false, disablePadding: false, label: 'Name' },
  { id: 'repoOpenPullCount', numeric: true, disablePadding: false, label: 'Open PRs' },
  { id: 'repoUrl', numeric: true, disablePadding: false, label: '' }
]

const Projects = ({ user, projects, getProjects, resetProjects }: ProjectsProps) => {
  const classes = useStyles()
  const snackbarContext = useSnackbar()
  const [isGithubButtonHovered, setGithubButtonHovered] = useState(false)
  const [isCreateProjectDialogOpen, setCreateProjectDialogOpen] = useState(false)

  useEffect(() => {
    getProjects(getNotifier('error', snackbarContext))
    return () => {
      resetProjects()
    }
    // eslint-disable-next-line
  }, [])

  if (!projects) {
    return <></>
  }

  return (
    <>
      {projects.length === 0 ? (
        <EmptyContent className={classes.emptyContent} Icon={LayersImg} info="Projects">
          <>
            {hasAtLeastAdminRole(user) && (
              <>
                <Button
                  className={classes.addProjectBtn}
                  variant="contained"
                  color="secondary"
                  onClick={() => setCreateProjectDialogOpen(true)}
                >
                  Add project
                </Button>
              </>
            )}
          </>
        </EmptyContent>
      ) : (
        <div>
          <PageTitle title="Projects" />
          <BreadCrumbs breadcrumbs={[{ text: 'Projects' }]} />
          <Tabs />
          <Table<IProject>
            headCells={headCells}
            count={projects.length}
            toolBarTitle=""
            toolBarButtons={
              hasAtLeastAdminRole(user)
                ? [
                    {
                      toolTip: 'Add project',
                      iconButton: <AddBoxOutlined />,
                      onClick: () => setCreateProjectDialogOpen(true)
                    }
                  ]
                : []
            }
          >
            {() => (
              <>
                {projects.map(project => (
                  <TableRow
                    to={!isGithubButtonHovered ? `projects/${project.id}/pulls` : undefined}
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
                            <span className={classes.repoDescription}>
                              {project.repoDescription}
                            </span>
                          )}
                        </div>
                      </div>
                    </TableCell>
                    <TableCell align="right">
                      <span className={classes.cellText}>{project.repoOpenPullCount}</span>
                    </TableCell>
                    <TableCell align="right">
                      <IconButton
                        className={classes.repoUrlImg}
                        color="default"
                        aria-label="Project Url"
                        onFocus={() => setGithubButtonHovered(true)}
                        onMouseOver={() => setGithubButtonHovered(true)}
                        onMouseOut={() => setGithubButtonHovered(false)}
                        onBlur={() => setGithubButtonHovered(false)}
                        onClick={() => window.open(project.repoUrl, '_blank')}
                      >
                        <GitHubIcon />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </>
            )}
          </Table>
        </div>
      )}
      <AddProjectDialog
        open={isCreateProjectDialogOpen}
        onClose={() => setCreateProjectDialogOpen(false)}
      />
    </>
  )
}

const mapStateToProps = (state: AppState) => ({
  user: state.users.user.value,
  projects: state.projects.projects.value
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getProjects: bindActionCreators(getProjectsAction, dispatch),
  resetProjects: bindActionCreators(resetProjectsAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(Projects)
