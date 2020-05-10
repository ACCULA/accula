import React, { useEffect } from 'react'
import { Link, Route, Switch, useRouteMatch } from 'react-router-dom'
import { bindActionCreators } from 'redux'
import { connect, ConnectedProps } from 'react-redux'
import { Grid, Panel, Table } from 'react-bootstrap'
import { LinkContainer } from 'react-router-bootstrap'

import { pulls } from 'data'
import PullRequest from 'views/PullRequest'
import Breadcrumbs from 'components/Breadcrumbs'
import { AppDispatch, AppState } from 'store'
import { getProjectAction } from 'store/projects/actions'
import ProjectPanelHeading from './ProjectPanelHeading'

interface RouteParams {
  projectId: string
}

const mapStateToProps = (state: AppState) => ({
  projects: state.projects.projects,
  project: state.projects.project,
  isFetching: state.projects.isFetching
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getProject: bindActionCreators(getProjectAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)
type ProjectProps = ConnectedProps<typeof connector>

const Project = ({ isFetching, projects, project, getProject }: ProjectProps) => {
  const match = useRouteMatch<RouteParams>()
  const { projectId } = match.params
  const id = parseInt(projectId, 10)

  useEffect(() => {
    getProject(id)
  }, [getProject, id, projects])

  if (isFetching || !project || (project && project.id !== id)) {
    return <></>
  }

  return (
    <Switch>
      <Route path="/projects/:projectId/:pullRequestId" component={PullRequest} />
      <Route path="/projects/:projectId" exact>
        <Grid fluid className="tight">
          <Breadcrumbs
            breadcrumbs={[{ text: 'Projects', to: '/projects' }, { text: project.name }]}
          />
          <Panel className="project panel-project">
            <ProjectPanelHeading {...project} />
            <Table striped bordered hover responsive>
              <thead>
                <tr className="project-pull">
                  <th className="id">#</th>
                  <th>Pull Request</th>
                  <th>Author</th>
                </tr>
              </thead>
              <tbody>
                {pulls.map(pull => (
                  <LinkContainer to={`/projects/${projectId}/${pull.id}`} key={pull.id}>
                    <tr className="project-pull pointer">
                      <td className="id">{pull.id}</td>
                      <td>
                        <Link to={`/projects/${projectId}/${pull.id}`}>{pull.title}</Link>
                      </td>
                      <td className="avatar">
                        <img
                          className="border-gray"
                          src={pull.author.avatar}
                          alt={pull.author.login}
                        />
                        {pull.author.login}
                      </td>
                    </tr>
                  </LinkContainer>
                ))}
              </tbody>
            </Table>
          </Panel>
        </Grid>
      </Route>
    </Switch>
  )
}

export default connector(Project)
