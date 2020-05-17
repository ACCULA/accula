import React, { useState } from 'react'
import { Button } from 'react-bootstrap'

import { files } from 'data'
import FileDiffPanel from './FileDiffPanel'

export const PullFileChangesTab = () => {
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
        <FileDiffPanel
          key={i}
          fileName={`src/app/File${i}.java`}
          splitView={splitView} //
          oldCode={files.oldCode}
          newCode={files.newCode}
        />
      ))}
    </>
  )
}
