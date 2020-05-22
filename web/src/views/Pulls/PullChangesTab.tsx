import React, { useState } from 'react'
import { Button } from 'react-bootstrap'

import { files } from 'stubs'
import { CodeDiff, DiffMethod } from 'components/CodeDiff'

export const PullChangesTab = () => {
  const [splitView, setSplitView] = useState(false)
  return (
    <>
      <div className="pull-right">
        <Button bsStyle="info" onClick={() => setSplitView(!splitView)} style={{ marginTop: -7 }}>
          {splitView ? 'Unified view' : 'Split view'}
        </Button>
      </div>
      <h5>3 files changed</h5>
      {[1, 2, 3].map(i => (
        <CodeDiff
          key={i}
          leftTitle={<code>{`src/app/File${i}.java`}</code>}
          splitView={splitView} //
          oldValue={files.oldCode}
          newValue={files.newCode}
          compareMethod={DiffMethod.LINES}
          disableWordDiff
        />
      ))}
    </>
  )
}
