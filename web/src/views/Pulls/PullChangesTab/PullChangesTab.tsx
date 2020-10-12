import React from 'react'
import { Typography, useTheme } from '@material-ui/core'
import { LayersClearRounded } from '@material-ui/icons'
import EmptyContent from 'components/EmptyContent'
import CodeDiff from 'components/CodeDiff/CodeDiff'
import { AppState } from 'store'
import SplitUnifiedViewButton from 'components/CodeDiff/SplitUnifiedViewButton'
import { connect, ConnectedProps } from 'react-redux'
import LoadingWrapper from 'components/LoadingWrapper/LoadingWrapper'
import { IPullDiffsState } from 'store/pulls/types'
import { useStyles } from './styles'
import { getPullTitle } from '../Pull'

interface PullChangesTabProps extends PropsFromRedux {
  diffs: IPullDiffsState
}

const PullChangesTab = ({ diffs, settings }: PullChangesTabProps) => {
  const theme = useTheme()
  const classes = useStyles()

  return (
    <div>
      <LoadingWrapper deps={[diffs]}>
        {diffs.value && (
          <>
            {diffs.value.length > 0 ? (
              <>
                <div className={classes.titleField}>
                  <Typography className={classes.title} gutterBottom>
                    {diffs.value.length} files changed
                  </Typography>
                  <SplitUnifiedViewButton />
                </div>
                {diffs.value.map(({ baseContent, baseFilename, headFilename, headContent }, i) => (
                  <CodeDiff
                    key={i}
                    title={getPullTitle(baseFilename, headFilename)}
                    splitView={settings.splitCodeView === 'unified'}
                    oldValue={baseContent}
                    newValue={headContent}
                    disableWordDiff
                    language="java"
                    useDarkTheme={theme.palette.type === 'dark'}
                    defaultExpanded
                  />
                ))}
              </>
            ) : (
              <EmptyContent Icon={LayersClearRounded} info="No changes" />
            )}
          </>
        )}
      </LoadingWrapper>
    </div>
  )
}

const mapStateToProps = (state: AppState) => ({
  settings: state.settings.settings
})

const mapDispatchToProps = () => ({})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(PullChangesTab)
