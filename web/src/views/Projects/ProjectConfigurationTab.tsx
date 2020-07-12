import React from 'react'
import { Panel } from 'react-bootstrap'
import { Wrapper } from 'store/wrapper'
import { IProject, IUser } from 'types'
import { Loader } from 'components/Loader'
import Select from 'react-select'

interface ProjectConfigurationTabProps {
  project: Wrapper<IProject>
  repoAdmins: Wrapper<IUser[]>
}

export const ProjectConfigurationTab = ({ project, repoAdmins }: ProjectConfigurationTabProps) => {
  if (project.isFetching || repoAdmins.isFetching || !project.value) {
    return <Loader />
  }
  const admins = repoAdmins.value.map(u => ({ label: `@${u.login}`, value: u.id }))
  return (
    <Panel>
      <Panel.Heading>Configuration</Panel.Heading>
      <Panel.Body>
        <Select isMulti defaultValue={admins} options={admins} />
      </Panel.Body>
    </Panel>
  )
}
