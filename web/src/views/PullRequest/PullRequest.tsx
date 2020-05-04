import React from 'react'
import { useRouteMatch } from 'react-router-dom'
import { Project as Proj, PullRequest as Pull } from 'types'
import { projects, pulls } from 'data'

interface RouteParams {
  projectId: string
  pullRequestId: string
}

const PullRequest = () => {
  const match = useRouteMatch<RouteParams>()
  const { projectId, pullRequestId } = match.params
  const project: Proj = projects.find(p => p.id.toString() === projectId)
  const pull: Pull = pulls.find(p => p.id.toString() === pullRequestId)
  return (
    <>
      {project.name}
      {pull.author.login}
    </>
  )
}

export default PullRequest
