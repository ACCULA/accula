import React from 'react'
import { IProject, IPull } from 'types'
import Link from 'components/Link'
import { Avatar, IconButton, Typography } from '@material-ui/core'
import PullDate from 'components/PullDate'
import PullTable from 'components/PullTable'
import { GitHub, CheckCircleOutlineRounded, ScheduleRounded } from '@material-ui/icons'
import PullStatus from 'components/PullStatus/PullStatus'
import PullLabel from 'components/PullLabel'
import { ReactComponent as PrLogo } from 'images/pull_request.svg'
import { useStyles } from './styles'

interface PullOverviewTabProps {
  pull: IPull
  project: IProject
}

const PullOverviewTab = ({ pull, project }: PullOverviewTabProps) => {
  const classes = useStyles()

  return (
    <div>
      <div className={classes.pullOverview}>
        <div className={classes.authorView}>
          <Avatar className={classes.authorAvatar} src={pull.author.avatar} />
          <Link className={classes.authorLoginField} to={pull.author.url}>
            <IconButton
              className={classes.githubButton}
              color="default"
              aria-label="Log in with GitHub"
            >
              <GitHub />
            </IconButton>
            <span className={classes.authorLogin}>{`@${pull.author.login}`}</span>
          </Link>
        </div>
        <div className={classes.pullInfo}>
          <h1 className={classes.pullTitle}>{pull.title}</h1>
          <div className={classes.pullInfoField}>
            <PrLogo className={classes.prImage} />
            <span>
              Pull request into{' '}
              <Link to={pull.base.url}>
                <PullLabel text={pull.base.label} />
              </Link>{' '}
              from{' '}
              <Link to={pull.head.url}>
                <PullLabel text={pull.head.label} />
              </Link>
            </span>
          </div>
          <div className={classes.pullInfoField}>
            <CheckCircleOutlineRounded className={classes.prImage} />
            <span>Status</span>
            <PullStatus className={classes.pullStatus} open={pull.open} />
          </div>
          <div className={classes.pullInfoField}>
            <ScheduleRounded className={classes.prImage} />
            <div>
              <span>Created</span>
              <PullDate className={classes.dateField} date={pull.createdAt} />
            </div>
          </div>
          <div className={classes.pullInfoField}>
            <ScheduleRounded className={classes.prImage} />
            <div>
              <span>Last update</span>
              <PullDate className={classes.dateField} date={pull.updatedAt} />
            </div>
          </div>
        </div>
      </div>
      {pull.previousPulls.length !== 0 && (
        <div className={classes.pullRequestsBlock}>
          <Typography className={classes.titleOfBlock} gutterBottom>
            Previous pull requests
          </Typography>
          <PullTable pulls={pull.previousPulls} project={project} exclude={['author']} />
        </div>
      )}
    </div>
  )
}

export default PullOverviewTab
