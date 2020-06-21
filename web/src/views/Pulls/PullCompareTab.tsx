import React, { useEffect, useState } from 'react'
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
  onSelect: (compareWith: number) => void
}

export const PullCompareTab = ({ pullId, pulls, compares, onSelect }: PullCompareTabProps) => {
  const [compareWith, setCompareWith] = useState(0)
  const [splitView, setSplitView] = useState(false)

  useEffect(() => {
    if (compareWith !== 0 || (compares.value && compareWith !== compares.source)) {
      onSelect(compareWith)
    }
  }, [compares, compareWith, onSelect])

  return (
    <>
      <div>
        <h5 style={{ display: 'inline-block', marginRight: 15 }}>Compare with</h5>
        <FormGroup
          controlId="formControlsSelect"
          style={{ display: 'inline-block' }}
          onSubmit={e => console.log(e)}
        >
          <FormControl
            componentClass="select"
            placeholder="select"
            onChange={e => setCompareWith(parseInt((e as any).target.value, 10))}
          >
            <option value="0">...</option>
            {pulls &&
              pulls
                .filter(p => p.number !== pullId)
                .map(pull => (
                  <option key={pull.number} value={pull.number}>
                    {`${pull.title} (${pull.head.label})`}
                  </option>
                ))}
          </FormControl>
        </FormGroup>
      </div>
      {!compares.value || !compares.value.length ? (
        <></>
      ) : compares.isFetching ? (
        <Loader />
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
