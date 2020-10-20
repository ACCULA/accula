import React, { useEffect, useState } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Button,
  TextField
} from '@material-ui/core'
import { Formik, Field, Form } from 'formik'
import LoadingButton from 'components/LoadingButton'
import { AppDispatch } from 'store'
import { bindActionCreators } from 'redux'
import { createProjectAction } from 'store/projects/actions'
import { connect, ConnectedProps } from 'react-redux'
import { getNotifier } from 'App'
import { useSnackbar } from 'notistack'
import { useStyles } from './styles'

const githubRepoUrlRegex = /https:\/\/github.com\/[\w\d_-]+\/[\w\d_-]+\/?$/

interface CreateProjectDialogProps extends PropsFromRedux {
  open: boolean
  onClose?: () => void
}

const AddProjectDialog = ({ open, onClose, addProject }: CreateProjectDialogProps) => {
  const classes = useStyles()
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

  const handleSubmit = ({ url }: any) => {
    if (!fetching && url !== undefined) {
      setFetching(true)
      addProject(
        url,
        () => {
          setFetching(false)
          handleClose()
        },
        msg => {
          getNotifier('error', snackbarContext)(msg)
          setFetching(false)
        }
      )
    }
  }

  return (
    <Dialog fullWidth className={classes.root} open={isDialogOpen} onClose={handleClose}>
      <DialogTitle>Add new project</DialogTitle>
      <Formik
        validateOnChange={false}
        validateOnBlur={false}
        initialValues={{ url: '' }}
        onSubmit={handleSubmit}
      >
        {({ values, setFieldValue, validateForm, errors, setErrors }) => (
          <Form>
            <DialogContent className={classes.content}>
              <DialogContentText>Enter a link to a GitHub repository</DialogContentText>
              <Field
                error={errors.url !== undefined}
                type="input"
                name="url"
                size="medium"
                variant="outlined"
                color="secondary"
                placeholder="For example, https://github.com/organization/repository"
                fullWidth
                autoComplete="off"
                onChange={e => {
                  setErrors({})
                  setFieldValue('url', e.target.value)
                }}
                validate={(value: string) => {
                  if (githubRepoUrlRegex.test(value)) {
                    setFieldValue('url', value, false)
                    return undefined
                  }
                  return 'Incorrect link'
                }}
                as={TextField}
              />
            </DialogContent>

            <DialogActions>
              <Button onClick={handleClose} disabled={fetching}>
                Cancel
              </Button>
              <LoadingButton
                text="Add"
                submitting={fetching}
                onClick={() =>
                  validateForm().then(errs => {
                    if (errs.url === undefined) {
                      handleSubmit(values.url)
                    }
                  })
                }
              />
            </DialogActions>
          </Form>
        )}
      </Formik>
    </Dialog>
  )
}

const mapStateToProps = () => ({})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  addProject: bindActionCreators(createProjectAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(AddProjectDialog)
