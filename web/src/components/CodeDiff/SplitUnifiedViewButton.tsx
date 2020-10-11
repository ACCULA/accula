import React from 'react'
import { IconButton, Tooltip } from '@material-ui/core'
import { VerticalSplitRounded, HorizontalSplitRounded } from '@material-ui/icons'
import { AppDispatch, AppState } from 'store'
import { bindActionCreators } from 'redux'
import { changeSettingsAction } from 'store/settings/actions'
import { connect, ConnectedProps } from 'react-redux'

interface SplitUnifiedViewButton extends PropsFromRedux {}

const PullChangesTab = ({ settings, changeSettings }: SplitUnifiedViewButton) => {
  const handleSplitView = () => {
    changeSettings({
      ...settings,
      splitCodeView: settings.splitCodeView === 'unified' ? 'split' : 'unified'
    })
  }

  return (
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
