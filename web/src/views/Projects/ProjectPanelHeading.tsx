import React from 'react'
import { Panel } from 'react-bootstrap'

import { IProject } from 'types'
import { Link } from 'components/Link'

interface ProjectPanelHeadingProps {
  project: IProject
}

export const ProjectPanelHeading = ({ project }: ProjectPanelHeadingProps) => {
  const { repoName, repoOwner, repoOwnerAvatar, repoUrl } = project
  return (
    <Panel.Heading>
      <div className="avatar">
        <img className="border-gray" src={repoOwnerAvatar} alt={repoOwner} />
      </div>
      <div className="title">
        <div className="owner">{repoOwner}/</div>
        <div className="name">
          <Link to={repoUrl}>{repoName}</Link>
        </div>
      </div>
    </Panel.Heading>
  )
}
