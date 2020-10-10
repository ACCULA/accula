import React, { useEffect, useState } from 'react'
import { bindActionCreators } from 'redux'
import { connect, ConnectedProps } from 'react-redux'
import { useHistory, useParams } from 'react-router-dom'
import { Badge, Tab, Tabs } from 'react-bootstrap'

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
import { getProjectAction, getProjectConfAction } from 'store/projects/actions'
import { PullCompareTab } from 'views/Pulls/PullCompareTab'
import { useLocation } from 'react-use'
import { isProjectAdmin } from 'utils'
import { getCurrentUserAction } from 'store/users/actions'
import { PageTitle } from 'components/PageTitle'
import { PullClonesTab } from './PullClonesTab'
import { PullChangesTab } from './PullChangesTab'
import { PullOverviewTab } from './PullOverviewTab'

const mapStateToProps = (state: AppState) => ({
  user: state.users.user,
  project: state.projects.project,
  projectConf: state.projects.projectConf,
  pull: state.pulls.pull,
  pulls: state.pulls.pulls,
  diffs: state.pulls.diffs,
  compares: state.pulls.compares,
  clones: state.pulls.clones
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getUser: bindActionCreators(getCurrentUserAction, dispatch),
  getProject: bindActionCreators(getProjectAction, dispatch),
  getProjectConf: bindActionCreators(getProjectConfAction, dispatch),
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
  user,
  project,
  projectConf,
  pull,
  pulls,
  diffs,
  compares,
  clones,
  getUser,
  getProject,
  getProjectConf,
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
    getPull(projectId, pullId)
    getPulls(projectId)
    getDiffs(projectId, pullId)
    getClones(projectId, pullId)
  }, [getPull, getPulls, getDiffs, getClones, projectId, pullId])

  useEffect(() => {
    getUser()
    getProject(projectId)
    getProjectConf(projectId)
  }, [getUser, getProject, getProjectConf, projectId])

  if (pull.value && pull.value.number !== pullId) {
    return <Loader />
  }
  return (
    <div className="content">
      <PageTitle
        title={pull.value && project.value && `${pull.value.title} - ${project.value.repoName}`}
      />
      <Breadcrumbs
        breadcrumbs={
          project.value && pull.value
            ? [
                { text: 'Projects', to: '/projects' },
                { text: project.value.repoName, to: `/projects/${project.value.id}/pulls` },
                { text: pull.value.title }
              ]
            : [{ text: '' }]
        }
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
            isAdmin={isProjectAdmin(user.value, project.value, projectConf.value)}
          />
        </Tab>
      </Tabs>
    </div>
  )
}

export default connector(Pull)
