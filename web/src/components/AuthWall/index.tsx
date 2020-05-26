import React from 'react'
import { Alert } from 'react-bootstrap'

export const AuthWall = () => (
  <div className="content">
    <Alert bsStyle="danger">
      <h4>Authentication required</h4>
      <p>Please sign in using your GitHub account to access this page!</p>
    </Alert>
  </div>
)
