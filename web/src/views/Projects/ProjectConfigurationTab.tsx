import React from 'react'
import { Panel } from 'react-bootstrap'
import { Wrapper } from 'store/wrapper'
import { IProject } from 'types'
import { Loader } from 'components/Loader'

interface ProjectConfigurationTabProps {
  project: Wrapper<IProject>
}

export const ProjectConfigurationTab = ({ project }: ProjectConfigurationTabProps) => {
  return project.isFetching || !project.value ? (
    <Loader />
  ) : (
    <Panel>
      <Panel.Heading>Configuration</Panel.Heading>
      <Panel.Body>Forms</Panel.Body>
    </Panel>
  )
}
