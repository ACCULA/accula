import React, { useEffect, useState } from 'react'
import { bindActionCreators } from 'redux'
import { connect, ConnectedProps } from 'react-redux'
import { Route, Switch } from 'react-router-dom'
import { Button, Clearfix, Col, Grid, Row } from 'react-bootstrap'

import Breadcrumbs from 'components/Breadcrumbs'
import { AppDispatch, AppState } from 'store'
import { createProjectAction, getProjectsAction } from 'store/projects/actions'
import CreateProjectModal from './CreateProjectModal'
import Project from './Project'
import ProjectPanel from './ProjectPanel'

const mapStateToProps = (state: AppState) => ({
  project: state.projects.project,
  projects: state.projects.projects,
  isFetching: state.projects.isFetching
})

const mapDispatchToProps = (dispatch: AppDispatch) =>
  bindActionCreators(
    {
      getProjects: getProjectsAction,
      createProject: createProjectAction
    },
    dispatch
  )

const connector = connect(mapStateToProps, mapDispatchToProps)
type ProjectsProps = ConnectedProps<typeof connector>

const Projects = ({ isFetching, projects, getProjects, createProject }: ProjectsProps) => {
  const [isShowModal, setShowModal] = useState(false)

  useEffect(() => {
    if (!projects) {
      getProjects()
    }
  }, [getProjects, projects])

  if (isFetching || !projects) {
    return <></>
  }

  return (
    <div className="content">
      <Switch>
        <Route path="/projects/:projectId" component={Project} />
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
              {projects.map(proj => (
                <Col key={proj.id} xs={12} sm={6} md={6} lg={4}>
                  <ProjectPanel project={proj} />
                </Col>
              ))}
            </Row>
          </Grid>
          <CreateProjectModal
            show={isShowModal}
            onClose={() => setShowModal(false)}
            onSubmit={url => {
              createProject(url)
              setShowModal(false)
            }}
          />
        </Route>
      </Switch>
    </div>
  )
}

export default connector(Projects)
