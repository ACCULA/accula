import React, { useEffect } from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { Grid, Tab, Tabs } from 'react-bootstrap'
import { useHistory, useParams } from 'react-router-dom'
import { bindActionCreators } from 'redux'

import { Breadcrumbs } from 'components/Breadcrumbs'
import { AppDispatch, AppState } from 'store'
import {
  getBaseFilesAction,
  getProjectAction,
  getProjectConfAction,
  getRepoAdminsAction,
  updateProjectConfAction
} from 'store/projects/actions'
import { getPullsAction } from 'store/pulls/actions'
import { ProjectPullsTab } from 'views/Projects/ProjectPullsTab'
import { ProjectConfigurationTab } from 'views/Projects/ProjectConfigurationTab'
import { ProjectOverviewTab } from 'views/Projects/ProjectOverviewTab'
import { isProjectAdmin } from 'utils'
import { PageTitle } from 'components/PageTitle'

const mapStateToProps = (state: AppState) => ({
  project: state.projects.project,
  projectConf: state.projects.projectConf,
  updateProjectConfState: state.projects.updateProjectConf,
  repoAdmins: state.projects.repoAdmins,
  baseFiles: state.projects.baseFiles,
  pulls: state.pulls.pulls,
  user: state.users.user
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getProject: bindActionCreators(getProjectAction, dispatch),
  getProjectConf: bindActionCreators(getProjectConfAction, dispatch),
  getRepoAdmins: bindActionCreators(getRepoAdminsAction, dispatch),
  getBaseFiles: bindActionCreators(getBaseFilesAction, dispatch),
  getPulls: bindActionCreators(getPullsAction, dispatch),
  updateProjectConf: bindActionCreators(updateProjectConfAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)
type ProjectProps = ConnectedProps<typeof connector>

const Project = ({
  project,
  projectConf,
  updateProjectConfState,
  repoAdmins,
  baseFiles,
  pulls,
  user,
  getProject,
  getProjectConf,
  getRepoAdmins,
  getBaseFiles,
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
    getBaseFiles(projectId)
    getPulls(projectId)
  }, [getProject, getProjectConf, getRepoAdmins, getBaseFiles, getPulls, projectId])

  return (
    <div className="content">
      <PageTitle title={project.value && project.value.repoName} />
      <Grid fluid className="tight">
        <Breadcrumbs
          breadcrumbs={
            project.value && [
              { text: 'Projects', to: '/projects' },
              { text: project.value.repoName }
            ]
          }
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
            <ProjectOverviewTab project={project} />
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
          {isProjectAdmin(user.value, project.value, projectConf.value) && (
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
                baseFiles={baseFiles}
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
