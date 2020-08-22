import React, { useEffect, useState } from 'react'
import { ControlLabel, FormControl, FormGroup, HelpBlock, Panel } from 'react-bootstrap'
import { Wrapper } from 'store/wrapper'
import { IProject, IProjectConf, IUser } from 'types'
import { Loader } from 'components/Loader'
import Select from 'react-select'
import { LoadingButton } from 'components/LoadingButton'

interface ProjectConfigurationTabProps {
  project: Wrapper<IProject>
  projectConf: Wrapper<IProjectConf>
  updateConf: (conf: IProjectConf) => void
  updateConfState: [boolean, string]
  repoAdmins: Wrapper<IUser[]>
}

const createLabel = (user: IUser) => ({
  label: user.name ? `@${user.login} (${user.name})` : `@${user.login}`,
  value: user.id
})

export const ProjectConfigurationTab = ({
  project, //
  projectConf,
  updateConf,
  updateConfState,
  repoAdmins
}: ProjectConfigurationTabProps) => {
  const [adminOptions, setAdminOptions] = useState(null)
  const [admins, setAdmins] = useState(null)
  const [cloneMinLineCount, setCloneMinLineCount] = useState(0)

  useEffect(() => {
    if (repoAdmins.value) {
      const values = repoAdmins.value //
        .filter(u => u.id !== project.value.creatorId)
        .map(createLabel)
      setAdminOptions(values)
    }
  }, [repoAdmins.value, project.value])

  useEffect(() => {
    if (adminOptions && projectConf.value) {
      const values = adminOptions //
        .filter(u => projectConf.value.admins.indexOf(u.value) !== -1)
      setAdmins(values)
    }
  }, [projectConf.value, adminOptions])

  useEffect(() => {
    if (projectConf.value) {
      setCloneMinLineCount(projectConf.value.cloneMinLineCount)
    }
  }, [projectConf.value])

  return project.isFetching ||
    repoAdmins.isFetching ||
    projectConf.isFetching ||
    !project.value ||
    !repoAdmins.value ||
    !projectConf.value ||
    !admins ? (
    <Loader />
  ) : (
    <Panel>
      <Panel.Heading>Project configuration</Panel.Heading>
      <Panel.Body>
        <FormGroup controlId="formControlsSelect">
          <ControlLabel>Project admins</ControlLabel>
          <Select
            isMulti //
            defaultValue={admins}
            options={adminOptions}
            onChange={e => setAdmins((e as any) || [])}
          />
          <HelpBlock>
            Admin can resolve clones and update project settings. Only a repository admin can become
            a project admin.
          </HelpBlock>
        </FormGroup>
        <FormGroup>
          <ControlLabel>Clone minimum line count</ControlLabel>
          <FormControl
            type="number" //
            placeholder="..."
            value={cloneMinLineCount}
            onChange={e => setCloneMinLineCount((e.target as any).value)}
          />
          <HelpBlock>Minimum source code line count to be considered as a clone</HelpBlock>
        </FormGroup>
        <LoadingButton
          bsStyle="info" //
          className="pull-right"
          onClick={() =>
            updateConf({
              admins: admins.map(u => u.value),
              cloneMinLineCount
            })
          }
          isLoading={updateConfState[0]}
        >
          Save
        </LoadingButton>
      </Panel.Body>
    </Panel>
  )
}
