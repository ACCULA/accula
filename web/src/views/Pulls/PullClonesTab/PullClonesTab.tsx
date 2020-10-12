import React from 'react'
import { IClone, IProject, IPull } from 'types'
import { Button, IconButton, Tooltip, Typography, useTheme } from '@material-ui/core'
import { LibraryAddCheckRounded, RefreshRounded } from '@material-ui/icons'
import EmptyContent from 'components/EmptyContent'
import CodeDiff from 'components/CodeDiff/CodeDiff'
import PullLabel from 'components/PullLabel'
import { AppDispatch, AppState } from 'store'
import SplitUnifiedViewButton from 'components/CodeDiff/SplitUnifiedViewButton'
import { connect, ConnectedProps } from 'react-redux'
import { bindActionCreators } from 'redux'
import { refreshClonesAction } from 'store/pulls/actions'
import { IPullClonesState } from 'store/pulls/types'
import { DiffMethod } from 'react-diff-viewer'
import { useStyles } from './styles'

interface PullClonesTabProps extends PropsFromRedux {
  clones: IPullClonesState
  isAdmin: boolean
  project: IProject
  pull: IPull
}

const PullClonesTab = ({
  project,
  pull,
  clones,
  refreshClones,
  isAdmin,
  settings
}: PullClonesTabProps) => {
  const theme = useTheme()
  const classes = useStyles()

  if (!clones.value) {
    return <></>
  }

  const getTitle = (clone: IClone): JSX.Element => {
    return (
      <>
        <span className={classes.cloneTitleText}> Code cloned from </span>
        <PullLabel
          className={classes.fromTitle}
          text={`#${clone.source.pullNumber}@${clone.source.repo}:${clone.source.file}`}
        />
        <span className={classes.cloneTitleText}>into</span>
        <PullLabel className={classes.intoTitle} text={clone.target.file} />
      </>
    )
  }

  return (
    <div>
      {clones.value.length > 0 ? (
        <>
          <div className={classes.titleField}>
            <Typography className={classes.title} gutterBottom>
              {clones.value.length} clones found
            </Typography>
            {isAdmin && (
              <Tooltip title="Refresh clones" placement="top">
                <IconButton onClick={() => refreshClones(project.id, pull.number)}>
                  <RefreshRounded />
                </IconButton>
              </Tooltip>
            )}
            <SplitUnifiedViewButton />
          </div>
          {clones.value.map(clone => (
            <CodeDiff
              key={clone.id}
              title={getTitle(clone)}
              splitView={settings.splitCodeView === 'unified'}
              oldValue={clone.source.content}
              newValue={clone.target.content}
              language="java"
              showDiffOnly
              useDarkTheme={theme.palette.type === 'dark'}
              defaultExpanded
              compareMethod={DiffMethod.WORDS_WITH_SPACE}
              // disableWordDiff
            />
          ))}
        </>
      ) : (
        <EmptyContent Icon={LibraryAddCheckRounded} info="No clones">
          <>
            {isAdmin && (
              <>
                <Button
                  className={classes.refreshButton}
                  variant="contained"
                  color="secondary"
                  onClick={() => refreshClones(project.id, pull.number)}
                >
                  Refresh clones
                </Button>
              </>
            )}
          </>
        </EmptyContent>
      )}
    </div>
  )
}

const mapStateToProps = (state: AppState) => ({
  settings: state.settings.settings
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  refreshClones: bindActionCreators(refreshClonesAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(PullClonesTab)
