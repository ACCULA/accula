import React, { useState } from 'react'
import { Route, Switch } from 'react-router-dom'
import { Button, Clearfix, Col, Grid, Row } from 'react-bootstrap'

import { projects } from 'data'
import Breadcrumbs from 'components/Breadcrumbs'
import ProjectPanel from './ProjectPanel'
import Project from './Project'
import CreateProjectModal from './CreateProjectModal'

const Projects = () => {
  const [isShowModal, setShowModal] = useState(false)
  return (
    <div className="content">
      <Switch>
        <Route path="/projects/:projectId">
          <Project />
        </Route>
        <Route path="/projects" exact>
          <Grid fluid className="tight">
            <Clearfix>
              <div className="pull-right">
                <Button bsStyle="info" style={{ marginTop: -5 }} onClick={() => setShowModal(true)}>
                  <i className="fa fa-plus" /> Add project
                </Button>
              </div>
              <Breadcrumbs breadcrumbs={[{ text: 'Projects' }]} />
            </Clearfix>
            <Row>
              {projects.map(project => (
                <Col key={project.id} xs={12} sm={6} md={6} lg={4}>
                  <ProjectPanel {...project} />
                </Col>
              ))}
            </Row>
          </Grid>
          <CreateProjectModal
            show={isShowModal}
            onClose={() => setShowModal(false)}
            onCreate={() => setShowModal(false)}
          />
        </Route>
      </Switch>
    </div>
  )
}

export default Projects
