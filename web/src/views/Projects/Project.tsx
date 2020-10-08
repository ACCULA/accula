import React, { useEffect } from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { useHistory, useParams } from 'react-router-dom'
import { bindActionCreators } from 'redux'
import { AppDispatch, AppState } from 'store'
import {
  getProjectAction,
  getProjectConfAction,
  getRepoAdminsAction,
  updateProjectConfAction
} from 'store/projects/actions'
import { getPullsAction } from 'store/pulls/actions'
import { historyPush, isProjectAdmin } from 'utils'
import { PageTitle } from 'components/PageTitle'
import BreadCrumbs from 'components/BreadCrumbs'
import Tabs, { Tab } from 'components/Tabs/Tabs'
import { useSnackbar } from 'notistack'
import { CircularProgress, IconButton } from '@material-ui/core'
import { CloseRounded } from '@material-ui/icons'
import { ReactComponent as PrLogo } from 'images/pull_request.svg'
import ProjectPullsTab from './ProjectPullsTab/ProjectPullsTab'

interface ProjectProps extends PropsFromRedux {}

const Project = ({
  project,
  projectConf,
  user,
  pulls,
  getProject,
  getProjectConf,
  getPulls
}: ProjectProps) => {
  const history = useHistory()
  const { enqueueSnackbar, closeSnackbar } = useSnackbar()
  const { prId, tab }: any = useParams()
  const projectId = parseInt(prId, 10)

  const showErrorNotification = (msg: string) =>
    enqueueSnackbar(msg, {
      variant: 'error',
      action: key => (
        <IconButton onClick={() => closeSnackbar(key)} aria-label="Close notification">
          <CloseRounded />
        </IconButton>
      )
    })

  useEffect(() => {
    getProject(projectId, showErrorNotification)
    getProjectConf(projectId, showErrorNotification)
    getPulls(projectId, showErrorNotification)
    // eslint-disable-next-line
  }, [projectId])

  if (!user || !project || !projectConf) {
    return <></>
  }

  const handleChangeTab = (t: Tab) => {
    historyPush(history, `/projects/${projectId}/${t.id}`)
  }

  const isAdmin = isProjectAdmin(user, project, projectConf)
  const tabs = [
    {
      id: 'pulls',
      text: 'Pull requests',
      Icon: PrLogo,
      badgeValue: pulls ? (
        pulls.filter(pull => pull.open).length
      ) : (
        <CircularProgress size={12} color="inherit" />
      )
    }
  ] as Tab[]

  if (isAdmin) {
    tabs.push({ id: 'configuration', text: 'Configuration' })
  }

  return (
    <div>
      <BreadCrumbs
        breadcrumbs={[{ text: 'Projects', to: '/projects' }, { text: project.repoName }]}
      />
      <PageTitle title={project && project.repoName} />
      <Tabs tabs={tabs} onChange={handleChangeTab} />
      {tab === 'pulls' && <ProjectPullsTab project={project} pulls={pulls} />}
      {isAdmin && tab === 'configuration' && (
        <></>
        // <ProjectConfigurationTab
        //   project={project} //
        // />
      )}
    </div>
  )
}

const mapStateToProps = (state: AppState) => ({
  project: state.projects.project.value,
  projectConf: state.projects.projectConf.value,
  updateProjectConfState: state.projects.updateProjectConf,
  repoAdmins: state.projects.repoAdmins,
  pulls: state.pulls.pulls.value,
  user: state.users.user.value
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getProject: bindActionCreators(getProjectAction, dispatch),
  getProjectConf: bindActionCreators(getProjectConfAction, dispatch),
  getRepoAdmins: bindActionCreators(getRepoAdminsAction, dispatch),
  getPulls: bindActionCreators(getPullsAction, dispatch),
  updateProjectConf: bindActionCreators(updateProjectConfAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)
type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(Project)
