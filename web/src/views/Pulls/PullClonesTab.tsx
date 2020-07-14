import React, { useState } from 'react'
import { Button } from 'react-bootstrap'

import { CodeDiff, DiffMethod } from 'components/CodeDiff'
import { Loader } from 'components/Loader'
import { IPullClonesState } from 'store/pulls/types'

interface PullClonesTabProps {
  clones: IPullClonesState
}

export const PullClonesTab = ({ clones }: PullClonesTabProps) => {
  const [splitView, setSplitView] = useState(false)
  return clones.isFetching ? (
    <Loader />
  ) : (
    <>
      <div className="pull-right">
        {clones.value && clones.value.length > 0 && (
          <Button bsStyle="info" onClick={() => setSplitView(!splitView)} style={{ marginTop: -7 }}>
            {splitView ? 'Unified view' : 'Split view'}
          </Button>
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
    </>
  )
}
