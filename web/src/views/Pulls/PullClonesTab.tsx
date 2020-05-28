import React, { useState } from 'react'
import { Button } from 'react-bootstrap'

import { CodeDiff, DiffMethod } from 'components/CodeDiff'
import { IClone } from 'types'

interface PullClonesTabProps {
  clones: IClone[]
}

export const PullClonesTab = ({ clones }: PullClonesTabProps) => {
  const [splitView, setSplitView] = useState(false)
  return (
    <>
      <div className="pull-right">
        <Button bsStyle="info" onClick={() => setSplitView(!splitView)} style={{ marginTop: -7 }}>
          {splitView ? 'Unified view' : 'Split view'}
        </Button>
      </div>
      <h5>{clones?.length || 0} clones found</h5>
      {clones &&
        clones.map(clone => (
          <CodeDiff
            key={clone.id}
            leftTitle={
              <>
                Code cloned from{' '}
                <span className="left-title left-title-colored">
                  {`#${clone.target.pullId}@${clone.target.repoName}:${clone.target.file}`}
                </span>{' '}
                into{' '}
                <span className="right-title right-title-colored">
                  {clone.source.file}
                </span>
              </>
            }
            oldValue={atob(clone.target.code)}
            newValue={atob(clone.source.code)}
            splitView={splitView}
            showDiffOnly
            leftOffset={10}
            rightOffset={20}
            compareMethod={DiffMethod.WORDS_WITH_SPACE}
            // disableWordDiff
          />
        ))}
    </>
  )
}
