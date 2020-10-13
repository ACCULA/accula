import React from 'react'
import { LayersClearRounded } from '@material-ui/icons'
import EmptyContent from 'components/EmptyContent'
import LoadingWrapper from 'components/LoadingWrapper/LoadingWrapper'
import { IPullDiffsState } from 'store/pulls/types'
import CodeDiffList from 'components/CodeDiffList/CodeDiffList'
import { IDiff } from 'types'
import { getPullTitle } from '../Pull'

interface PullChangesTabProps {
  diffs: IPullDiffsState
}

const PullChangesTab = ({ diffs }: PullChangesTabProps) => {
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

export default PullChangesTab
