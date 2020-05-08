import React from 'react'
import { Link, Route, Switch, useRouteMatch } from 'react-router-dom'
import { Grid, Panel, Table } from 'react-bootstrap'

import { projects, pulls } from 'data'
import { Project as Proj } from 'types'
import PullRequest from 'views/PullRequest'
import Breadcrumbs from 'components/Breadcrumbs'
import ProjectPanelHeading from './ProjectPanelHeading'

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
                ))}
              </tbody>
            </Table>
          </Panel>
        </Grid>
      </Route>
    </Switch>
  )
}

export default Project
