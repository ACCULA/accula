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
import { useSnackbar } from 'notistack'
import { historyPush } from 'utils'
import { getNotifier } from 'App'
import { useHistory } from 'react-router'
import { useStyles } from './styles'

const minFileMinSimilarityIndex = 0
const minCloneTokenCount = 0

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
  const snackbarContext = useSnackbar()
  const [adminOptions, setAdminOptions] = useState<IUser[]>(null)
  const [excludedFilesOptions, setExcludedFilesOptions] = useState(null)
  const [fetching, setFetching] = useState(false)

  useEffect(() => {
    getProjectConf(project.id, getNotifier('error', snackbarContext))
    getRepoAdmins(project.id, getNotifier('error', snackbarContext))
    getBaseFiles(project.id, getNotifier('error', snackbarContext))
    // eslint-disable-next-line
  }, [])

  useEffect(() => {
    if (repoAdmins && projectConf) {
      const options = repoAdmins.filter(u => u.id !== project.creatorId)
      setAdminOptions(options)
    }
    // eslint-disable-next-line
  }, [repoAdmins, projectConf])

  useEffect(() => {
    if (baseFiles) {
      setExcludedFilesOptions(['All', ...baseFiles])
    }
    // eslint-disable-next-line
  }, [baseFiles])

  if (
    !project ||
    !projectConf ||
    !repoAdmins ||
    !baseFiles ||
    !excludedFilesOptions ||
    !adminOptions
  ) {
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
          admins: admins.map(a => a.id),
          excludedFiles,
          fileMinSimilarityIndex:
            fileMinSimilarityIndex === '' ? minFileMinSimilarityIndex : fileMinSimilarityIndex,
          cloneMinTokenCount: cloneMinTokenCount === '' ? minCloneTokenCount : cloneMinTokenCount
        },
        () => {
          getNotifier('success', snackbarContext)('Configuration has been successfully updated')
          setFetching(false)
        },
        msg => {
          getNotifier('error', snackbarContext)(msg)
          setFetching(false)
        }
      )
    }
  }

  const handleDeleteProject = () => {
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
    <div className={classes.root}>
      <Typography className={classes.title} gutterBottom>
        Project configuration
      </Typography>
      <Formik
        validationSchema={validationSchema}
        initialValues={{
          admins: adminOptions.filter(u => projectConf.admins.includes(u.id)),
          excludedFiles:
            excludedFilesOptions.length === projectConf.excludedFiles.length
              ? excludedFilesOptions.slice(1)
              : excludedFilesOptions.filter((f: string) => projectConf.excludedFiles.includes(f)),
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
                  renderTags={(value: IUser[], getTagProps: any) =>
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
                      <Avatar
                        className={classes.avatarOption}
                        alt={option.login}
                        src={option.avatar}
                      />
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
                  options={excludedFilesOptions}
                  getOptionLabel={(option: string) => option}
                  filterSelectedOptions
                  disableCloseOnSelect
                  defaultValue={excludedFilesOptions.filter((f: string) =>
                    projectConf.excludedFiles.includes(f)
                  )}
                  value={values.excludedFiles}
                  onChange={(_, value: string[]) => {
                    if (value.includes('All')) {
                      setFieldValue('excludedFiles', excludedFilesOptions.slice(1))
                    } else {
                      setFieldValue('excludedFiles', value)
                    }
                  }}
                  renderTags={(value: string[], getTagProps: any) =>
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
                  filterOptions={options => {
                    if (values.excludedFiles.length === excludedFilesOptions.length - 1) {
                      return []
                    }
                    return options
                  }}
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
    </div>
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
