import React, { useEffect } from 'react'
import EmptyContent from 'components/EmptyContent'
import { useStyles } from './styles'
import ProjectCloneStatisticsTable from "../../../components/ProjectCloneStatisticsTable/ProjectCloneStatisticsTable";
import { Star } from "@material-ui/icons";
import { AppDispatch, AppState } from "../../../store";
import { bindActionCreators } from "redux";
import { getTopCloneSourcesAction } from "../../../store/projects/actions";
import { connect, ConnectedProps } from "react-redux";
import { getNotifier } from "../../../App";
import { IProject } from "../../../types";
import { useSnackbar } from "notistack";

interface ProjectTopCloneSourcesTabProps extends PropsFromRedux {
  project: IProject
}

const ProjectTopCloneSourcesTab = ({
 project,
 topCloneSources,
 getTopCloneSources,
}: ProjectTopCloneSourcesTabProps) => {
  const classes = useStyles()
  const snackbarContext = useSnackbar()

  useEffect(() => {
    if (!Number.isNaN(project.id) && !topCloneSources) {
      getTopCloneSources(project.id, getNotifier('error', snackbarContext))
    }
    // eslint-disable-next-line
  }, [project])

  if (!topCloneSources) {
    return <></>
  }

  if (topCloneSources.length === 0) {
    return <EmptyContent className={classes.emptyContent} Icon={Star} info="No clones detected yet" />
  }

  return <ProjectCloneStatisticsTable cloneStatisticsItems={topCloneSources} />
}

const mapStateToProps = (state: AppState) => ({
  topCloneSources: state.projects.topCloneSources.value,
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getTopCloneSources: bindActionCreators(getTopCloneSourcesAction, dispatch),
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(ProjectTopCloneSourcesTab)
