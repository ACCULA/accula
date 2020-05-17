import React, { useState } from 'react'
import { Panel } from 'react-bootstrap'
import ReactDiffViewer, { DiffMethod } from 'react-diff-viewer'
import Prism from 'prismjs'
import 'prismjs/components/prism-java'

interface FileDiffPanelProps {
  fileName: string
  oldCode: string
  newCode: string
  splitView: boolean
}

const FileDiffPanel = ({ fileName, oldCode, newCode, splitView }: FileDiffPanelProps) => {
  const [isShow, setShow] = useState(true)
  return (
    <Panel expanded={isShow} onToggle={show => setShow(show)}>
      <Panel.Heading onClick={() => setShow(!isShow)} className="pointer">
        <i className={`fas ${isShow ? 'fa-chevron-down' : 'fa-chevron-right'}`} />
        <pre style={{ marginLeft: 5 }}>{fileName}</pre>
      </Panel.Heading>
      <Panel.Collapse>
        <ReactDiffViewer
          oldValue={oldCode}
          newValue={newCode}
          splitView={splitView}
          styles={{
            gutter: {
              minWidth: 40,
              textAlign: 'center'
            }
          }}
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
      </Panel.Collapse>
    </Panel>
  )
}

export default FileDiffPanel
