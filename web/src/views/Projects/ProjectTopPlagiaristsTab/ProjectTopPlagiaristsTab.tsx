import React, { useEffect } from 'react'
import EmptyContent from 'components/EmptyContent'
import { TrendingUp } from '@material-ui/icons'
import { bindActionCreators } from 'redux'
import { connect, ConnectedProps } from 'react-redux'
import { useSnackbar } from 'notistack'
import { useStyles } from './styles'
import ProjectCloneStatisticsTable from '../../../components/ProjectCloneStatisticsTable/ProjectCloneStatisticsTable'
import { AppDispatch, AppState } from '../../../store'
import { getTopPlagiaristsAction } from '../../../store/projects/actions'
import { getNotifier } from '../../../App'
import { IProject } from '../../../types'

interface ProjectTopPlagiaristsTabProps extends PropsFromRedux {
  project: IProject
}

const ProjectTopPlagiaristsTab = ({
  project,
  topPlagiarists,
  getTopPlagiarists
}: ProjectTopPlagiaristsTabProps) => {
  const classes = useStyles()
  const snackbarContext = useSnackbar()

  useEffect(() => {
    if (!Number.isNaN(project.id) && !topPlagiarists) {
      getTopPlagiarists(project.id, getNotifier('error', snackbarContext))
    }
    // eslint-disable-next-line
  }, [project])

  if (!topPlagiarists) {
    return <></>
  }

  if (topPlagiarists.length === 0) {
    return (
      <EmptyContent
        className={classes.emptyContent}
        Icon={TrendingUp}
        info="No clones detected yet"
      />
    )
  }

  return <ProjectCloneStatisticsTable cloneStatisticsItems={topPlagiarists} />
}

const mapStateToProps = (state: AppState) => ({
  topPlagiarists: state.projects.topPlagiarists.value
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getTopPlagiarists: bindActionCreators(getTopPlagiaristsAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(ProjectTopPlagiaristsTab)
