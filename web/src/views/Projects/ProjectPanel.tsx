import React from 'react'
import { Link } from 'react-router-dom'
import { Panel } from 'react-bootstrap'

import { Project } from 'types'
import ProjectPanelHeading from './ProjectPanelHeading'

interface ProjectPanelProps {
  project: Project
}

const ProjectPanel = ({ project }: ProjectPanelProps) => {
  return (
    <Panel className="panel-project">
      <ProjectPanelHeading {...project} />
      <Panel.Body>
        <p>{project.description}</p>
      </Panel.Body>
      <Panel.Footer className="clearfix">
        <div className="pull-right">
          <Link to={`/projects/${project.id}`}>
            View {project.openPullCount} opened pull requests
            <i className="fa fa-fw fa-chevron-circle-right" />
          </Link>
        </div>
      </Panel.Footer>
    </Panel>
  )
}

export default ProjectPanel
