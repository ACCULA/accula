import React from 'react'
import { Link, Route, Switch, useRouteMatch } from 'react-router-dom'
import { Grid, Panel, Row, Table } from 'react-bootstrap'

import { projects, pulls } from 'data'
import { Project as Proj } from 'types'
import PullRequest from 'views/PullRequest'

interface RouteParams {
  projectId: string
}

const Project = () => {
  const match = useRouteMatch<RouteParams>()
  const { projectId } = match.params
  const project: Proj = projects.find(p => p.id.toString() === projectId)

  return (
    <Switch>
      <Route path="/projects/:projectId/:pullRequestId">
        <PullRequest />
      </Route>
      <Route path="/projects/:projectId" exact>
        <Grid fluid className="project panel-project">
          <Row>
            <Panel>
              <Panel.Heading className="project-name">
                <div className="avatar">
                  <img className="avatar border-gray" src={project.avatar} alt={project.owner} />
                </div>
                <div className="title">
                  <div className="owner">{project.owner}/</div>
                  <div className="name">
                    <a href={project.url} target="_blank" rel="noopener noreferrer">
                      {project.name}
                    </a>
                  </div>
                </div>
              </Panel.Heading>
              <Table striped bordered hover responsive>
                <thead>
                  <tr className="project-pull">
                    <th className="id">#</th>
                    <th>Pull Request</th>
                    <th>Author</th>
                  </tr>
                </thead>
                <tbody>
                  {pulls.map(pull => {
                    return (
                      <tr key={pull.id} className="project-pull">
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
                    )
                  })}
                </tbody>
              </Table>
            </Panel>
          </Row>
        </Grid>
      </Route>
    </Switch>
  )
}

export default Project
