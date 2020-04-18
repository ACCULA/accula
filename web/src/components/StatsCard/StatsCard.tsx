import React from 'react'
import { Col, Row } from 'react-bootstrap'

interface StatsCardProps {
  bigIcon: string
  statsText: string
  statsValue: string
  statsIcon: string
  statsIconText: string
}

export const StatsCard = (props: StatsCardProps) => {
  const { statsText, statsIconText, statsValue, bigIcon, statsIcon } = props
  return (
    <div className="card card-stats">
      <div className="content">
        <Row>
          <Col xs={5}>
            <div className="icon-big text-center icon-warning">
              {bigIcon}
            </div>
          </Col>
          <Col xs={7}>
            <div className="numbers">
              <p>{statsText}</p>
              {statsValue}
            </div>
          </Col>
        </Row>
        <div className="footer">
          <hr />
          <div className="stats">
            {statsIcon} {statsIconText}
          </div>
        </div>
      </div>
    </div>
  )
}

export default StatsCard
