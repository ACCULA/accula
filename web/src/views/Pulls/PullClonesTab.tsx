import React, { useState } from 'react'
import Prism from 'prismjs'
import 'prismjs/themes/prism.css'
import { CodeDiff, DiffMethod } from 'components/CodeDiff'
import { Button } from 'react-bootstrap'

const oldCode = `
@Modifying
@Query("UPDATE refresh_token " +
       "SET token = :newToken, expiration_date = :newExpirationDate " +
       "WHERE user_id = :userId AND token = :oldToken")
Mono<Void> replaceRefreshToken(final Long userId,
                               final String oldToken,
                               final String newToken,
                               final Instant newExpirationDate);
`

const newCode = `
@Modifying
@Query("UPDATE refresh_token " +
       "SET token = :newToken, expiration_date = :newExpirationDate " +
       "WHERE token = :oldToken AND user_id = :userId")
Mono<Void> replaceRefreshToken(final Long userId,
                               final String oldToken,
                               final String newToken,
                               final Instant newExpirationDate);
`

export const PullClonesTab = () => {
  const [splitView, setSplitView] = useState(false)
  return (
    <>
      <div className="pull-right">
        <Button bsStyle="info" onClick={() => setSplitView(!splitView)} style={{ marginTop: -7 }}>
          {splitView ? 'Unified view' : 'Split view'}
        </Button>
      </div>
      <h5>1 clone found</h5>
      <CodeDiff
        leftTitle={
          <>
            Code cloned from{' '}
            <span className="left-title left-title-colored">highload-2017/#11/src/main/Test.java</span> into{' '}
            <span className="right-title right-title-colored">src/main/Test.java</span>
          </>
        }
        oldValue={oldCode}
        newValue={newCode}
        splitView={splitView}
        showDiffOnly
        leftOffset={10}
        rightOffset={20}
        compareMethod={DiffMethod.WORDS}
        // disableWordDiff
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
    </>
  )
}
