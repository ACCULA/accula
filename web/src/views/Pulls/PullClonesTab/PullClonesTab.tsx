import React from 'react'
import { IClone, IProject, IPull } from 'types'
import { Button } from '@material-ui/core'
import { LibraryAddCheckRounded, RefreshRounded } from '@material-ui/icons'
import CloneLabel from "components/CloneLabel";
import EmptyContent from 'components/EmptyContent'
import { AppDispatch } from 'store'
import LoadingWrapper from 'components/LoadingWrapper'
import { connect, ConnectedProps } from 'react-redux'
import { bindActionCreators } from 'redux'
import { refreshClonesAction } from 'store/pulls/actions'
import { IPullClonesState } from 'store/pulls/types'
import { DiffMethod } from 'react-diff-viewer'
import CodeDiffList from 'components/CodeDiffList/CodeDiffList'
import { useStyles } from './styles'

interface PullClonesTabProps extends PropsFromRedux {
  clones: IPullClonesState
  isAdmin: boolean
  project: IProject
  pull: IPull
}

const PullClonesTab = ({ project, pull, clones, refreshClones, isAdmin }: PullClonesTabProps) => {
  const classes = useStyles()

  const getTitle = (clone: IClone): JSX.Element => {
    return (
      <>
        <span className={classes.cloneTitleText}> Code cloned from </span>
        <CloneLabel
          className={classes.fromTitle}
          type="removed"
          snippet={clone.source}
        />
        <br />
        <span className={classes.cloneTitleText}>into</span>
        <CloneLabel
          type="added"
          className={classes.intoTitle}
          snippet={clone.target}
        />
      </>
    )
  }

  return (
    <div>
      <LoadingWrapper deps={[clones]}>
        {clones.value && clones.value.length > 0 ? (
          <CodeDiffList
            title={`${clones.value.length} clones found`}
            list={clones.value}
            language="java"
            getDiffTitle={(clone: IClone) => getTitle(clone)}
            getOldValue={(clone: IClone) => clone.source.content}
            getNewValue={(clone: IClone) => clone.target.content}
            getLeftOffset={(clone: IClone) => clone.source.fromLine}
            getRightOffset={(clone: IClone) => clone.target.fromLine}
            compareMethod={DiffMethod.WORDS_WITH_SPACE}
            toolbarButtons={isAdmin ? [
              {
                tip: 'Refresh clones',
                onClick: () => refreshClones(project.id, pull.number),
                Icon: RefreshRounded
              }
            ] : []}
          />
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
      </LoadingWrapper>
    </div>
  )
}

const mapStateToProps = () => ({})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  refreshClones: bindActionCreators(refreshClonesAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(PullClonesTab)
