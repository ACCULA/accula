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
  getBaseFilesAction,
  getProjectConfAction,
  getRepoAdminsAction,
  updateProjectConfAction
} from 'store/projects/actions'
import { useSnackbar, VariantType } from 'notistack'
import { CloseRounded } from '@material-ui/icons'
import { historyPush } from 'utils'
import { useHistory } from 'react-router'
import { useStyles } from './styles'

const minFileMinSimilarityIndex = 5
const minCloneTokenCount = 15

// Todo: add a limit on the maximum value of numeric variables
const validationSchema = Yup.object().shape({
  fileMinSimilarityIndex: Yup.number().min(
    minFileMinSimilarityIndex,
    `Value should be more than ${minFileMinSimilarityIndex}`
  ),
  cloneMinTokenCount: Yup.number().min(
    minCloneTokenCount,
    `Value should be more than ${minCloneTokenCount}`
  )
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
  baseFiles,
  updateProjectConf,
  getRepoAdmins,
  getProjectConf,
  getBaseFiles,
  deleteProject
}: ProjectSettingsTabProps) => {
  const classes = useStyles()
  const history = useHistory()
  const { enqueueSnackbar, closeSnackbar } = useSnackbar()
  const [adminOptions, setAdminOptions] = useState<IUser[]>([])
  const [excludedFilesOptions, setExcludedFilesOptions] = useState([])
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
    getBaseFiles(project.id, showNotification('error'))
    // eslint-disable-next-line
  }, [])

  useEffect(() => {
    if (repoAdmins && projectConf) {
      const options = repoAdmins.filter(u => u.id !== project.creatorId)
      setAdminOptions(options)
    }
    if (baseFiles) {
      setExcludedFilesOptions(baseFiles)
    }
    // eslint-disable-next-line
  }, [])

  if (!project || !projectConf || !repoAdmins || !baseFiles) {
    return <></>
  }

  const handleSubmit = ({
    admins,
    excludedFiles,
    fileMinSimilarityIndex,
    cloneMinTokenCount
  }: any) => {
    if (!fetching) {
      setFetching(true)
      updateProjectConf(
        project.id,
        {
          admins,
          excludedFiles,
          fileMinSimilarityIndex:
            fileMinSimilarityIndex === '' ? minFileMinSimilarityIndex : fileMinSimilarityIndex,
          cloneMinTokenCount: cloneMinTokenCount === '' ? minCloneTokenCount : cloneMinTokenCount
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
          admins: adminOptions.filter(u => projectConf.admins.includes(u.id)),
          excludedFiles: excludedFilesOptions.filter(f =>
            projectConf.excludedFiles.includes(f.value)
          ),
          fileMinSimilarityIndex: projectConf ? projectConf.fileMinSimilarityIndex : '',
          cloneMinTokenCount: projectConf ? projectConf.cloneMinTokenCount : ''
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
                  defaultValue={adminOptions.filter(u => projectConf.admins.includes(u.id))}
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
                  become a project admin
                </Typography>
                <Field
                  name="excludedFiles"
                  component={Autocomplete}
                  multiple
                  limitTags={5}
                  id="excluded-files-select"
                  options={adminOptions}
                  getOptionLabel={(option: string) => option}
                  filterSelectedOptions
                  defaultValue={excludedFilesOptions.filter(f =>
                    projectConf.excludedFiles.includes(f.value)
                  )}
                  onChange={(_, value: string[]) => setFieldValue('excludedFiles', value)}
                  renderTags={(value: string[], getTagProps) =>
                    value.map((option, index) => (
                      <Chip
                        className={classes.chip}
                        key={option}
                        label={option}
                        color="secondary"
                        {...getTagProps({ index })}
                      />
                    ))
                  }
                  renderInput={params => (
                    <TextField
                      {...params}
                      variant="outlined"
                      label="Excluded files"
                      color="secondary"
                      placeholder="Files"
                    />
                  )}
                />
                <Typography className={classes.description} variant="body2" component="p">
                  Files that will be excluded during code clone analysis
                </Typography>
                <Field
                  error={errors.cloneMinTokenCount !== undefined}
                  helperText={errors.cloneMinTokenCount}
                  name="cloneMinTokenCount"
                  id="clone-min-token-count"
                  label="Clone minimum token count"
                  placeholder={`Default value is ${minCloneTokenCount}`}
                  type="number"
                  fullWidth
                  variant="outlined"
                  color="secondary"
                  as={TextField}
                />
                <Typography className={classes.description} variant="body2" component="p">
                  Minimum source code token count to be considered as a clone
                </Typography>
                <Field
                  error={errors.fileMinSimilarityIndex !== undefined}
                  helperText={errors.fileMinSimilarityIndex}
                  name="fileMinSimilarityIndex"
                  id="file-min-sim-index"
                  label="File minimum similarity index"
                  placeholder={`Default value is ${minFileMinSimilarityIndex}`}
                  type="number"
                  fullWidth
                  variant="outlined"
                  color="secondary"
                  as={TextField}
                />
                <Typography className={classes.description} variant="body2" component="p">
                  Minimum similarity percent to consider file as renamed
                </Typography>
                <div className={classes.saveButtonContainer}>
                  <LoadingButton
                    text="Save"
                    submitting={fetching}
                    disabled={errors.fileMinSimilarityIndex !== undefined}
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
  projectConf: state.projects.projectConf.value,
  baseFiles: state.projects.baseFiles.value
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getRepoAdmins: bindActionCreators(getRepoAdminsAction, dispatch),
  updateProjectConf: bindActionCreators(updateProjectConfAction, dispatch),
  getProjectConf: bindActionCreators(getProjectConfAction, dispatch),
  getBaseFiles: bindActionCreators(getBaseFilesAction, dispatch),
  deleteProject: bindActionCreators(deleteProjectAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(ProjectSettingsTab)
