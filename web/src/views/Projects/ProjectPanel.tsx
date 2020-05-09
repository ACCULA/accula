import React from 'react'
import { Link } from 'react-router-dom'
import { Panel } from 'react-bootstrap'

import { Project } from 'types'
import ProjectPanelHeading from './ProjectPanelHeading'

const ProjectPanel = (project: Project) => {
  return (
    <Panel className="panel-project">
      <ProjectPanelHeading {...project} />
      <Panel.Body>
        <p>{project.description}</p>
      </Panel.Body>
      <Panel.Footer className="clearfix">
        <div className="pull-right">
          <Link to={`/projects/${project.id}`}>
            View {project.openPullRequestCount} opened pull requests
            <i className="fa fa-fw fa-chevron-circle-right" />
          </Link>
        </div>
      </Panel.Footer>
    </Panel>
  )
}

export default ProjectPanel