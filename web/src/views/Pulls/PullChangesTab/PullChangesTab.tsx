import React from 'react'
import { IDiff } from 'types'
import { IconButton, Tooltip, Typography, useTheme } from '@material-ui/core'
import {
  VerticalSplitRounded,
  HorizontalSplitRounded,
  LayersClearRounded
} from '@material-ui/icons'
import EmptyContent from 'components/EmptyContent'
import CodeDiff from 'components/CodeDiff/CodeDiff'
import PullLabel from 'components/PullLabel'
import { AppDispatch, AppState } from 'store'
import { bindActionCreators } from 'redux'
import { changeSettingsAction } from 'store/settings/actions'
import { connect, ConnectedProps } from 'react-redux'
import { useStyles } from './styles'

interface PullChangesTabProps extends PropsFromRedux {
  diffs: IDiff[]
}

const PullChangesTab = ({ diffs, settings, changeSettings }: PullChangesTabProps) => {
  const theme = useTheme()
  const classes = useStyles()

  if (!diffs) {
    return <></>
  }

  const handleSplitView = () => {
    changeSettings({
      ...settings,
      splitCodeView: settings.splitCodeView === 'unified' ? 'split' : 'unified'
    })
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
            <Tooltip
              title={`${settings.splitCodeView === 'unified' ? 'Unified' : 'Split'}`}
              placement="top"
            >
              <IconButton onClick={() => handleSplitView()}>
                {(settings.splitCodeView === 'split' && <HorizontalSplitRounded />) || (
                  <VerticalSplitRounded />
                )}
              </IconButton>
            </Tooltip>
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

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  changeSettings: bindActionCreators(changeSettingsAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(PullChangesTab)
