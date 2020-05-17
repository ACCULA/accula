import React from 'react'
import { Panel } from 'react-bootstrap'

import { IProject } from 'types'

export const ProjectPanelHeading = (project: IProject) => {
  const urlHack = project.repoUrl.replace('api.', '').replace('repos/', '')
  return (
    <Panel.Heading>
      <div className="avatar">
        <img className="border-gray" src={project.repoOwnerAvatar} alt={project.repoOwner} />
      </div>
      <div className="title">
        <div className="owner">{project.repoOwner}/</div>
        <div className="name">
          <a href={urlHack} target="_blank" rel="noopener noreferrer">
            {project.repoName}
          </a>
        </div>
      </div>
    </Panel.Heading>
  )
}
