import React, { useState } from 'react'
import { IPullClonesState } from 'store/pulls/types'

interface PullClonesTabProps {
  clones: IPullClonesState
  refreshClones: () => void
  isAdmin: boolean
}

export const PullClonesTab = ({
  clones, //
  refreshClones,
  isAdmin
}: PullClonesTabProps) => {
  const [splitView, setSplitView] = useState(true)

  return (
    <></>
    // <LoadingWrapper deps={[clones]}>
    //   <div className="pull-right">
    //     {isAdmin && (
    //       <Button
    //         // bsStyle="info" //
    //         className="pull-refresh-clone"
    //         onClick={refreshClones}
    //       >
    //         <i className="fas fa-fw fa-sync-alt" /> Refresh
    //       </Button>
    //     )}
    //     {clones.value && clones.value.length > 0 && (
    //       <SplitUnifiedViewButton splitView={splitView} setSplitView={setSplitView} />
    //     )}
    //   </div>
    //   <h5>{clones.value?.length || 0} clones found</h5>
    //   {clones.value &&
    //     clones.value.map(clone => (
    //       <CodeDiff
    //         key={clone.id}
    //         leftTitle={
    //           <>
    //             Code cloned from{' '}
    //             <span className="left-title left-title-colored">
    //               {`#${clone.source.pullNumber}@${clone.source.repo}:${clone.source.file}`}
    //             </span>{' '}
    //             into <span className="right-title right-title-colored">{clone.target.file}</span>
    //           </>
    //         }
    //         oldValue={clone.source.content}
    //         newValue={clone.target.content}
    //         splitView={splitView}
    //         showDiffOnly
    //         leftOffset={clone.source.fromLine}
    //         rightOffset={clone.target.fromLine}
    //         compareMethod={DiffMethod.WORDS_WITH_SPACE}
    //         // disableWordDiff
    //       />
    //     ))}
    // </LoadingWrapper>
  )
}
