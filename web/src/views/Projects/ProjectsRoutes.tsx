import React from 'react'
import { Route, Switch } from 'react-router-dom'

import Pull from 'views/Pulls/Pull'
import Project from 'views/Projects/Project'
import Projects from 'views/Projects/Projects'
import { AppState } from 'store'
import { connect } from 'react-redux'

const mapStateToProps = (state: AppState) => ({
  auth: state.users.user.value !== undefined
})

const connector = connect(mapStateToProps, null)

const ProjectsRoutes = () => {
  return (
    <Switch>
      <Route path="/projects" exact component={Projects} />
      <Route path="/projects/:prId/:tab?" exact component={Project} />
      <Route path="/projects/:prId/pulls/:plId/:tab?" exact component={Pull} />
    </Switch>
  )
}

export default connector(ProjectsRoutes)
