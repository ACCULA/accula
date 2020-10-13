import React from 'react'
import { Route, Switch } from 'react-router-dom'

import Pull from 'views/Pulls/Pull'
import Project from 'views/Projects/Project'
import Projects from 'views/Projects/Projects'

const ProjectsRoutes = () => {
  return (
    <Switch>
      <Route path="/projects" exact component={Projects} />
      <Route path="/projects/:prId/:tab?" exact component={Project} />
      <Route
        path="/projects/:prId/pulls/:plId/:tab?"
        exact
        render={(props: any) => {
          const {
            match: {
              params: { prId, plId, tab }
            }
          } = props
          return <Pull key={`prId=${prId}&plId=${plId}&tab=${tab}`} {...props} />
        }}
      />
    </Switch>
  )
}

export default ProjectsRoutes
