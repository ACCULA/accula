import React, { useEffect, useState } from 'react'
import { ControlLabel, FormControl, FormGroup, HelpBlock, Panel } from 'react-bootstrap'
import { Wrapper } from 'store/wrapper'
import { IProject, IProjectConf, IUser } from 'types'
import { Loader } from 'components/Loader'
import { Select } from 'components/Select'
import { LoadingButton } from 'components/LoadingButton'

interface ProjectConfigurationTabProps {
  project: Wrapper<IProject>
  projectConf: Wrapper<IProjectConf>
  updateConf: (conf: IProjectConf) => void
  updateConfState: [boolean, string]
  repoAdmins: Wrapper<IUser[]>
  baseFiles: Wrapper<String[]>
}

const createAdminLabel = (user: IUser) => ({
  label: user.name ? `@${user.login} (${user.name})` : `@${user.login}`,
  value: user.id
})

const createFileLabel = (file: string) => ({
  label: file,
  value: file
})

export const ProjectConfigurationTab = ({
  project, //
  projectConf,
  updateConf,
  updateConfState,
  repoAdmins,
  baseFiles
}: ProjectConfigurationTabProps) => {
  const [adminOptions, setAdminOptions] = useState(null)
  const [admins, setAdmins] = useState(null)
  const [excludedFilesOptions, setExcludedFilesOptions] = useState(null)
  const [excludedFiles, setExcludedFiles] = useState(null)
  const [cloneMinTokenCount, setCloneMinTokenCount] = useState(0)
  const [fileMinSimilarityIndex, setFileMinSimilarityIndex] = useState(0)

  useEffect(() => {
    if (repoAdmins.value && project.value) {
      const values = repoAdmins.value //
        .filter(u => u.id !== project.value.creatorId)
        .map(createAdminLabel)
      setAdminOptions(values)
    }
  }, [repoAdmins.value, project.value])

  useEffect(() => {
    if (baseFiles.value) {
      const values = baseFiles.value.map(createFileLabel)
      setExcludedFilesOptions(values)
    }
  }, [baseFiles.value])

  useEffect(() => {
    if (adminOptions && projectConf.value) {
      const values = adminOptions //
        .filter(u => projectConf.value.admins.indexOf(u.value) !== -1)
      setAdmins(values)
    }
  }, [projectConf.value, adminOptions])

  useEffect(() => {
    if (excludedFilesOptions && projectConf.value) {
      const values = excludedFilesOptions //
        .filter(f => projectConf.value.excludedFiles.indexOf(f.value) !== -1)
      setExcludedFiles(values)
    }
  }, [projectConf.value, excludedFilesOptions])

  useEffect(() => {
    if (projectConf.value) {
      setCloneMinTokenCount(projectConf.value.cloneMinTokenCount)
    }
  }, [projectConf.value])

  useEffect(() => {
    if (projectConf.value) {
      setFileMinSimilarityIndex(projectConf.value.fileMinSimilarityIndex)
    }
  }, [projectConf.value])

  return project.isFetching ||
    repoAdmins.isFetching ||
    baseFiles.isFetching ||
    projectConf.isFetching ||
    !project.value ||
    !repoAdmins.value ||
    !baseFiles.value ||
    !projectConf.value ||
    !admins ||
    !excludedFiles ? (
    <Loader />
  ) : (
    <Panel>
      <Panel.Heading>Project configuration</Panel.Heading>
      <Panel.Body>
        <FormGroup>
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
          <ControlLabel>Clone minimum token count</ControlLabel>
          <FormControl
            type="number" //
            placeholder="..."
            value={cloneMinTokenCount}
            onChange={e => setCloneMinTokenCount((e.target as any).value)}
          />
          <HelpBlock>Minimum source code token count to be considered as a clone</HelpBlock>
        </FormGroup>
        <FormGroup>
          <ControlLabel>File minimum similarity index</ControlLabel>
          <FormControl
            type="number" //
            placeholder="..."
            value={fileMinSimilarityIndex}
            onChange={e => setFileMinSimilarityIndex((e.target as any).value)}
          />
          <HelpBlock>Minimum similarity percent to consider file as renamed</HelpBlock>
        </FormGroup>
        <FormGroup>
          <ControlLabel>Excluded files</ControlLabel>
          <Select
            isMulti //
            defaultValue={excludedFiles}
            options={excludedFilesOptions}
            value={excludedFiles}
            onChange={e => setExcludedFiles((e as any) || [])}
          />
          <HelpBlock>Files that will be excluded during code clone analysis</HelpBlock>
        </FormGroup>
        <LoadingButton
          bsStyle="info" //
          className="pull-right"
          onClick={() =>
            updateConf({
              admins: admins.map(u => u.value),
              cloneMinTokenCount,
              fileMinSimilarityIndex,
              excludedFiles: excludedFiles.map(s => s.value)
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
