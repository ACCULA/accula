import React, { useEffect } from 'react'
import { useRouteMatch } from 'react-router-dom'
import { bindActionCreators } from 'redux'
import { connect, ConnectedProps } from 'react-redux'
import { Grid, Panel, Table } from 'react-bootstrap'
import { LinkContainer } from 'react-router-bootstrap'

import { Breadcrumbs } from 'components/Breadcrumbs'
import { AppDispatch, AppState } from 'store'
import { getProjectAction } from 'store/projects/actions'
import { getPullsAction } from 'store/pulls/actions'
import { ProjectPanelHeading } from './ProjectPanelHeading'

interface RouteParams {
  projectId: string
}

const mapStateToProps = (state: AppState) => ({
  isFetching:
    state.projects.isFetching ||
    !state.projects.project ||
    state.pulls.isFetching ||
    !state.pulls.pulls,
  projectsFetching: state.projects.isFetching,
  project: state.projects.project,
  pullsFetching: state.pulls.isFetching,
  pulls: state.pulls.pulls
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getProject: bindActionCreators(getProjectAction, dispatch),
  getPulls: bindActionCreators(getPullsAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)
type ProjectProps = ConnectedProps<typeof connector>

const Project = ({
  projectsFetching,
  project,
  pullsFetching,
  pulls,
  getProject,
  getPulls
}: ProjectProps) => {
  const match = useRouteMatch<RouteParams>()
  const { projectId } = match.params
  const id = parseInt(projectId, 10)

  useEffect(() => {
    getProject(id)
  }, [getProject, id, project])

  useEffect(() => {
    getPulls(id)
  }, [getPulls, id, pulls])

  if (projectsFetching || !project || (project && project.id !== id) || pullsFetching || !pulls) {
    return <></>
  }

  return (
    <div className="content">
      <Grid fluid className="tight">
        <Breadcrumbs
          breadcrumbs={[{ text: 'Projects', to: '/projects' }, { text: project.repoName }]}
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
                <LinkContainer to={`/projects/${projectId}/pulls/${pull.id}`} key={pull.id}>
                  <tr className="project-pull pointer">
                    <td className="id">{pull.id}</td>
                    <td>{pull.title}</td>
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
    </div>
  )
}

export default connector(Project)
