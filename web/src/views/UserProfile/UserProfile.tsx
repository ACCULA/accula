import React from 'react'
import { Col, ControlLabel, FormControl, FormGroup, Grid, Panel, Row } from 'react-bootstrap'

import Card from 'components/Card'
import FormInputs from 'components/FormInputs'
import UserCard from 'components/UserCard'
import CustomButton from 'components/CustomButton'
import { AppState } from 'store'
import { connect, ConnectedProps } from 'react-redux'

const mapStateToProps = (state: AppState) => ({
  user: state.users.user
})

const connector = connect(mapStateToProps, null)
type UserProfileProps = ConnectedProps<typeof connector>

const UserProfile = ({ user }: UserProfileProps) => {
  if (!user) {
    return <></>
  }
  return (
    <div className="content">
      <Grid fluid>
        <Row>
          <Panel>
            <Panel.Heading>Profile</Panel.Heading>
            <Panel.Body>@{user.login}: {user.name}</Panel.Body>
          </Panel>
        </Row>
        {false && <Row>
          <Col md={8}>
            <Card title="Edit Profile">
              <form>
                <FormInputs
                  ncols={['col-md-5', 'col-md-3', 'col-md-4']}
                  properties={[
                    {
                      label: 'Company (disabled)',
                      type: 'text',
                      bsClass: 'form-control',
                      placeholder: 'Company',
                      defaultValue: 'Creative Code Inc.',
                      disabled: true
                    },
                    {
                      label: 'Username',
                      type: 'text',
                      bsClass: 'form-control',
                      placeholder: 'Username',
                      defaultValue: 'michael23'
                    },
                    {
                      label: 'Email address',
                      type: 'email',
                      bsClass: 'form-control',
                      placeholder: 'Email'
                    }
                  ]}
                />
                <FormInputs
                  ncols={['col-md-6', 'col-md-6']}
                  properties={[
                    {
                      label: 'First name',
                      type: 'text',
                      bsClass: 'form-control',
                      placeholder: 'First name',
                      defaultValue: 'Mike'
                    },
                    {
                      label: 'Last name',
                      type: 'text',
                      bsClass: 'form-control',
                      placeholder: 'Last name',
                      defaultValue: 'Andrew'
                    }
                  ]}
                />
                <FormInputs
                  ncols={['col-md-12']}
                  properties={[
                    {
                      label: 'Adress',
                      type: 'text',
                      bsClass: 'form-control',
                      placeholder: 'Home Adress',
                      defaultValue: 'Bld Mihail Kogalniceanu, nr. 8 Bl 1, Sc 1, Ap 09'
                    }
                  ]}
                />
                <FormInputs
                  ncols={['col-md-4', 'col-md-4', 'col-md-4']}
                  properties={[
                    {
                      label: 'City',
                      type: 'text',
                      bsClass: 'form-control',
                      placeholder: 'City',
                      defaultValue: 'Mike'
                    },
                    {
                      label: 'Country',
                      type: 'text',
                      bsClass: 'form-control',
                      placeholder: 'Country',
                      defaultValue: 'Andrew'
                    },
                    {
                      label: 'Postal Code',
                      type: 'number',
                      bsClass: 'form-control',
                      placeholder: 'ZIP Code'
                    }
                  ]}
                />

                <Row>
                  <Col md={12}>
                    <FormGroup controlId="formControlsTextarea">
                      <ControlLabel>About Me</ControlLabel>
                      <FormControl
                        rows={5}
                        componentClass="textarea"
                        bsClass="form-control"
                        placeholder="Here can be your description"
                        defaultValue="Lamborghini Mercy, Your chick she so thirsty, I'm in that two seat Lambo."
                      />
                    </FormGroup>
                  </Col>
                </Row>
                <CustomButton bsStyle="info" pullRight fill type="submit">
                  Update Profile
                </CustomButton>
                <div className="clearfix" />
              </form>
            </Card>
          </Col>
          <Col md={4}>
            <UserCard
              bgImage="https://ununsplash.imgix.net/photo-1431578500526-4d9613015464?fit=crop&fm=jpg&h=300&q=75&w=400"
              avatar="https://pbs.twimg.com/profile_images/954020529391902720/jW4dnFtA_400x400.jpg"
              name="Mike Andrew"
              userName="michael24"
              description={<span>Lamborghini Mercy</span>}
              socials={
                <div>
                  <CustomButton simple>
                    <i className="fa fa-facebook-square" />
                  </CustomButton>
                  <CustomButton simple>
                    <i className="fa fa-twitter" />
                  </CustomButton>
                  <CustomButton simple>
                    <i className="fa fa-google-plus-square" />
                  </CustomButton>
                </div>
              }
            />
          </Col>
        </Row>}
      </Grid>
    </div>
  )
}

export default connector(UserProfile)
