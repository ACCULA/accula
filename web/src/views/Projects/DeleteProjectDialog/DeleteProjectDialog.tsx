import React, { useEffect, useState } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Button
} from '@material-ui/core'
import LoadingButton from 'components/LoadingButton'
import { AppDispatch } from 'store'
import { bindActionCreators } from 'redux'
import { connect, ConnectedProps } from 'react-redux'
import { getNotifier } from 'App'
import { useSnackbar } from 'notistack'
import { historyPush } from 'utils'
import { IProject } from 'types'
import { deleteProjectAction } from 'store/projects/actions'
import { useHistory } from 'react-router'

interface DeleteProjectDialogProps extends PropsFromRedux {
  project: IProject
  open: boolean
  onClose?: () => void
}

const DeleteProjectDialog = ({
  project,
  open,
  onClose,
  deleteProject
}: DeleteProjectDialogProps) => {
  const history = useHistory()
  const snackbarContext = useSnackbar()
  const [isDialogOpen, openDialog] = useState(false)
  const [fetching, setFetching] = useState(false)

  useEffect(() => {
    openDialog(open)
  }, [open])

  const handleClose = () => {
    if (!fetching) {
      openDialog(false)
      if (onClose) {
        onClose()
      }
    }
  }

  const handleDeleteProject = () => {
    setFetching(true)
    deleteProject(
      project.id,
      () => {
        getNotifier('success', snackbarContext)('Project has been successfully deleted')
        setFetching(false)
        historyPush(history, '/projects')
      },
      msg => {
        getNotifier('error', snackbarContext)(msg)
        setFetching(false)
      }
    )
  }

  return (
    <Dialog open={isDialogOpen} onClose={() => handleClose()}>
      <DialogTitle>Are you sure you want to delete the project?</DialogTitle>
      <DialogContent>
        <DialogContentText>
          This action cannot be undone. This will permanently delete this project.
        </DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={() => handleClose()} disabled={fetching}>
          Cancel
        </Button>
        <LoadingButton text="Delete" submitting={fetching} onClick={() => handleDeleteProject()} />
      </DialogActions>
    </Dialog>
  )
}

const mapStateToProps = () => ({})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  deleteProject: bindActionCreators(deleteProjectAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(DeleteProjectDialog)
