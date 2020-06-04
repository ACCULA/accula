import React, { useState } from 'react'
import { Col, Grid, Panel, Row } from 'react-bootstrap'
import { Graph, GraphConfiguration, GraphData, GraphLink, GraphNode } from 'react-d3-graph'

interface Node extends GraphNode {
  x?: number
  y?: number
  title?: string
}

interface Link extends GraphLink {
  title?: string
  value?: number
}

export const PullGraphTab = () => {
  const data: GraphData<Node, Link> = {
    nodes: [
      { id: '1', title: '2019-highload/#1/@vaddya', x: 100, y: 200 },
      { id: '2', title: '2019-highload/#2/@lamtev', x: 300, y: 100 },
      { id: '3', title: '2019-highload/#3/@vaddya', x: 500, y: 300 }
    ],
    links: [
      { source: '1', target: '3', value: 5, title: '5' },
      { source: '2', target: '3', value: 10, title: '10' }
    ]
  }

  const [curr, setCurr] = useState([])

  // $tango: #ED812B;
  // $matisse: #2178A3;
  const config: Partial<GraphConfiguration<GraphNode, GraphLink>> = {
    nodeHighlightBehavior: true,
    directed: true,
    highlightDegree: 0,
    // staticGraph: true,
    node: {
      color: '#2178A3',
      size: 500,
      fontSize: 18,
      highlightFontSize: 18,
      mouseCursor: 'auto',
      labelProperty: 'title' as any,
    },
    link: {
      highlightColor: '#2178A3',
      color: '#ED812B',
      renderLabel: true,
      fontSize: 18,
      fontColor: '#A0A0A0',
      strokeWidth: 3,
      labelProperty: 'title' as any,
      semanticStrokeWidth: true,
    }
  }

  const onClickNode = nodeId => {
    console.log(`Clicked node ${nodeId}`)
  }

  const onDoubleClickNode = nodeId => {
    // open pull page
    console.log(`Double clicked node ${nodeId}`)
  }

  const onClickLink = (source, target) => {
    setCurr([source, target])
    console.log(`Clicked link between ${source} and ${target}`)
  }

  return (
    <Grid fluid style={{ padding: 0 }}>
      <Row>
        <Col xs={12} sm={12} md={8} lg={8}>
          <Panel>
            <Panel.Heading>
              <Panel.Title>Borrowing graph</Panel.Title>
            </Panel.Heading>
            <Panel.Body className="clone-graph" style={{ overflow: 'hidden' }}>
              <Graph
                id="clone-graph" // id is mandatory, if no id is defined rd3g will throw an error
                data={data}
                config={config}
                onClickNode={onClickNode}
                onDoubleClickNode={onDoubleClickNode}
                onClickLink={onClickLink}
              />
            </Panel.Body>
          </Panel>
        </Col>
        <Col xs={12} sm={12} md={4} lg={4}>
          <Panel>
            <Panel.Heading>
              <Panel.Title>Current</Panel.Title>
            </Panel.Heading>
            <Panel.Body>{curr && curr.length > 0 && <>{`${curr[0]} -> ${curr[1]}`}</>}</Panel.Body>
          </Panel>
        </Col>
      </Row>
    </Grid>
  )
}
