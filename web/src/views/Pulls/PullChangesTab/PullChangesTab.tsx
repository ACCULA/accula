import React from 'react'
import { IDiff } from 'types'
import { Typography, useTheme } from '@material-ui/core'
import { LayersClearRounded } from '@material-ui/icons'
import EmptyContent from 'components/EmptyContent'
import CodeDiff from 'components/CodeDiff/CodeDiff'
import PullLabel from 'components/PullLabel'
import { AppState } from 'store'
import SplitUnifiedViewButton from 'components/CodeDiff/SplitUnifiedViewButton'
import { connect, ConnectedProps } from 'react-redux'
import { useStyles } from './styles'

interface PullChangesTabProps extends PropsFromRedux {
  diffs: IDiff[]
}

const PullChangesTab = ({ diffs, settings }: PullChangesTabProps) => {
  const theme = useTheme()
  const classes = useStyles()

  if (!diffs) {
    return <></>
  }

  const getTitle = (base?: string, head?: string): JSX.Element => {
    if (base && head) {
      if (base === head) {
        return <PullLabel text={base} />
      }
      return <PullLabel className={classes.removedTitle} text={`${base} -> ${head}`} />
    }
    if (base) {
      return <PullLabel className={classes.removedTitle} text={base} />
    }
    if (head) {
      return <PullLabel className={classes.addedTitle} text={head} />
    }
    return <code />
  }

  return (
    <div>
      {diffs.length > 0 ? (
        <>
          <div className={classes.titleField}>
            <Typography className={classes.title} gutterBottom>
              {diffs.length} files changed
            </Typography>
            <SplitUnifiedViewButton />
          </div>
          {diffs.map(({ baseContent, baseFilename, headFilename, headContent }, i) => (
            <CodeDiff
              key={i}
              title={getTitle(baseFilename, headFilename)}
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
