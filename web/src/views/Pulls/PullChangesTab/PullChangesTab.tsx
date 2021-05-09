import React, { useEffect } from 'react'
import { LayersClearRounded } from '@material-ui/icons'
import EmptyContent from 'components/EmptyContent'
import LoadingWrapper from 'components/LoadingWrapper/LoadingWrapper'
import CodeDiffList from 'components/CodeDiffList/CodeDiffList'
import { IDiff } from 'types'
import { getPullTitle } from '../Pull'
import { AppDispatch, AppState } from "../../../store";
import { bindActionCreators } from "redux";
import { getDiffsAction } from "../../../store/pulls/actions";
import { connect, ConnectedProps } from "react-redux";

interface PullChangesTabProps extends PropsFromRedux {}

const PullChangesTab = ({ pull, diffs, getDiffs }: PullChangesTabProps) => {
  useEffect(() => {
      getDiffs(pull.projectId, pull.number)
  }, [pull, getDiffs])

  return (
    <div>
      <LoadingWrapper deps={[diffs]}>
        {diffs.value && (
          <>
            {diffs.value.length > 0 ? (
              <CodeDiffList
                title={`${diffs.value.length} files changed`}
                list={diffs.value}
                language="java"
                getDiffTitle={(diff: IDiff) => getPullTitle(diff.baseFilename, diff.headFilename)}
                getOldValue={(diff: IDiff) => diff.baseContent}
                getNewValue={(diff: IDiff) => diff.headContent}
                disableWordDiff
              />
            ) : (
              <EmptyContent Icon={LayersClearRounded} info="No changes" />
            )}
          </>
        )}
      </LoadingWrapper>
    </div>
  )
}

const mapStateToProps = (state: AppState) => ({
    pull: state.pulls.pull.value,
    diffs: state.pulls.diffs,
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
    getDiffs: bindActionCreators(getDiffsAction, dispatch),
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(PullChangesTab)
