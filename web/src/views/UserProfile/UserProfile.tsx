import React from 'react'
import { Grid, Panel, Row } from 'react-bootstrap'

import { Loader } from 'components/Loader'
import { AppState } from 'store'
import { connect, ConnectedProps } from 'react-redux'

const mapStateToProps = (state: AppState) => ({
  isFetching: state.users.user.isFetching || !state.users.user.value,
  user: state.users.user.value
})

const connector = connect(mapStateToProps, null)
type UserProfileProps = ConnectedProps<typeof connector>

const UserProfile = ({ isFetching, user }: UserProfileProps) => {
  return isFetching ? (
    <Loader />
  ) : (
    <div className="content">
      <Grid fluid>
        <Row>
          <Panel>
            <Panel.Heading>Profile</Panel.Heading>
            <Panel.Body>
              @{user.login}: {user.name}
            </Panel.Body>
          </Panel>
        </Row>
      </Grid>
    </div>
  )
}

export default connector(UserProfile)
