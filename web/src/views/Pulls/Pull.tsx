import React, { useEffect } from 'react'
import { bindActionCreators } from 'redux'
import { connect, ConnectedProps } from 'react-redux'
import { useHistory, useParams } from 'react-router-dom'
import { AppDispatch, AppState } from 'store'
import { getClonesAction, getPullAction } from 'store/pulls/actions'
import { getProjectAction } from 'store/projects/actions'
import { historyPush, isProjectAdmin } from 'utils'
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
import PullLabel from 'components/PullLabel'
import PullOverviewTab from './PullOverviewTab'
import PullChangesTab from './PullChangesTab'
import PullCompareTab from './PullCompareTab'
import PullClonesTab from './PullClonesTab/PullClonesTab'

const clonesTabId = 'clones'
const compareTabId = 'compare'
const changesTabId = 'changes'

const tabValues: string[] = [clonesTabId, compareTabId, changesTabId]

const validateTab = (tab: string) => tabValues.includes(tab) || tab === undefined

interface PullsProps extends PropsFromRedux {}

const Pull = ({
  user,
  project,
  pull,
  diffs,
  clones,
  getProject,
  getPull,
  getClones
}: PullsProps) => {
  const history = useHistory()
  const { prId, plId, tab }: any = useParams()
  const projectId = parseInt(prId, 10)
  const pullId = parseInt(plId, 10)

  useEffect(() => {
    if (!Number.isNaN(projectId) && !Number.isNaN(pullId) && validateTab(tab)) {
      getProject(projectId)
      getPull(projectId, pullId)
      getClones(projectId, pullId)
    }
    // eslint-disable-next-line
  }, [])

  if (
    Number.isNaN(projectId) ||
    Number.isNaN(pullId) ||
    !validateTab(tab) ||
    user.isFetching ||
    !project ||
    !pull
  ) {
    return <></>
  }

  const handleChangeTab = (t: Tab) => {
    historyPush(history, `/projects/${projectId}/pulls/${pullId}${t.id ? `/${t.id}` : ''}`)
  }

  const tabs: Tab[] = [
    {
      id: '',
      text: 'Overview',
      Icon: VisibilityRounded
    },
    {
      id: clonesTabId,
      text: 'Clones',
      Icon: FileCopyRounded,
      badgeValue: clones.value ? (
          clones.value.length
      ) : (
          <CircularProgress size={12} color="inherit" />
      )
    },
    {
      id: compareTabId,
      text: 'Compare',
      Icon: CompareArrowsRounded
    },
    {
      id: changesTabId,
      text: 'Changes',
      Icon: CodeRounded,
      badgeValue: (
          diffs && diffs.value ? diffs.value.length
              : diffs && diffs.isFetching ? <CircularProgress size={12} color="inherit" /> : null
      ),
    },
  ]

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
      {tab === clonesTabId && (
        <PullClonesTab
          project={project}
          pull={pull}
          clones={clones}
          isAdmin={isProjectAdmin(user.value, project)}
        />
      )}
      {tab === compareTabId && <PullCompareTab project={project} pull={pull} />}
      {tab === changesTabId && <PullChangesTab/>}
    </div>
  )
}

export const getPullTitle = (base?: string, head?: string): JSX.Element => {
  if (base && head) {
    if (base === head) {
      return <PullLabel text={base} />
    }
    return <PullLabel type="added" text={`${base} -> ${head}`} />
  }
  if (base) {
    return <PullLabel type="removed" text={base} />
  }
  if (head) {
    return <PullLabel type="removed" text={head} />
  }
  return <code />
}

const mapStateToProps = (state: AppState) => ({
  project: state.projects.project.value,
  user: state.users.user,
  pull: state.pulls.pull.value,
  diffs: state.pulls.diffs,
  clones: state.pulls.clones
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getProject: bindActionCreators(getProjectAction, dispatch),
  getPull: bindActionCreators(getPullAction, dispatch),
  getClones: bindActionCreators(getClonesAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(Pull)
