import React, { useState } from 'react'

import { IShortPull } from 'types'
import { Wrapper } from 'store/wrapper'
import { IPullComparesState } from 'store/pulls/types'

interface PullCompareTabProps {
  pullId: number
  pulls: Wrapper<IShortPull[]>
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
    <></>
    // <LoadingWrapper deps={[pulls]}>
    //   <div>
    //     <h5 style={{ display: 'inline-block', marginRight: 15 }}>Compare with</h5>
    //     <FormGroup controlId="formControlsSelect" style={{ display: 'inline-block' }}>
    //       <FormControl
    //         // componentClass="select"
    //         placeholder="select"
    //         onChange={e => onSelect(parseInt((e as any).target.value, 10))}
    //         value={compareWith}
    //       >
    //         <option value="0">...</option>
    //         {pulls.value &&
    //           pulls.value
    //             .filter(p => p.number !== pullId)
    //             .sort((a, b) => (a.number > b.number ? -1 : a.number === b.number ? 0 : 1))
    //             .map(pull => (
    //               <option key={pull.number} value={pull.number}>
    //                 {`#${pull.number} ${pull.title} (@${pull.author.login})`}
    //               </option>
    //             ))}
    //       </FormControl>
    //     </FormGroup>
    //   </div>
    //   {compares.isFetching ? (
    //     <Loader />
    //   ) : !compares.value || !compares.value.length ? (
    //     <></>
    //   ) : (
    //     <>
    //       <div className="pull-right">
    //         <SplitUnifiedViewButton splitView={splitView} setSplitView={setSplitView} />
    //       </div>
    //       <h5>{compares.value.length} files changed</h5>
    //       {compares.value.map((diff, i) => {
    //         const { baseContent, baseFilename, headFilename, headContent } = diff
    //         return (
    //           <CodeDiff
    //             key={i}
    //             leftTitle={getTitle(baseFilename, headFilename)}
    //             splitView={splitView} //
    //             oldValue={baseContent}
    //             newValue={headContent}
    //             compareMethod={DiffMethod.LINES}
    //             disableWordDiff
    //           />
    //         )
    //       })}
    //     </>
    //   )}
    // </LoadingWrapper>
  )
}
