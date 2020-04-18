import React from 'react'
import { Col, Grid, Row, Table } from 'react-bootstrap'

import Card from 'components/Card'

const PullRequestList = () => (
  <div className="content">
    <Grid fluid>
      <Row>
        <Col md={12}>
          <Card
            title="Striped Table with Hover"
            category="Here is a subtitle for this table"
            ctTableFullWidth
            ctTableResponsive
            content={
              <Table striped hover>
                <thead>
                  <tr>
                    {[].map((prop, key) => {
                      return <th key={key}>{prop}</th>
                    })}
                  </tr>
                </thead>
                <tbody>
                  {[].map((prop, key) => {
                    return (
                      <tr key={key}>
                        {prop.map((prop, key) => {
                          return <td key={key}>{prop}</td>
                        })}
                      </tr>
                    )
                  })}
                </tbody>
              </Table>
            }
          />
        </Col>

        <Col md={12}>
          <Card
            plain
            title="Striped Table with Hover"
            category="Here is a subtitle for this table"
            ctTableFullWidth
            ctTableResponsive
            content={
              <Table hover>
                <thead>
                  <tr>
                    {[].map((prop, key) => (
                      <th key={key}>{prop}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {[].map((prop, key) => {
                    return (
                      <tr key={key}>
                        {prop.map((prop, key) => (
                          <td key={key}>{prop}</td>
                        ))}
                      </tr>
                    )
                  })}
                </tbody>
              </Table>
            }
          />
        </Col>
      </Row>
    </Grid>
  </div>
)

export default PullRequestList
