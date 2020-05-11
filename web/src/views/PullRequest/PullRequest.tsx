import React, { useState } from 'react'
import { useRouteMatch } from 'react-router-dom'
import { Badge, Tab, Tabs } from 'react-bootstrap'

import { Project as Proj, PullRequest as Pull } from 'types'
import { projects, pulls } from 'data'
import Breadcrumbs from 'components/Breadcrumbs'
import { PullOverviewTab } from './PullOverviewTab'
import { FileChangesTab } from './FileChangesTab'
import { CloneDetectionTab } from './CloneDetectionTab'

interface RouteParams {
  projectId: string
  pullRequestId: string
}

const PullRequest = () => {
  const match = useRouteMatch<RouteParams>()
  const { projectId, pullRequestId } = match.params
  const project: Proj = projects.find(p => p.id.toString() === projectId)
  const pull: Pull = pulls.find(p => p.id.toString() === pullRequestId)
  const [tab, setTab] = useState(1)
  return (
    <>
      <Breadcrumbs
        breadcrumbs={[
          { text: 'Projects', to: '/projects' },
          { text: project.name, to: `/projects/${project.id}` },
          { text: pull.title }
        ]}
      />
      <Tabs
        activeKey={tab} //
        onSelect={key => setTab(key)}
        id="pull-tabs"
      >
        <Tab
          eventKey={1}
          title={
            <>
              <i className="fas fa-fw fa-eye" /> Overview
            </>
          }
        >
          <PullOverviewTab {...pull} />
        </Tab>
        <Tab
          eventKey={2}
          title={
            <>
              <i className="fas fa-fw fa-code" /> Changes <Badge>4</Badge>
            </>
          }
        >
          <FileChangesTab />
        </Tab>
        <Tab
          eventKey={3}
          title={
            <>
              <i className="far fa-fw fa-copy" /> Clones <Badge>1</Badge>
            </>
          }
        >
          <CloneDetectionTab />
        </Tab>
      </Tabs>
    </>
  )
}

export default PullRequest
