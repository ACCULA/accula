import React from 'react'
import Prism from 'prismjs'
import 'prismjs/themes/prism.css'
import { CodeDiff, DiffMethod } from 'components/CodeDiff'

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
  return (
    <>
      Clone detections
      <CodeDiff
        leftTitle={<pre>@lamtev: /src/main/Test.java</pre>}
        rightTitle={<code>@vaddya: /src/main/Test.java</code>}
        oldValue={oldCode}
        newValue={newCode}
        splitView={false}
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
