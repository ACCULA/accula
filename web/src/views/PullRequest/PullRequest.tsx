import React from 'react'
import { useRouteMatch } from 'react-router-dom'
import { Project as Proj, PullRequest as Pull } from 'types'
import { projects, pulls } from 'data'
import { Panel } from 'react-bootstrap'
import Breadcrumbs from 'components/Breadcrumbs'

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
      <Breadcrumbs
        breadcrumbs={[
          { text: 'Projects', to: '/projects' },
          { text: project.name, to: `/projects/${project.id}` },
          { text: `${pull.title} (@${pull.author.login})` }
        ]}
      />
      <Panel>
        <Panel.Heading>{pull.author.login}</Panel.Heading>
        <Panel.Body>Body</Panel.Body>
      </Panel>
    </>
  )
}

export default PullRequest
