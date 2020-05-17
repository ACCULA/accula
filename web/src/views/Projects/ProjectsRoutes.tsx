import { Route, Switch } from 'react-router-dom'
import Pull from 'views/Pulls/Pull'
import React from 'react'
import Project from 'views/Projects/Project'
import Projects from 'views/Projects/Projects'

export const ProjectsRoutes = () => (
  <Switch>
    <Route path="/projects" exact component={Projects} />
    <Route path="/projects/:projectId" exact component={Project} />
    <Route path="/projects/:projectId/pulls/:pullId/:tabName?" exact component={Pull} />
  </Switch>
)
