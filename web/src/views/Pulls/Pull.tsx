import React, { useEffect } from 'react'
import { bindActionCreators } from 'redux'
import { connect, ConnectedProps } from 'react-redux'
import { useHistory, useParams } from 'react-router-dom'
import { Badge, Tab, Tabs } from 'react-bootstrap'
import { Helmet } from 'react-helmet'

import { Breadcrumbs } from 'components/Breadcrumbs'
import { Loader } from 'components/Loader'
import { AppDispatch, AppState } from 'store'
import { getClonesAction, getDiffAction, getPullAction } from 'store/pulls/actions'
import { getProjectAction } from 'store/projects/actions'
import { PullClonesTab } from './PullClonesTab'
import { PullChangesTab } from './PullChangesTab'
import { PullOverviewTab } from './PullOverviewTab'
import { PullGraphTab } from 'views/Pulls/PullGraphTab'

const mapStateToProps = (state: AppState) => ({
  isFetching:
    state.pulls.pull.isFetching ||
    state.projects.project.isFetching ||
    !state.pulls.pull.value ||
    !state.projects.project,
  project: state.projects.project.value,
  pull: state.pulls.pull.value,
  diffs: state.pulls.diff,
  clones: state.pulls.clones
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getProject: bindActionCreators(getProjectAction, dispatch),
  getPull: bindActionCreators(getPullAction, dispatch),
  getDiffs: bindActionCreators(getDiffAction, dispatch),
  getClones: bindActionCreators(getClonesAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)
type PullsProps = ConnectedProps<typeof connector>

const Pull = ({
  isFetching,
  project,
  pull,
  diffs,
  clones,
  getProject,
  getPull,
  getClones,
  getDiffs
}: PullsProps) => {
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

  useEffect(() => {
    getClones(projectId, pullId)
  }, [getClones, projectId, pullId])

  useEffect(() => {
    getDiffs(projectId, pullId)
  }, [getDiffs, projectId, pullId])

  if (isFetching || (pull && pull.number !== pullId)) {
    return <Loader />
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
          eventKey="graph"
          title={
            <>
              <i className="fas fa-fw fa-project-diagram" /> Graph
            </>
          }
        >
          <PullGraphTab />
        </Tab>
        <Tab
          eventKey="overview"
          title={
            <>
              <i className="fas fa-fw fa-eye" /> Overview
            </>
          }
        >
          <PullOverviewTab pull={pull} />
        </Tab>
        <Tab
          eventKey="changes"
          title={
            <>
              <i className="fas fa-fw fa-code" /> Changes{' '}
              <Badge>
                {diffs.isFetching ? (
                  <i className="fas fa-spinner fa-spin" />
                ) : (
                  diffs.value && diffs.value.length
                )}
              </Badge>
            </>
          }
        >
          <PullChangesTab isFetching={diffs.isFetching} diffs={diffs.value} />
        </Tab>
        <Tab
          eventKey="clones"
          title={
            <>
              <i className="far fa-fw fa-copy" /> Clones{' '}
              <Badge>
                {clones.isFetching ? (
                  <i className="fas fa-spinner fa-spin" />
                ) : (
                  clones.value && clones.value.length
                )}
              </Badge>
            </>
          }
        >
          <PullClonesTab isFetching={clones.isFetching} clones={clones.value} />
        </Tab>
      </Tabs>
    </div>
  )
}

export default connector(Pull)
