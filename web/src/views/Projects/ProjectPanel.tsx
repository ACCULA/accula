import React from 'react'
import { Link } from 'react-router-dom'
import { Panel } from 'react-bootstrap'

import { IProject } from 'types'
import { ProjectPanelHeading } from './ProjectPanelHeading'

export const ProjectPanel = (project: IProject) => {
  return (
    <Panel className="panel-project">
      <ProjectPanelHeading project={project} />
      <Panel.Body>
        <p>{project.repoDescription}</p>
      </Panel.Body>
      <Panel.Footer className="clearfix">
        <div className="pull-right">
          <Link to={`/projects/${project.id}/pulls`}>
            View {project.repoOpenPullCount} opened pull requests
            <i className="fa fa-fw fa-chevron-circle-right" />
          </Link>
        </div>
      </Panel.Footer>
    </Panel>
  )
}
