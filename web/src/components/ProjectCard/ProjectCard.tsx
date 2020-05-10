import React from 'react'
import { Project } from 'types'
import { Link } from 'react-router-dom'

const ProjectCard = ({
  id,
  url,
  owner,
  name,
  description,
  avatar,
  openPullRequestCount
}: Project) => {
  return (
    <div className="panel panel-default panel-project">
      <div className="panel-heading">
        <div className="avatar">
          <img className="border-gray" src={avatar} alt={owner} />
        </div>
        <div className="title">
          <div className="owner">{owner}/</div>
          <div className="name">
            <a href={url} target="_blank" rel="noopener noreferrer">
              {name}
            </a>
          </div>
        </div>
      </div>
      <div className="panel-body">
        <p>{description}</p>
      </div>
      <div className="panel-footer clearfix">
        <div className="pull-right">
          <Link to={`/projects/${id}`}>
            View {openPullRequestCount} opened pull requests
            <i className="fa fa-fw fa-chevron-circle-right" />
          </Link>
        </div>
      </div>
    </div>
  )
}

export default ProjectCard
