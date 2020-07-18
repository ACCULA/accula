import React from 'react'
import { ListGroup, ListGroupItem, Panel } from 'react-bootstrap'
import { Wrapper } from 'store/wrapper'
import { IProject } from 'types'
import { ProjectPanelHeading } from 'views/Projects/ProjectPanelHeading'
import { LoadingWrapper } from 'components/LoadingWrapper'

interface ProjectOverviewTabProps {
  project: Wrapper<IProject>
}

export const ProjectOverviewTab = ({ project }: ProjectOverviewTabProps) => (
  <LoadingWrapper deps={[project]}>
    {project.value && (
      <Panel className="project panel-project">
        <ProjectPanelHeading project={project.value} />
        <ListGroup>
          <ListGroupItem>{project.value.repoDescription}</ListGroupItem>
        </ListGroup>
      </Panel>
    )}
  </LoadingWrapper>
)
