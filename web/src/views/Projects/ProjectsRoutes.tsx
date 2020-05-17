import React from 'react'
import { Route, Switch } from 'react-router-dom'

import Pull from 'views/Pulls/Pull'
import Project from 'views/Projects/Project'
import Projects from 'views/Projects/Projects'

export const ProjectsRoutes = () => (
  <Switch>
    <Route path="/projects" exact component={Projects} />
    <Route path="/projects/:prId" exact component={Project} />
    <Route path="/projects/:prId/pulls/:plId/:tab?" exact component={Pull} />
  </Switch>
)
