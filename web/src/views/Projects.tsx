import React from 'react'
import { Col, Grid, Row } from 'react-bootstrap'
import ProjectCard from 'components/ProjectCard'

import { projects } from 'data'

const Projects = () => (
  <div className="content">
    <Grid fluid>
      <Row>
        {projects.map(project => (
          <Col key={project.id} xs={12} sm={6} md={6} lg={4}>
            <ProjectCard {...project} />
          </Col>
        ))}
      </Row>
    </Grid>
  </div>
)

export default Projects
