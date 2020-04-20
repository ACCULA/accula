import React from 'react'
import { Col, Grid, Row } from 'react-bootstrap'
import ProjectCard from 'components/ProjectCard'

import { projects } from 'data'
import { Route, Switch } from 'react-router-dom'
import Project from './Project'

const Projects = () => {
  return (
    <div className="content">
      <Switch>
        <Route path="/projects/:id">
          <Project />
        </Route>
        <Route path="/projects" exact>
          <Grid fluid>
            <Row>
              {projects.map(project => (
                <Col key={project.id} xs={12} sm={6} md={6} lg={4}>
                  <ProjectCard {...project} />
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
