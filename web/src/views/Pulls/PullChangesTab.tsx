import React, { useState } from 'react'
import { Button } from 'react-bootstrap'
import { CodeDiff, DiffMethod } from 'components/CodeDiff'
import { Loader } from 'components/Loader'
import { IPullDiffsState } from 'store/pulls/types'

interface PullChangesTabProps {
  diffs: IPullDiffsState
}

export const getTitle = (base?: string, head?: string): JSX.Element => {
  if (base && head) {
    if (base === head) {
      return <code>{base}</code>
    }
    return <code className="removed">{`${base} -> ${head}`}</code>
  }
  if (base) {
    return <code className="removed">{base}</code>
  }
  if (head) {
    return <code className="added">{head}</code>
  }
  return <code />
}

export const PullChangesTab = ({ diffs }: PullChangesTabProps) => {
  const [splitView, setSplitView] = useState(false)
  return diffs.isFetching || !diffs.value ? (
    <Loader />
  ) : (
    <>
      <div className="pull-right">
        <Button bsStyle="info" onClick={() => setSplitView(!splitView)} style={{ marginTop: -7 }}>
          {splitView ? 'Unified view' : 'Split view'}
        </Button>
      </div>
      <h5>{diffs.value.length} files changed</h5>
      {diffs.value.map((diff, i) => {
        const { baseContent, baseFilename, headFilename, headContent } = diff
        return (
          <CodeDiff
            key={i}
            leftTitle={getTitle(baseFilename, headFilename)}
            splitView={splitView} //
            oldValue={baseContent}
            newValue={headContent}
            compareMethod={DiffMethod.LINES}
            disableWordDiff
          />
        )
      })}
    </>
  )
}
