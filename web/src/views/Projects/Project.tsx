import React, { useEffect } from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { useHistory, useParams } from 'react-router-dom'
import { bindActionCreators } from 'redux'
import { AppDispatch, AppState } from 'store'
import { getProjectAction, resetProjectInfo } from 'store/projects/actions'
import { getPullsAction, resetPullsInfo } from 'store/pulls/actions'
import { historyPush, isProjectAdmin } from 'utils'
import { PageTitle } from 'components/PageTitle'
import BreadCrumbs from 'components/BreadCrumbs'
import Tabs, { Tab } from 'components/Tabs/Tabs'
import { useSnackbar } from 'notistack'
import { getNotifier } from 'App'
import { CircularProgress } from '@material-ui/core'
import { ReactComponent as PrLogo } from 'images/pull_request.svg'
import SettingsIcon from '@material-ui/icons/Settings'
import ProjectPullsTab from './ProjectPullsTab/ProjectPullsTab'
import ProjectSettingsTab from './ProjectSettingsTab'

interface ProjectProps extends PropsFromRedux {}

const tabValues = ['pulls', 'settings'] as string[]

const validateTab = (tab: string) => tabValues.includes(tab) || tab === undefined

const Project = ({
  project,
  user,
  pulls,
  getProject,
  getPulls,
  resetPulls,
  resetProject
}: ProjectProps) => {
  const history = useHistory()
  const snackbarContext = useSnackbar()
  const { prId, tab }: any = useParams()
  const projectId = parseInt(prId, 10)

  useEffect(() => {
    if (!Number.isNaN(projectId) && validateTab(tab)) {
      getProject(projectId, getNotifier('error', snackbarContext))
      getPulls(projectId, getNotifier('error', snackbarContext))
    }
    return () => {
      resetProject()
      resetPulls()
    }
    // eslint-disable-next-line
  }, [])

  if (Number.isNaN(projectId) || !validateTab(tab) || user.isFetching || !project) {
    return <></>
  }

  const handleChangeTab = (t: Tab) => {
    historyPush(history, `/projects/${projectId}/${t.id}`)
  }

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

  const isAdmin = isProjectAdmin(user.value, project)
  if (isAdmin) {
    tabs.push({ id: 'settings', text: 'Settings', Icon: SettingsIcon })
  }

  return (
    <>
      <PageTitle title={project && project.repoName} />
      <BreadCrumbs
        breadcrumbs={[{ text: 'Projects', to: '/projects' }, { text: project.repoName }]}
      />
      <Tabs tabs={tabs} onChange={handleChangeTab} activeId={tab} />
      {(tab === 'pulls' || tab === undefined) && (
        <ProjectPullsTab project={project} pulls={pulls} />
      )}
      {isAdmin && tab === 'settings' && <ProjectSettingsTab user={user.value} project={project} />}
    </>
  )
}

const mapStateToProps = (state: AppState) => ({
  project: state.projects.project.value,
  pulls: state.pulls.pulls.value,
  user: state.users.user
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getProject: bindActionCreators(getProjectAction, dispatch),
  resetPulls: bindActionCreators(resetPullsInfo, dispatch),
  resetProject: bindActionCreators(resetProjectInfo, dispatch),
  getPulls: bindActionCreators(getPullsAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(Project)
