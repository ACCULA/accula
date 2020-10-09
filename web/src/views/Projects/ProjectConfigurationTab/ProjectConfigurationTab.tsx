import React, { useEffect, useState } from 'react'
import {
  Chip,
  Avatar,
  TextField,
  Card,
  CardContent,
  Typography,
  IconButton
} from '@material-ui/core'
import { IProject, IProjectConf, IUser } from 'types'
import Autocomplete from '@material-ui/lab/Autocomplete'
import { Field, Form, Formik } from 'formik'
import * as Yup from 'yup'
import LoadingButton from 'components/LoadingButton'
import { connect, ConnectedProps } from 'react-redux'
import { bindActionCreators } from 'redux'
import { AppDispatch, AppState } from 'store'
import { getRepoAdminsAction, updateProjectConfAction } from 'store/projects/actions'
import { useSnackbar, VariantType } from 'notistack'
import { CloseRounded } from '@material-ui/icons'
import { useStyles } from './styles'

const validationSchema = Yup.object().shape({
  cloneMinLineCount: Yup.number()
    .min(5, 'Value should be more than 5')
    .max(10000, 'Value should be less than 10000')
})

interface ProjectConfigurationTabProps extends PropsFromRedux {
  project: IProject
  projectConf: IProjectConf
}

const ProjectConfigurationTab = ({
  project,
  repoAdmins,
  projectConf,
  updateProjectConf,
  getRepoAdmins
}: ProjectConfigurationTabProps) => {
  const classes = useStyles()
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
            <Card className={classes.root}>
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
    </>
  )
}
const mapStateToProps = (state: AppState) => ({
  repoAdmins: state.projects.repoAdmins.value
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getRepoAdmins: bindActionCreators(getRepoAdminsAction, dispatch),
  updateProjectConf: bindActionCreators(updateProjectConfAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(ProjectConfigurationTab)
