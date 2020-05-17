import React, { useEffect } from 'react'
import { bindActionCreators } from 'redux'
import { connect, ConnectedProps } from 'react-redux'
import { useHistory, useParams } from 'react-router-dom'
import { Badge, Tab, Tabs } from 'react-bootstrap'
import { Helmet } from 'react-helmet'

import { Breadcrumbs } from 'components/Breadcrumbs'
import { AppDispatch, AppState } from 'store'
import { getPullAction } from 'store/pulls/actions'
import { getProjectAction } from 'store/projects/actions'
import { PullClonesTab } from './PullClonesTab'
import { PullChangesTab } from './PullChangesTab'
import { PullOverviewTab } from './PullOverviewTab'

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
  const { prId, plId, tab } = useParams()
  const projectId = parseInt(prId, 10)
  const pullId = parseInt(plId, 10)

  useEffect(() => {
    getProject(projectId)
  }, [getProject, projectId])

  useEffect(() => {
    getPull(projectId, pullId)
  }, [getPull, projectId, pullId])

  if (isFetching) {
    return <></>
  }
  return (
    <div className="content">
      <Helmet>
        <title>{`${pull.title} - ${project.repoName} - ACCULA`}</title>
      </Helmet>
      <Breadcrumbs
        breadcrumbs={[
          { text: 'Projects', to: '/projects' },
          { text: project.repoName, to: `/projects/${project.id}` },
          { text: pull.title }
        ]}
      />
      <Tabs
        activeKey={tab || 'overview'} //
        onSelect={key => {
          const tabPath = key === 'overview' ? '' : `/${key}`
          history.push(`/projects/${projectId}/pulls/${pullId}${tabPath}`)
        }}
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
          <PullChangesTab />
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
