import React from 'react'
import { Panel } from 'react-bootstrap'

import { Project } from 'types'

const ProjectPanelHeading = (project: Project) => {
  return (
    <Panel.Heading>
      <div className="avatar">
        <img className="border-gray" src={project.repoOwnerAvatar} alt={project.repoOwner} />
      </div>
      <div className="title">
        <div className="owner">{project.repoOwner}/</div>
        <div className="name">
          <a href={project.url} target="_blank" rel="noopener noreferrer">
            {project.repoName}
          </a>
        </div>
      </div>
    </Panel.Heading>
  )
}

export default ProjectPanelHeading
