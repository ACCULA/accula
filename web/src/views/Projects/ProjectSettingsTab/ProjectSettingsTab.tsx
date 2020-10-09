import React, { useEffect, useState } from 'react'
import clsx from 'clsx'
import {
  Chip,
  Avatar,
  TextField,
  Card,
  CardContent,
  Typography,
  IconButton
} from '@material-ui/core'
import DeleteRoundedIcon from '@material-ui/icons/DeleteRounded'
import { IProject, IUser } from 'types'
import Autocomplete from '@material-ui/lab/Autocomplete'
import { Field, Form, Formik } from 'formik'
import * as Yup from 'yup'
import LoadingButton from 'components/LoadingButton'
import { connect, ConnectedProps } from 'react-redux'
import { bindActionCreators } from 'redux'
import { AppDispatch, AppState } from 'store'
import {
  deleteProjectAction,
  getProjectConfAction,
  getRepoAdminsAction,
  updateProjectConfAction
} from 'store/projects/actions'
import { useSnackbar, VariantType } from 'notistack'
import { CloseRounded } from '@material-ui/icons'
import { historyPush } from 'utils'
import { useHistory } from 'react-router'
import { useStyles } from './styles'

const validationSchema = Yup.object().shape({
  cloneMinLineCount: Yup.number()
    .min(5, 'Value should be more than 5')
    .max(10000, 'Value should be less than 10000')
})

interface ProjectSettingsTabProps extends PropsFromRedux {
  project: IProject
  user: IUser
}

const ProjectSettingsTab = ({
  user,
  project,
  repoAdmins,
  projectConf,
  updateProjectConf,
  getRepoAdmins,
  getProjectConf,
  deleteProject
}: ProjectSettingsTabProps) => {
  const classes = useStyles()
  const history = useHistory()
  const { enqueueSnackbar, closeSnackbar } = useSnackbar()
  const [adminOptions, setAdminOptions] = useState<IUser[]>([])
  const [fetching, setFetching] = useState(false)

  const showNotification = (variant: VariantType) => {
    return (msg: string) =>
      enqueueSnackbar(msg, {
        variant,
        action: key => (
          <IconButton onClick={() => closeSnackbar(key)} aria-label="Close notification">
            <CloseRounded />
          </IconButton>
        )
      })
  }

  useEffect(() => {
    getProjectConf(project.id, showNotification('error'))
    getRepoAdmins(project.id, showNotification('error'))
    // eslint-disable-next-line
  }, [])

  useEffect(() => {
    if (repoAdmins && projectConf) {
      const options = repoAdmins.filter(u => u.id !== project.creatorId)
      setAdminOptions(options)
    }
    // eslint-disable-next-line
  }, [])

  if (!project || !projectConf || !repoAdmins) {
    return <></>
  }

  const handleSubmit = ({ admins, cloneMinLineCount }: any) => {
    if (!fetching) {
      setFetching(true)
      updateProjectConf(
        project.id,
        {
          admins,
          cloneMinLineCount: cloneMinLineCount === '' ? 5 : cloneMinLineCount
        },
        () => {
          showNotification('success')('Configuration has been successfully updated')
          setFetching(false)
        },
        msg => {
          showNotification('error')(msg)
          setFetching(false)
        }
      )
    }
  }

  const handleDeleteProject = () => {
    deleteProject(
      project.id,
      () => {
        showNotification('success')('Project has been successfully deleted')
        setFetching(false)
        historyPush(history, '/projects')
      },
      msg => {
        showNotification('error')(msg)
        setFetching(false)
      }
    )
  }

  return (
    <>
      <Typography className={classes.title} gutterBottom>
        Project configuration
      </Typography>
      <Formik
        validationSchema={validationSchema}
        initialValues={{
          admins: adminOptions.filter(u => projectConf.admins.indexOf(u.id) !== -1),
          cloneMinLineCount: projectConf ? projectConf.cloneMinLineCount : ''
        }}
        onSubmit={handleSubmit}
      >
        {({ values, setFieldValue, errors }) => (
          <Form>
            <Card className={classes.card}>
              <CardContent>
                <Field
                  name="admins"
                  component={Autocomplete}
                  multiple
                  limitTags={5}
                  id="admins-select"
                  options={adminOptions}
                  getOptionLabel={(option: IUser) => option.login}
                  filterSelectedOptions
                  defaultValue={adminOptions.filter(u => projectConf.admins.indexOf(u.id) !== -1)}
                  onChange={(_, value: IUser[]) => setFieldValue('admins', value)}
                  renderTags={(value: IUser[], getTagProps) =>
                    value.map((option, index) => (
                      <Chip
                        className={classes.chip}
                        key={option.id}
                        avatar={<Avatar alt={option.login} src={option.avatar} />}
                        label={`@${option.login}`}
                        color="secondary"
                        {...getTagProps({ index })}
                      />
                    ))
                  }
                  renderOption={(option: IUser) => (
                    <div className={classes.option}>
                      <Avatar alt={option.login} src={option.avatar} />
                      <span className={classes.optionText}>
                        {option.name ? `@${option.login} (${option.name})` : `@${option.login}`}
                      </span>
                    </div>
                  )}
                  renderInput={params => (
                    <TextField
                      {...params}
                      variant="outlined"
                      label="Project admins"
                      color="secondary"
                      placeholder="Admins"
                    />
                  )}
                />
                <Typography className={classes.description} variant="body2" component="p">
                  Admin can resolve clones and update project settings. Only a repository admin can
                  become a project admin.
                </Typography>
                <Field
                  error={errors.cloneMinLineCount !== undefined}
                  helperText={errors.cloneMinLineCount || ''}
                  name="cloneMinLineCount"
                  id="clone-min-line-count"
                  label="Clone minimum line count"
                  type="number"
                  fullWidth
                  variant="outlined"
                  color="secondary"
                  placeholder="Default value is 5"
                  as={TextField}
                />
                <Typography className={classes.description} variant="body2" component="p">
                  Minimum source code line count to be considered as a clone.
                </Typography>
                <div className={classes.saveButtonContainer}>
                  <LoadingButton
                    text="Save"
                    submitting={fetching}
                    disabled={errors.cloneMinLineCount !== undefined}
                    onClick={() => handleSubmit(values)}
                  />
                </div>
              </CardContent>
            </Card>
          </Form>
        )}
      </Formik>
      {user && user.id === project.creatorId && (
        <>
          <Typography className={classes.title} gutterBottom>
            Danger Zone
          </Typography>
          <Card className={clsx(classes.card, classes.dangerCard)}>
            <CardContent className={classes.cardBox}>
              <div className={classes.dangerBoxTextField}>
                <span className={classes.titleBox}>Delete this project</span>
              </div>
              <IconButton className={classes.dangerButton} onClick={() => handleDeleteProject()}>
                <DeleteRoundedIcon />
              </IconButton>
            </CardContent>
          </Card>
        </>
      )}
    </>
  )
}
const mapStateToProps = (state: AppState) => ({
  repoAdmins: state.projects.repoAdmins.value,
  projectConf: state.projects.projectConf.value
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getRepoAdmins: bindActionCreators(getRepoAdminsAction, dispatch),
  updateProjectConf: bindActionCreators(updateProjectConfAction, dispatch),
  getProjectConf: bindActionCreators(getProjectConfAction, dispatch),
  deleteProject: bindActionCreators(deleteProjectAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(ProjectSettingsTab)
