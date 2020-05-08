import React from 'react'
import { Route, Switch } from 'react-router-dom'
import { Col, Grid, Row } from 'react-bootstrap'

import { projects } from 'data'
import ProjectPanel from './ProjectPanel'
import Project from './Project'

const Projects = () => {
  return (
    <div className="content">
      <Switch>
        <Route path="/projects/:projectId">
          <Project />
        </Route>
        <Route path="/projects" exact>
          <Grid fluid className="tight">
            <Row>
              {projects.map(project => (
                <Col key={project.id} xs={12} sm={6} md={6} lg={4}>
                  <ProjectPanel {...project} />
                </Col>
              ))}
            </Row>
          </Grid>
        </Route>
      </Switch>
    </div>
  )
}

export default Projects
