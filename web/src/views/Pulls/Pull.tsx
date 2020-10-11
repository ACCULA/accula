import React, { useEffect, useState } from 'react'
import { bindActionCreators } from 'redux'
import { connect, ConnectedProps } from 'react-redux'
import { useHistory, useParams } from 'react-router-dom'
import { AppDispatch, AppState } from 'store'
import { getClonesAction, getDiffsAction, getPullAction } from 'store/pulls/actions'
import { getProjectAction, getProjectConfAction } from 'store/projects/actions'
import { PullCompareTab } from 'views/Pulls/PullCompareTab'
import { historyPush, isProjectAdmin } from 'utils'
import { getCurrentUserAction } from 'store/users/actions'
import { PageTitle } from 'components/PageTitle'
import {
  VisibilityRounded,
  CodeRounded,
  CompareArrowsRounded,
  FileCopyRounded
} from '@material-ui/icons'
import BreadCrumbs from 'components/BreadCrumbs'
import Tabs, { Tab } from 'components/Tabs/Tabs'
import { CircularProgress } from '@material-ui/core'
import PullOverviewTab from './PullOverviewTab'
import PullChangesTab from './PullChangesTab'

const tabValues = ['changes', 'compare', 'clones'] as string[]

const validateTab = (tab: string) => tabValues.includes(tab) || tab === undefined

interface PullsProps extends PropsFromRedux {}

const Pull = ({
  project,
  pull,
  diffs,
  clones,
  getProject,
  getPull,
  getDiffs,
  getClones
}: PullsProps) => {
  const history = useHistory()
  const { prId, plId, tab }: any = useParams()
  const projectId = parseInt(prId, 10)
  const pullId = parseInt(plId, 10)
  //   const [compareWith, setCompareWith] = useState(0)

  //   useEffect(() => {
  //     const query = parseInt(new URLSearchParams(location.search).get('with') || '0', 10)
  //     if (compareWith !== query) {
  //       setCompareWith(query)
  //       getCompares(projectId, pullId, query)
  //     }
  //   }, [compareWith, location, getCompares, projectId, pullId])

  useEffect(() => {
    if (!Number.isNaN(projectId) && !Number.isNaN(pullId) && validateTab(tab)) {
      getProject(projectId)
      getPull(projectId, pullId)
      getDiffs(projectId, pullId)
      getClones(projectId, pullId)
    }
    // eslint-disable-next-line
  }, [])

  if (Number.isNaN(projectId) || Number.isNaN(pullId) || !validateTab(tab) || !project || !pull) {
    return <></>
  }

  const handleChangeTab = (t: Tab) => {
    historyPush(history, `/projects/${projectId}/pulls/${pullId}${t.id ? `/${t.id}` : ''}`)
  }

  const tabs = [
    {
      id: '',
      text: 'Overview',
      Icon: VisibilityRounded
    },
    {
      id: 'changes',
      text: 'Changes',
      Icon: CodeRounded,
      badgeValue: diffs ? diffs.length : <CircularProgress size={12} color="inherit" />
    },
    {
      id: 'compare',
      text: 'Compare',
      Icon: CompareArrowsRounded
    },
    {
      id: 'clones',
      text: 'Clones',
      Icon: FileCopyRounded,
      badgeValue: clones ? clones.length : <CircularProgress size={12} color="inherit" />
    }
  ] as Tab[]

  return (
    <div>
      <PageTitle
        title={pull && project && `${pull.author.login} - ${pull.title} - ${project.repoName}`}
      />
      <BreadCrumbs
        breadcrumbs={[
          { text: 'Projects', to: '/projects' },
          { text: project.repoName, to: `/projects/${project.id}/pulls` },
          { text: pull.title }
        ]}
      />
      <Tabs tabs={tabs} onChange={handleChangeTab} activeId={tab} />
      {tab === undefined && <PullOverviewTab project={project} pull={pull} />}
      {tab === 'changes' && <PullChangesTab diffs={diffs} />}
    </div>
  )
}

const mapStateToProps = (state: AppState) => ({
  project: state.projects.project.value,
  pull: state.pulls.pull.value,
  diffs: state.pulls.diffs.value,
  clones: state.pulls.clones.value
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getUser: bindActionCreators(getCurrentUserAction, dispatch),
  getProject: bindActionCreators(getProjectAction, dispatch),
  getPull: bindActionCreators(getPullAction, dispatch),
  getDiffs: bindActionCreators(getDiffsAction, dispatch),
  getClones: bindActionCreators(getClonesAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(Pull)
