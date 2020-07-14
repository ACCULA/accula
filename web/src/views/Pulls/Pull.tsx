import React, { useEffect, useState } from 'react'
import { bindActionCreators } from 'redux'
import { connect, ConnectedProps } from 'react-redux'
import { useHistory, useParams } from 'react-router-dom'
import { Badge, Tab, Tabs } from 'react-bootstrap'
import { Helmet } from 'react-helmet'

import { Breadcrumbs } from 'components/Breadcrumbs'
import { Loader } from 'components/Loader'
import { AppDispatch, AppState } from 'store'
import {
  getClonesAction,
  getComparesAction,
  getDiffsAction,
  getPullAction,
  getPullsAction,
  refreshClonesAction
} from 'store/pulls/actions'
import { getProjectAction } from 'store/projects/actions'
import { PullCompareTab } from 'views/Pulls/PullCompareTab'
import { useLocation } from 'react-use'
import { PullClonesTab } from './PullClonesTab'
import { PullChangesTab } from './PullChangesTab'
import { PullOverviewTab } from './PullOverviewTab'

const mapStateToProps = (state: AppState) => ({
  isFetching:
    state.pulls.pull.isFetching ||
    state.projects.project.isFetching ||
    !state.pulls.pull.value ||
    !state.projects.project,
  project: state.projects.project.value,
  pull: state.pulls.pull.value,
  pulls: state.pulls.pulls.value,
  diffs: state.pulls.diffs,
  compares: state.pulls.compares,
  clones: state.pulls.clones
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getProject: bindActionCreators(getProjectAction, dispatch),
  getPull: bindActionCreators(getPullAction, dispatch),
  getPulls: bindActionCreators(getPullsAction, dispatch),
  getDiffs: bindActionCreators(getDiffsAction, dispatch),
  getCompares: bindActionCreators(getComparesAction, dispatch),
  getClones: bindActionCreators(getClonesAction, dispatch),
  refreshClones: bindActionCreators(refreshClonesAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)
type PullsProps = ConnectedProps<typeof connector>

const Pull = ({
  isFetching,
  project,
  pull,
  pulls,
  diffs,
  compares,
  clones,
  getProject,
  getPull,
  getPulls,
  getDiffs,
  getCompares,
  getClones,
  refreshClones
}: PullsProps) => {
  const history = useHistory()
  const location = useLocation()

  const { prId, plId, tab } = useParams()
  const projectId = parseInt(prId, 10)
  const pullId = parseInt(plId, 10)
  const [compareWith, setCompareWith] = useState(0)

  useEffect(() => {
    const query = parseInt(new URLSearchParams(location.search).get('with') || '0', 10)
    if (compareWith !== query) {
      setCompareWith(query)
      getCompares(projectId, pullId, query)
    }
  }, [compareWith, location, getCompares, projectId, pullId])

  useEffect(() => {
    getProject(projectId)
  }, [getProject, projectId])

  useEffect(() => {
    getPull(projectId, pullId)
  }, [getPull, projectId, pullId])

  useEffect(() => {
    getPulls(projectId)
  }, [getPulls, projectId])

  useEffect(() => {
    getDiffs(projectId, pullId)
  }, [getDiffs, projectId, pullId])

  useEffect(() => {
    getClones(projectId, pullId)
  }, [getClones, projectId, pullId])

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
          <PullChangesTab diffs={diffs} />
        </Tab>
        <Tab
          eventKey="compare"
          title={
            <>
              <i className="fas fa-fw fa-exchange-alt" /> Compare
            </>
          }
        >
          <PullCompareTab
            pullId={pullId}
            pulls={pulls}
            compares={compares}
            compareWith={compareWith}
            onSelect={num => {
              if (num === 0) {
                history.push(`/projects/${projectId}/pulls/${pullId}/compare`)
              } else {
                history.push(`/projects/${projectId}/pulls/${pullId}/compare?with=${num}`)
              }
            }}
          />
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
          <PullClonesTab
            clones={clones} //
            refreshClones={() => refreshClones(projectId, pullId)}
          />
        </Tab>
      </Tabs>
    </div>
  )
}

export default connector(Pull)
