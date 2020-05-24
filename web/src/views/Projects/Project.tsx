import React, { useEffect } from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { Grid, Panel, Table } from 'react-bootstrap'
import { useParams } from 'react-router-dom'
import { LinkContainer } from 'react-router-bootstrap'
import { Helmet } from 'react-helmet'
import { bindActionCreators } from 'redux'

import { Breadcrumbs } from 'components/Breadcrumbs'
import { Loader } from 'components/Loader'
import { AppDispatch, AppState } from 'store'
import { getProjectAction } from 'store/projects/actions'
import { getPullsAction } from 'store/pulls/actions'
import { ProjectPanelHeading } from './ProjectPanelHeading'

const mapStateToProps = (state: AppState) => ({
  isFetching:
    state.projects.isFetching ||
    !state.projects.project ||
    state.pulls.isFetching ||
    !state.pulls.pulls,
  project: state.projects.project,
  pulls: state.pulls.pulls
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getProject: bindActionCreators(getProjectAction, dispatch),
  getPulls: bindActionCreators(getPullsAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)
type ProjectProps = ConnectedProps<typeof connector>

const Project = ({
  isFetching, //
  project,
  getProject,
  pulls,
  getPulls
}: ProjectProps) => {
  const { prId } = useParams()
  const projectId = parseInt(prId, 10)

  useEffect(() => {
    getProject(projectId)
  }, [getProject, projectId])

  useEffect(() => {
    getPulls(projectId)
  }, [getPulls, projectId])

  if (isFetching || (project && project.id !== projectId)) {
    return <Loader />
  }

  return (
    <div className="content">
      <Helmet>
        <title>{`${project.repoName} - ACCULA`}</title>
      </Helmet>
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
                <LinkContainer to={`/projects/${projectId}/pulls/${pull.number}`} key={pull.number}>
                  <tr className="project-pull pointer">
                    <td className="id">{pull.number}</td>
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
