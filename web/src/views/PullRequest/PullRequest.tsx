import React, { useState } from 'react'
import { useRouteMatch } from 'react-router-dom'
import {
  Badge,
  Button,
  Col,
  Grid,
  ListGroup,
  ListGroupItem,
  Panel,
  Row,
  Tab,
  Tabs
} from 'react-bootstrap'
import { formatDistanceToNow } from 'date-fns'

import { Project as Proj, PullRequest as Pull } from 'types'
import { files, projects, pulls } from 'data'
import Breadcrumbs from 'components/Breadcrumbs'
import FileDiffPanel from 'views/PullRequest/FileDiffPanel'

interface RouteParams {
  projectId: string
  pullRequestId: string
}

const PullOverviewTab = (pull: Pull) => {
  return (
    <>
      <Grid fluid style={{ padding: 0 }}>
        <Row>
          <Col xs={12} sm={4} md={4} lg={4}>
            <div className="card card-user">
              <div className="image">
                <img
                  src="https://i.mycdn.me/i?r=AyH4iRPQ2q0otWIFepML2LxR8bnRjmGChK1FId0cM99oJg&dpr=2"
                  alt="..."
                />
              </div>
              <div className="content">
                <div className="author">
                  <img className="avatar border-gray" src={pull.author.avatar} alt="..." />
                  <h4 className="title">
                    <code>{`@${pull.author.login}`}</code>
                    <br />
                    <small>{pull.author.name}</small>
                  </h4>
                </div>
              </div>
            </div>
          </Col>
          <Col xs={12} sm={8} md={8} lg={8}>
            <Panel className="pull-overview">
              <Panel.Heading>
                <Panel.Title>{pull.title}</Panel.Title>
              </Panel.Heading>
              <ListGroup>
                <ListGroupItem>
                  Pull request into{' '}
                  <a href={pull.base.url} target="_blank" rel="noopener noreferrer">
                    <code>{pull.base.label}</code>
                  </a>{' '}
                  from{' '}
                  <a href={pull.fork.url} target="_blank" rel="noopener noreferrer">
                    <code>{pull.fork.label}</code>
                  </a>
                </ListGroupItem>
                <ListGroupItem>
                  Pull request status
                  {pull.open ? (
                    <Badge className="badge-success">Open</Badge> //
                  ) : (
                    <Badge className="badge-danger">Closed</Badge>
                  )}
                </ListGroupItem>
                <ListGroupItem>
                  Checks status <Badge className="badge-success">Passed</Badge>
                </ListGroupItem>
                <ListGroupItem>
                  Clones found <Badge className="badge-warning">1 clone</Badge>
                </ListGroupItem>
                <ListGroupItem>
                  Created
                  <Badge className="badge-info">
                    {formatDistanceToNow(new Date(pull.createdAt))}
                  </Badge>
                </ListGroupItem>
                <ListGroupItem>
                  Last update
                  <Badge className="badge-info">
                    {formatDistanceToNow(new Date(pull.updatedAt))}
                  </Badge>
                </ListGroupItem>
              </ListGroup>
            </Panel>
          </Col>
        </Row>
      </Grid>
    </>
  )
}

const FileChangesTab = () => {
  const [splitView, setSplitView] = useState(false)
  return (
    <>
      <div className="pull-right">
        <Button bsStyle="info" onClick={() => setSplitView(!splitView)} style={{ marginTop: -7 }}>
          {splitView ? 'Unified view' : 'Split view'}
        </Button>
      </div>
      <h5>3 files changed</h5>
      {[1, 2, 3].map(i => (
        <FileDiffPanel
          key={i}
          fileName={`src/app/File${i}.java`}
          splitView={splitView} //
          oldCode={files.oldCode}
          newCode={files.newCode}
        />
      ))}
    </>
  )
}

const CloneDetectionTab = () => {
  return <>Clone detections</>
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
        <Tab eventKey={1} title="Overview">
          <PullOverviewTab {...pull} />
        </Tab>
        <Tab
          eventKey={2}
          title={
            <>
              Changes <Badge>4</Badge>
            </>
          }
        >
          <FileChangesTab />
        </Tab>
        <Tab
          eventKey={3}
          title={
            <>
              Clones <Badge>1</Badge>
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
