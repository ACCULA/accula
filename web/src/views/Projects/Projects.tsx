import React, { useEffect } from 'react'
import { useSnackbar } from 'notistack'
import { bindActionCreators } from 'redux'
import { connect, ConnectedProps } from 'react-redux'
import { Helmet } from 'react-helmet'
import { AddBoxOutlined } from '@material-ui/icons'
import { AppDispatch, AppState } from 'store'
import { getProjectsAction } from 'store/projects/actions'
import Button from '@material-ui/core/Button'
import BreadCrumbs from 'components/BreadCrumbs'
import Tabs from 'components/Tabs'
import Table from 'components/Table'
import { HeadCell } from 'components/Table/components/TableHeader/TableHeader'
import { IProject } from 'types'
import layersImg from 'images/layers.svg'
import GitHubIcon from '@material-ui/icons/GitHub'
import { Avatar, IconButton, TableCell } from '@material-ui/core'
import { StyledTableRow } from 'components/Table/styles'
import { useStyles } from './styles'

type ProjectsProps = PropsFromRedux

const headCells: HeadCell<IProject>[] = [
  { id: 'repoName', numeric: false, disablePadding: false, label: 'Name' },
  { id: 'repoOpenPullCount', numeric: true, disablePadding: false, label: 'Open pr' },
  { id: 'repoUrl', numeric: true, disablePadding: false, label: '' }
]

const Projects = ({ projects, getProjects }: ProjectsProps) => {
  const { enqueueSnackbar } = useSnackbar()
  const classes = useStyles()

  useEffect(() => {
    getProjects(message => enqueueSnackbar(message, { variant: 'error' }))
    // eslint-disable-next-line
  }, [getProjects])

  if (!projects) {
    return <></>
  }

  if (projects.length === 0) {
    return (
      <div className={classes.emptyContent}>
        <img className={classes.layersImg} src={layersImg} alt="Projects" />
        <span className={classes.projectsText}>Projects</span>
        <Button className={classes.addProjectBtn} variant="contained" color="secondary">
          Add project
        </Button>
      </div>
    )
  }

  return (
    <div>
      <Helmet>
        <title>Projects - ACCULA</title>
      </Helmet>
      <div className={classes.breadcrumbs}>
        <BreadCrumbs breadcrumbs={[{ text: 'Projects' }]} />
      </div>
      <div className={classes.tabs}>
        <Tabs />
      </div>
      <Table<IProject>
        headCells={headCells}
        toolBarTitle=""
        toolBarButtons={[
          {
            toolTip: 'Add project',
            iconButton: <AddBoxOutlined />,
            onClick: () => console.log('Add project')
          }
        ]}
      >
        {projects.map(project => (
          <StyledTableRow
            hover
            onClick={() => console.log('Click on', project.repoName)}
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
                  <span className={classes.repoDescription}>{project.repoDescription || ''}</span>
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
    </div>
  )
}

const mapStateToProps = (state: AppState) => ({
  projects: state.projects.projects.value
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getProjects: bindActionCreators(getProjectsAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(Projects)
