import React from 'react'
import { IProject, IShortPull } from 'types'
import PullTable from 'components/PullTable'
import { ReactComponent as PrLogo } from 'images/pull_request.svg'
import { useStyles } from './styles'

interface ProjectPullsTabProps {
  project: IProject
  pulls: IShortPull[]
}

const ProjectPullsTab = ({ project, pulls }: ProjectPullsTabProps) => {
  const classes = useStyles()

  if (!pulls) {
    return <></>
  }

  if (pulls.length === 0) {
    return (
      <div className={classes.emptyContent}>
        <PrLogo className={classes.prImage} />
        <span className={classes.prText}>No pull requests</span>
      </div>
    )
  }

  return <PullTable pulls={pulls} project={project} />
}

export default ProjectPullsTab
