import React, { useEffect, useState } from 'react'
import { bindActionCreators } from 'redux'
import { connect, ConnectedProps } from 'react-redux'
import { useHistory, useRouteMatch } from 'react-router-dom'
import { Badge, Tab, Tabs } from 'react-bootstrap'

import { Breadcrumbs } from 'components/Breadcrumbs'
import { AppDispatch, AppState } from 'store'
import { getPullAction } from 'store/pulls/actions'
import { getProjectAction } from 'store/projects/actions'
import { PullClonesTab } from './PullClonesTab'
import { PullFileChangesTab } from './PullFileChangesTab'
import { PullOverviewTab } from './PullOverviewTab'

interface RouteParams {
  projectId: string
  pullId: string
  tabName: string
}

const mapStateToProps = (state: AppState) => ({
  isFetching:
    state.pulls.isFetching ||
    state.projects.isFetching ||
    !state.pulls.pull ||
    !state.projects.project,
  project: state.projects.project,
  pull: state.pulls.pull
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getProject: bindActionCreators(getProjectAction, dispatch),
  getPull: bindActionCreators(getPullAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)
type PullsProps = ConnectedProps<typeof connector>

const Pull = ({ isFetching, project, getProject, pull, getPull }: PullsProps) => {
  const history = useHistory()
  const match = useRouteMatch<RouteParams>()
  const { projectId, pullId, tabName } = match.params
  const prId = parseInt(projectId, 10)
  const plId = parseInt(pullId, 10)

  useEffect(() => {
    getProject(prId)
  }, [getProject, prId])

  useEffect(() => {
    getPull(prId, plId)
  }, [getPull, prId, plId])

  const [tab, setTab] = useState(tabName || 'overview')
  useEffect(() => {
    const tabPath = tab === 'overview' ? '' : `/${tab}`
    history.push(`/projects/${projectId}/pulls/${pullId}${tabPath}`)
  }, [tab, tabName, history, pullId, projectId])

  if (isFetching) {
    return <></>
  }
  return (
    <div className="content">
      <Breadcrumbs
        breadcrumbs={[
          { text: 'Projects', to: '/projects' },
          { text: project.repoName, to: `/projects/${project.id}` },
          { text: pull.title }
        ]}
      />
      <Tabs
        activeKey={tab} //
        onSelect={key => setTab(key)}
        id="pull-tabs"
      >
        <Tab
          eventKey="overview"
          title={
            <>
              <i className="fas fa-fw fa-eye" /> Overview
            </>
          }
        >
          <PullOverviewTab {...pull} />
        </Tab>
        <Tab
          eventKey="changes"
          title={
            <>
              <i className="fas fa-fw fa-code" /> Changes <Badge>4</Badge>
            </>
          }
        >
          <PullFileChangesTab />
        </Tab>
        <Tab
          eventKey="clones"
          title={
            <>
              <i className="far fa-fw fa-copy" /> Clones <Badge>1</Badge>
            </>
          }
        >
          <PullClonesTab />
        </Tab>
      </Tabs>
    </div>
  )
}

export default connector(Pull)
