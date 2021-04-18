import React from 'react'
import { IProject, IShortPull } from 'types'
import PullTable from 'components/PullTable'
import { ReactComponent as PrLogo } from 'images/pull_request.svg'
import EmptyContent from 'components/EmptyContent'
import { useStyles } from './styles'

interface ProjectPullsTabProps {
  project: IProject
  pulls: IShortPull[]
}

const ProjectPullsTab = ({ project, pulls }: ProjectPullsTabProps) => {
  const classes = useStyles()

  if (project.state === 'CONFIGURING') {
    return <EmptyContent className={classes.emptyContent} Icon={PrLogo} info="Project is syncing with GitHub" />
  }

  if (!pulls) {
    return <></>
  }

  if (pulls.length === 0) {
    return <EmptyContent className={classes.emptyContent} Icon={PrLogo} info="No pull requests" />
  }

  return <PullTable pulls={pulls} project={project} />
}

export default ProjectPullsTab
