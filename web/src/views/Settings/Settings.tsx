import React from 'react'
import { Button, Grid, Panel } from 'react-bootstrap'

const Settings = () => (
  <div className="content">
    <Grid fluid className="tight">
      <Panel>
        <Panel.Body>
          <Button bsStyle="info" className="btn-fill">Save</Button><br /><br />
        </Panel.Body>
      </Panel>
    </Grid>
  </div>
)

export default Settings
