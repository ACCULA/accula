import React from 'react'
import { ListGroup, ListGroupItem, Panel } from 'react-bootstrap'
import { Wrapper } from 'store/wrapper'
import { IProject } from 'types'
import { Loader } from 'components/Loader'
import { ProjectPanelHeading } from 'views/Projects/ProjectPanelHeading'

interface ProjectOverviewTabProps {
  project: Wrapper<IProject>
}

export const ProjectOverviewTab = ({ project }: ProjectOverviewTabProps) => {
  return project.isFetching ? (
    <Loader />
  ) : (
    <Panel className="project panel-project">
      <ProjectPanelHeading project={project.value} />
      <ListGroup>
        <ListGroupItem>{project.value.repoDescription}</ListGroupItem>
      </ListGroup>
    </Panel>
  )
}
