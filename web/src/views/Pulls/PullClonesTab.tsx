import React, { useState } from 'react'
import { Button } from 'react-bootstrap'

import { CodeDiff, DiffMethod } from 'components/CodeDiff'
import { IPullClonesState } from 'store/pulls/types'
import { SplitUnifiedViewButton } from 'components/CodeDiff/SplitUnifiedViewButton'
import { LoadingWrapper } from 'components/LoadingWrapper'

interface PullClonesTabProps {
  clones: IPullClonesState
  refreshClones: () => void
  isAdmin: boolean
}

export const PullClonesTab = ({
  clones, //
  refreshClones,
  isAdmin
}: PullClonesTabProps) => {
  const [splitView, setSplitView] = useState(false)

  return (
    <LoadingWrapper deps={[clones]}>
      <div className="pull-right">
        {isAdmin && (
          <Button
            bsStyle="info" //
            className="pull-refresh-clone"
            onClick={refreshClones}
          >
            <i className="fas fa-fw fa-sync-alt" /> Refresh
          </Button>
        )}
        {clones.value && clones.value.length > 0 && (
          <SplitUnifiedViewButton splitView={splitView} setSplitView={setSplitView} />
        )}
      </div>
      <h5>{clones.value?.length || 0} clones found</h5>
      {clones.value &&
        clones.value.map(clone => (
          <CodeDiff
            key={clone.id}
            leftTitle={
              <>
                Code cloned from{' '}
                <span className="left-title left-title-colored">
                  {`#${clone.source.pullNumber}@${clone.source.repo}:${clone.source.file}`}
                </span>{' '}
                into <span className="right-title right-title-colored">{clone.target.file}</span>
              </>
            }
            oldValue={clone.target.content}
            newValue={clone.source.content}
            splitView={splitView}
            showDiffOnly
            leftOffset={clone.target.fromLine}
            rightOffset={clone.source.fromLine}
            compareMethod={DiffMethod.WORDS_WITH_SPACE}
            // disableWordDiff
          />
        ))}
    </LoadingWrapper>
  )
}
