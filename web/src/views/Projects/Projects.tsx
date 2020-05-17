import React, { useEffect, useState } from 'react'
import { bindActionCreators } from 'redux'
import { connect, ConnectedProps } from 'react-redux'
import { Button, Clearfix, Col, Grid, Row } from 'react-bootstrap'

import { Breadcrumbs } from 'components/Breadcrumbs'
import { AppDispatch, AppState } from 'store'
import { createProjectAction, getProjectsAction } from 'store/projects/actions'
import { CreateProjectModal } from './CreateProjectModal'
import { ProjectPanel } from './ProjectPanel'

const mapStateToProps = (state: AppState) => ({
  isFetching: state.projects.isFetching,
  authorized: state.users.token,
  projects: state.projects.projects
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getProjects: bindActionCreators(getProjectsAction, dispatch),
  createProject: bindActionCreators(createProjectAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)
type ProjectsProps = ConnectedProps<typeof connector>

const Projects = ({
  authorized, //
  isFetching,
  projects,
  getProjects,
  createProject
}: ProjectsProps) => {
  const [isShowModal, setShowModal] = useState(false)

  useEffect(() => {
    getProjects()
  }, [getProjects, projects])

  if (isFetching || !projects) {
    return <></>
  }
  return (
    <div className="content">
      <Grid fluid className="tight">
        <Clearfix>
          <div className="pull-right">
            {authorized && (
              <Button bsStyle="info" style={{ marginTop: -5 }} onClick={() => setShowModal(true)}>
                <i className="fa fa-plus" /> Add project
              </Button>
            )}
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
    </div>
  )
}

export default connector(Projects)
