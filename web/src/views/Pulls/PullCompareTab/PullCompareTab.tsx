import React, { useEffect, useState } from 'react'
import { IDiff, IProject, IShortPull } from 'types'
import { CompareRounded } from '@material-ui/icons'
import { AppDispatch, AppState } from 'store'
import { connect, ConnectedProps } from 'react-redux'
import { bindActionCreators } from 'redux'
import {
  clearComparesAction,
  getComparesAction,
  getPullsAction,
  setCompareWithAction
} from 'store/pulls/actions'
import { useLocation } from 'react-use'
import { Avatar, TextField } from '@material-ui/core'
import EmptyContent from 'components/EmptyContent'
import LoadingWrapper from 'components/LoadingWrapper'
import { DiffMethod } from 'react-diff-viewer'
import { Autocomplete } from '@material-ui/lab'
import { historyPush } from 'utils'
import { useHistory } from 'react-router'
import CodeDiffList from 'components/CodeDiffList/CodeDiffList'
import { getPullTitle } from '../Pull'
import { useStyles } from './styles'

interface PullCompareTabProps extends PropsFromRedux {
  project: IProject
  pull: IShortPull
}

const PullCompareTab = ({
  compares,
  project,
  pull,
  pulls,
  compareWith,
  getCompares,
  getPulls,
  setCompareWith,
  clearCompares
}: PullCompareTabProps) => {
  const history = useHistory()
  const location = useLocation()
  const classes = useStyles()
  const [pullOptions, setPullOptions] = useState<IShortPull[]>([])
  const [defaultOption, setDefaultOption] = useState<IShortPull>(null)

  useEffect(() => {
    history.push(
      `/projects/${project.id}/pulls/${pull.number}/compare${
        compareWith ? `?with=${compareWith}` : ''
      }`
    )
    // eslint-disable-next-line
  }, [compareWith])

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

  const handleDeleteOption = () => {
    clearCompares()
    historyPush(history, `/projects/${project.id}/pulls/${pull.number}/compare`)
  }

  const renderCodeDiff =
    compares.value && compares.value.length > 0 ? (
      <CodeDiffList
        title={`${compares.value.length} files changed`}
        list={compares.value}
        language="java"
        getDiffTitle={(diff: IDiff) => getPullTitle(diff.baseFilename, diff.headFilename)}
        getOldValue={(diff: IDiff) => diff.baseContent}
        getNewValue={(diff: IDiff) => diff.headContent}
        compareMethod={DiffMethod.LINES}
        disableWordDiff
      />
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
          disabled={compares.isFetching}
          onChange={(_, value: IShortPull, reason) => {
            if (reason === 'clear') {
              handleDeleteOption()
            } else if (reason === 'select-option' && value) {
              handleSelectPull(value.number)
            }
          }}
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
  compares: state.pulls.compares,
  compareWith: state.pulls.compareWith,
  pulls: state.pulls.pulls.value
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getCompares: bindActionCreators(getComparesAction, dispatch),
  getPulls: bindActionCreators(getPullsAction, dispatch),
  setCompareWith: bindActionCreators(setCompareWithAction, dispatch),
  clearCompares: bindActionCreators(clearComparesAction, dispatch)
})
const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(PullCompareTab)
