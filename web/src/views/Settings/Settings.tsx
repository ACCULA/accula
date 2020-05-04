import React from 'react'
import { Grid } from 'react-bootstrap'
import Button from 'components/CustomButton'

import Card from 'components/Card'

const Settings = () => (
  <div className="content">
    <Grid fluid>
      <Card title="Settings">
        <Button bsStyle="info">Save</Button>
      </Card>
    </Grid>
  </div>
)

export default Settings
