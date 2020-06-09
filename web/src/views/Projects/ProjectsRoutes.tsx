import React from 'react'
import { Route, Switch } from 'react-router-dom'

import Pull from 'views/Pulls/Pull'
import Project from 'views/Projects/Project'
import Projects from 'views/Projects/Projects'
import { AppState } from 'store'
import { connect, ConnectedProps } from 'react-redux'
import { PrivateRoute } from 'components/PrivateRoute'

const mapStateToProps = (state: AppState) => ({
  auth: state.users.user.value !== undefined
})

const connector = connect(mapStateToProps, null)
type ProjectsRoutesProps = ConnectedProps<typeof connector>

const ProjectsRoutes = ({ auth }: ProjectsRoutesProps) => {
  return (
    <Switch>
      <Route path="/projects" exact component={Projects} />
      <Route path="/projects/:prId" exact component={Project} auth={auth} />
      <Route path="/projects/:prId/pulls/:plId/:tab?" exact component={Pull} auth={auth} />
    </Switch>
  )
}

export default connector(ProjectsRoutes)
