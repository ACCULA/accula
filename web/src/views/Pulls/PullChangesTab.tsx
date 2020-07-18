import React, { useState } from 'react'
import { CodeDiff, DiffMethod } from 'components/CodeDiff'
import { IPullDiffsState } from 'store/pulls/types'
import { SplitUnifiedViewButton } from 'components/CodeDiff/SplitUnifiedViewButton'
import { LoadingWrapper } from 'components/LoadingWrapper'

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

  return (
    <LoadingWrapper deps={[diffs]}>
      {diffs.value && (
        <>
          <div className="pull-right">
            {diffs.value.length > 0 && (
              <SplitUnifiedViewButton splitView={splitView} setSplitView={setSplitView} />
            )}
          </div>
          <h5>{diffs.value.length} files changed</h5>
          {diffs.value.map(({ baseContent, baseFilename, headFilename, headContent }, i) => (
            <CodeDiff
              key={i}
              leftTitle={getTitle(baseFilename, headFilename)}
              splitView={splitView} //
              oldValue={baseContent}
              newValue={headContent}
              compareMethod={DiffMethod.LINES}
              disableWordDiff
            />
          ))}
        </>
      )}
    </LoadingWrapper>
  )
}
