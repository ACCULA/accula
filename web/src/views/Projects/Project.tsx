import React, { useEffect } from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { Grid, Tab, Tabs } from 'react-bootstrap'
import { useHistory, useParams } from 'react-router-dom'
import { Helmet } from 'react-helmet'
import { bindActionCreators } from 'redux'

import { Breadcrumbs } from 'components/Breadcrumbs'
import { Loader } from 'components/Loader'
import { AppDispatch, AppState } from 'store'
import {
  getProjectAction, getProjectConfAction,
  getRepoAdminsAction,
  updateProjectConfAction
} from 'store/projects/actions'
import { getPullsAction } from 'store/pulls/actions'
import { IProject, IProjectConf, IUser } from 'types'
import { ProjectPullsTab } from 'views/Projects/ProjectPullsTab'
import { ProjectConfigurationTab } from 'views/Projects/ProjectConfigurationTab'

const mapStateToProps = (state: AppState) => ({
  isFetching: state.projects.project.isFetching || !state.projects.project.value,
  project: state.projects.project,
  projectConf: state.projects.projectConf,
  updateProjectConfState: state.projects.updateProjectConf,
  repoAdmins: state.projects.repoAdmins,
  pulls: state.pulls.pulls,
  user: state.users.user
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getProject: bindActionCreators(getProjectAction, dispatch),
  getProjectConf: bindActionCreators(getProjectConfAction, dispatch),
  getRepoAdmins: bindActionCreators(getRepoAdminsAction, dispatch),
  getPulls: bindActionCreators(getPullsAction, dispatch),
  updateProjectConf: bindActionCreators(updateProjectConfAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)
type ProjectProps = ConnectedProps<typeof connector>

const showSettings = (user: IUser, project: IProject, projectConf: IProjectConf): boolean => {
  return (
    user && project && (project.creatorId === user.id || projectConf.admins.indexOf(user.id) !== -1)
  )
}

const Project = ({
  isFetching, //
  project,
  projectConf,
  updateProjectConfState,
  repoAdmins,
  pulls,
  user,
  getProject,
  getProjectConf,
  getRepoAdmins,
  getPulls,
  updateProjectConf
}: ProjectProps) => {
  const history = useHistory()
  const { prId, tab } = useParams()
  const projectId = parseInt(prId, 10)

  useEffect(() => {
    getProject(projectId)
    getProjectConf(projectId)
    getRepoAdmins(projectId)
    getPulls(projectId)
  }, [getProject, getProjectConf, getRepoAdmins, getPulls, projectId])

  return isFetching ? (
    <Loader />
  ) : (
    <div className="content">
      <Helmet>
        <title>{`${project.value.repoName} - ACCULA`}</title>
      </Helmet>
      <Grid fluid className="tight">
        <Breadcrumbs
          breadcrumbs={[{ text: 'Projects', to: '/projects' }, { text: project.value.repoName }]}
        />
        <Tabs
          activeKey={tab || 'overview'} //
          onSelect={key => {
            const tabPath = key === 'overview' ? '' : `/${key}`
            history.push(`/projects/${projectId}${tabPath}`)
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
            Overview
          </Tab>
          <Tab
            eventKey="pulls"
            title={
              <>
                <i className="fas fa-fw fa-code-branch" /> Pull requests
              </>
            }
          >
            <ProjectPullsTab project={project} pulls={pulls} />
          </Tab>
          {showSettings(user.value, project.value, projectConf.value) && (
            <Tab
              eventKey="configuration"
              title={
                <>
                  <i className="fas fa-fw fa-cog" /> Configuration
                </>
              }
            >
              <ProjectConfigurationTab
                project={project} //
                repoAdmins={repoAdmins}
                projectConf={projectConf}
                updateConf={conf => updateProjectConf(projectId, conf)}
                updateConfState={updateProjectConfState}
              />
            </Tab>
          )}
        </Tabs>
      </Grid>
    </div>
  )
}

export default connector(Project)
