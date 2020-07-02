import React, { useState } from 'react'
import { Button, FormControl, FormGroup } from 'react-bootstrap'

import { IPull } from 'types'
import { IPullComparesState } from 'store/pulls/types'
import { CodeDiff, DiffMethod } from 'components/CodeDiff'
import { Loader } from 'components/Loader'
import { getTitle } from './PullChangesTab'

interface PullCompareTabProps {
  pullId: number
  pulls: IPull[]
  compares: IPullComparesState
  compareWith: number
  onSelect: (compareWith: number) => void
}

export const PullCompareTab = ({
  pullId, //
  pulls,
  compares,
  compareWith,
  onSelect
}: PullCompareTabProps) => {
  const [splitView, setSplitView] = useState(false)

  return (
    <>
      <div>
        <h5 style={{ display: 'inline-block', marginRight: 15 }}>Compare with</h5>
        <FormGroup controlId="formControlsSelect" style={{ display: 'inline-block' }}>
          <FormControl
            componentClass="select"
            placeholder="select"
            onChange={e => onSelect(parseInt((e as any).target.value, 10))}
            value={compareWith}
          >
            <option value="0">...</option>
            {pulls &&
              pulls
                .filter(p => p.number !== pullId)
                .sort((a, b) => (a.number > b.number ? -1 : a.number === b.number ? 0 : 1))
                .map(pull => (
                  <option key={pull.number} value={pull.number}>
                    {`#${pull.number} ${pull.title} (${pull.head.label})`}
                  </option>
                ))}
          </FormControl>
        </FormGroup>
      </div>
      {compares.isFetching ? (
        <Loader />
      ) : !compares.value || !compares.value.length ? (
        <></>
      ) : (
        <>
          <div className="pull-right">
            <Button
              bsStyle="info"
              onClick={() => setSplitView(!splitView)}
              style={{ marginTop: -7 }}
            >
              {splitView ? 'Unified view' : 'Split view'}
            </Button>
          </div>
          <h5>{compares.value.length} files changed</h5>
          {compares.value.map((diff, i) => {
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
      )}
    </>
  )
}
