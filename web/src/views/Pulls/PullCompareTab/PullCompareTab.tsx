import React, { useEffect, useState } from 'react'
import { IProject, IShortPull } from 'types'
import { CompareRounded } from '@material-ui/icons'
import { AppDispatch, AppState } from 'store'
import { connect, ConnectedProps } from 'react-redux'
import { bindActionCreators } from 'redux'
import { getComparesAction, getPullsAction, setCompareWithAction } from 'store/pulls/actions'
import { useLocation } from 'react-use'
import { Avatar, TextField, Typography, useTheme } from '@material-ui/core'
import SplitUnifiedViewButton from 'components/CodeDiff/SplitUnifiedViewButton'
import CodeDiff from 'components/CodeDiff'
import EmptyContent from 'components/EmptyContent'
import LoadingWrapper from 'components/LoadingWrapper'
import { DiffMethod } from 'react-diff-viewer'
import { Autocomplete } from '@material-ui/lab'
import { historyPush } from 'utils'
import { useHistory } from 'react-router'
import { getPullTitle } from '../Pull'
import { useStyles } from './styles'

interface PullCompareTabProps extends PropsFromRedux {
  project: IProject
  pull: IShortPull
}

const PullCompareTab = ({
  settings,
  compares,
  project,
  pull,
  pulls,
  compareWith,
  getCompares,
  getPulls,
  setCompareWith
}: PullCompareTabProps) => {
  const history = useHistory()
  const location = useLocation()
  const classes = useStyles()
  const theme = useTheme()
  const [pullOptions, setPullOptions] = useState<IShortPull[]>([])
  const [defaultOption, setDefaultOption] = useState<IShortPull>(null)

  useEffect(() => {
    getPulls(project.id)
    // eslint-disable-next-line
  }, [project.id])

  useEffect(() => {
    if (pulls) {
      const options = pulls
        .filter(p => p.number !== pull.number)
        .sort((a, b) => (a.number > b.number ? -1 : a.number === b.number ? 0 : 1))
      setPullOptions(options)
      setDefaultOption(options.find(p => p.number === compareWith))
    }
    // eslint-disable-next-line
  }, [pulls])

  useEffect(() => {
    const query = parseInt(new URLSearchParams(location.search).get('with'), 10)
    if (!Number.isNaN(query) && compareWith !== query) {
      setCompareWith(query)
      getCompares(project.id, pull.number, query)
    }
    // eslint-disable-next-line
  }, [location])

  if (!pulls || (compareWith && defaultOption === null)) {
    return <></>
  }

  const handleSelectPull = (pullNum: number) =>
    historyPush(history, `/projects/${project.id}/pulls/${pull.number}/compare?with=${pullNum}`)

  const renderCodeDiff =
    compares.value && compares.value.length > 0 ? (
      <>
        <div className={classes.titleField}>
          <Typography className={classes.title} gutterBottom>
            {compares.value.length} files changed
          </Typography>
          <SplitUnifiedViewButton />
        </div>
        {compares.value.map(({ baseContent, baseFilename, headFilename, headContent }, i) => (
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
            compareMethod={DiffMethod.LINES}
          />
        ))}
      </>
    ) : (
      <EmptyContent Icon={CompareRounded} info="No differences" />
    )

  return (
    <div>
      <div className={classes.compareWithField}>
        <Autocomplete
          id="compare-pull-select"
          options={pullOptions}
          getOptionLabel={(option: IShortPull) =>
            `#${option.number} ${option.title} (@${option.author.login})`
          }
          filterSelectedOptions
          defaultValue={defaultOption}
          onChange={(_, value: IShortPull) => value && handleSelectPull(value.number)}
          renderOption={(option: IShortPull) => (
            <div className={classes.option}>
              <span className={classes.optionText}>{`#${option.number} ${option.title}`}</span>
              <span className={classes.optionText}>{`@${option.author.login}`}</span>
              <Avatar
                className={classes.avatarOption}
                alt={option.author.login}
                src={option.author.avatar}
              />
            </div>
          )}
          renderInput={params => (
            <TextField
              {...params}
              variant="outlined"
              label="Compare with"
              color="secondary"
              placeholder="Pull request"
            />
          )}
        />
      </div>
      {(compareWith && <LoadingWrapper deps={[compares]}>{renderCodeDiff}</LoadingWrapper>) || (
        <>{renderCodeDiff}</>
      )}
    </div>
  )
}
const mapStateToProps = (state: AppState) => ({
  settings: state.settings.settings,
  compares: state.pulls.compares,
  compareWith: state.pulls.compareWith,
  pulls: state.pulls.pulls.value
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getCompares: bindActionCreators(getComparesAction, dispatch),
  getPulls: bindActionCreators(getPullsAction, dispatch),
  setCompareWith: bindActionCreators(setCompareWithAction, dispatch)
})
const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(PullCompareTab)
