import React, { useState } from 'react'
import { CodeDiff, DiffMethod } from 'components/CodeDiff'
import { Loader } from 'components/Loader'
import { IPullDiffsState } from 'store/pulls/types'
import { SplitUnifiedViewButton } from 'components/CodeDiff/SplitUnifiedViewButton'

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
        {diffs.value.length > 0 && (
          <SplitUnifiedViewButton splitView={splitView} setSplitView={setSplitView} />
        )}
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
