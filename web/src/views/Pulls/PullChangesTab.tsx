import React, { useState } from 'react'
import { Button } from 'react-bootstrap'

import Prism from 'prismjs'
import 'prismjs/components/prism-java'

import { files } from 'data'
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
          leftTitle={`src/app/File${i}.java`}
          splitView={splitView} //
          oldValue={files.oldCode}
          newValue={files.newCode}
          compareMethod={DiffMethod.LINES}
          disableWordDiff
          renderContent={str => {
            if (str === undefined) {
              return <>{str}</>
            }
            return (
              <pre
                style={{ display: 'inline' }}
                /* eslint-disable-next-line react/no-danger */
                dangerouslySetInnerHTML={{
                  __html: Prism.highlight(str, Prism.languages.java, 'java')
                }}
              />
            )
          }}
        />
      ))}
    </>
  )
}
