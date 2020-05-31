import React, { useEffect, useState } from 'react'
import { bindActionCreators } from 'redux'
import { connect, ConnectedProps } from 'react-redux'
import { Button, Clearfix, Col, Grid, Row } from 'react-bootstrap'
import { Helmet } from 'react-helmet'

import { Breadcrumbs } from 'components/Breadcrumbs'
import { Loader } from 'components/Loader'
import { AppDispatch, AppState } from 'store'
import {
  createProjectAction,
  getProjectsAction,
  resetCreationStateAction
} from 'store/projects/actions'
import { CreateProjectModal } from './CreateProjectModal'
import { ProjectPanel } from './ProjectPanel'

const mapStateToProps = (state: AppState) => ({
  isFetching: state.projects.projects.isFetching || !state.projects.projects.value,
  user: state.users.user.value,
  projects: state.projects.projects.value,
  creationState: state.projects.creationState
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getProjects: bindActionCreators(getProjectsAction, dispatch),
  createProject: bindActionCreators(createProjectAction, dispatch),
  resetCreationState: () => dispatch(resetCreationStateAction())
})

const connector = connect(mapStateToProps, mapDispatchToProps)
type ProjectsProps = ConnectedProps<typeof connector>

const Projects = ({
  user, //
  isFetching,
  projects,
  creationState,
  getProjects,
  createProject,
  resetCreationState
}: ProjectsProps) => {
  const [isShowModal, setShowModal] = useState(false)

  useEffect(() => {
    getProjects()
  }, [getProjects])

  useEffect(() => {
    if (!creationState[0] && creationState[1] === null) {
      setShowModal(false)
    }
  }, [creationState, setShowModal])

  return isFetching ? (
    <Loader />
  ) : (
    <div className="content">
      <Helmet>
        <title>Projects - ACCULA</title>
      </Helmet>
      <Grid fluid className="tight">
        <Clearfix>
          <div className="pull-right">
            {user && (
              <Button bsStyle="info" style={{ marginTop: -5 }} onClick={() => setShowModal(true)}>
                <i className="fa fa-plus" /> Add project
              </Button>
            )}
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
        error={creationState[1]}
        isCreating={creationState[0]}
        resetError={resetCreationState}
        onClose={() => {
          resetCreationState()
          setShowModal(false)
        }}
        onSubmit={url => {
          resetCreationState()
          createProject(url)
        }}
      />
    </div>
  )
}

export default connector(Projects)
