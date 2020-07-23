import React from 'react'
import { Badge, Col, Grid, ListGroup, ListGroupItem, Panel, Row } from 'react-bootstrap'
import { format, formatDistanceToNow } from 'date-fns'

import { IPull } from 'types'
import { GitHubLink, Link } from 'components/Link'
import { Wrapper } from 'store/wrapper'
import { Loader } from 'components/Loader'

const DATE_TITLE_FORMAT = "d MMMM yyyy 'at' HH:mm"

interface PullOverviewTabProps {
  pull: Wrapper<IPull>
}

export const PullOverviewTab = ({ pull }: PullOverviewTabProps) => {
  return pull.isFetching || !pull.value ? (
    <Loader />
  ) : (
    <Grid fluid style={{ padding: 0 }}>
      <Row>
        <Col xs={12} sm={8} md={8} lg={5}>
          <Panel className="pull-overview">
            <Panel.Heading>
              <Panel.Title>
                <GitHubLink to={pull.value.url}>{pull.value.title}</GitHubLink>
              </Panel.Title>
            </Panel.Heading>
            <ListGroup>
              <ListGroupItem>
                <i className="fas fa-fw fa-code-branch" /> Pull request into{' '}
                <Link to={pull.value.base.url}>
                  <code>{pull.value.base.label}</code>
                </Link>{' '}
                from{' '}
                <Link to={pull.value.head.url}>
                  <code>{pull.value.head.label}</code>
                </Link>
              </ListGroupItem>
              <ListGroupItem>
                <i className="fas fa-fw fa-eye" /> Pull request status
                {pull.value.open ? (
                  <Badge className="badge-success">Open</Badge> //
                ) : (
                  <Badge className="badge-danger">Closed</Badge>
                )}
              </ListGroupItem>
              {/* <ListGroupItem> */}
              {/*  <i className="far fa-fw fa-check-square" /> Checks status */}
              {/*  <Badge className="badge-success">Passed</Badge> */}
              {/* </ListGroupItem> */}
              <ListGroupItem>
                <i className="far fa-fw fa-clock" /> Created
                <Badge
                  className="badge-info"
                  title={format(new Date(pull.value.createdAt), DATE_TITLE_FORMAT)}
                >
                  {formatDistanceToNow(new Date(pull.value.createdAt), { addSuffix: true })}
                </Badge>
              </ListGroupItem>
              <ListGroupItem>
                <i className="far fa-fw fa-clock" /> Last update
                <Badge
                  className="badge-info" //
                  title={format(new Date(pull.value.updatedAt), DATE_TITLE_FORMAT)}
                >
                  {formatDistanceToNow(new Date(pull.value.updatedAt), { addSuffix: true })}
                </Badge>
              </ListGroupItem>
            </ListGroup>
          </Panel>
        </Col>

        <Col xs={12} sm={4} md={4} lg={3}>
          <Panel className="panel-user">
            <Panel.Heading>
              <Panel.Title>Author</Panel.Title>
            </Panel.Heading>
            <ListGroup>
              <ListGroupItem className="text-center">
                <img className="avatar border-gray" src={pull.value.author.avatar} alt="..." />
                <h4 className="title">
                  <GitHubLink to={pull.value.author.url}>
                    <code>{`@${pull.value.author.login}`}</code>
                  </GitHubLink>
                </h4>
              </ListGroupItem>
            </ListGroup>
          </Panel>
        </Col>

        <Col xs={12} sm={12} md={12} lg={4}>
          <Panel className="pull-overview">
            <Panel.Heading>
              <Panel.Title>Previous pull requests</Panel.Title>
            </Panel.Heading>
            <ListGroup>
              {pull.value.previousPulls.length > 0 ? (
                pull.value.previousPulls.map(prevPull => (
                  <ListGroupItem key={prevPull.number}>
                    {prevPull.open ? (
                      <Badge className="badge-success prev-pull-badge">Open</Badge> //
                    ) : (
                      <Badge className="badge-danger prev-pull-badge">Closed</Badge>
                    )}
                    <Link to={`/projects/${prevPull.projectId}/pulls/${prevPull.number}`}>
                      {`#${prevPull.number}: ${prevPull.title}`}
                    </Link>
                  </ListGroupItem>
                ))
              ) : (
                <ListGroupItem>
                  This is the first pull request form <code>@{pull.value.author.login}</code>
                </ListGroupItem>
              )}
            </ListGroup>
          </Panel>
        </Col>
      </Row>
    </Grid>
  )
}
