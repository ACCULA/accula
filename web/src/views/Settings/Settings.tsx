import React, { useEffect, useState } from 'react'
import { bindActionCreators } from 'redux'
import { connect, ConnectedProps } from 'react-redux'
import { useSnackbar } from 'notistack'
import Autocomplete from '@material-ui/lab/Autocomplete'
import { Avatar, Card, CardContent, Chip, TextField, Typography } from '@material-ui/core'
import { Field, Form, Formik } from 'formik'
import { IUser } from 'types'
import { AppDispatch, AppState } from '../../store'
import { getNotifier } from '../../App'
import { useStyles } from './styles'
import { getAppSettingsAction, updateAppSettingsAction } from '../../store/settings/actions'
import LoadingButton from '../../components/LoadingButton'

type SettingsProps = PropsFromRedux

const Settings = ({ appSettings, getAppSettings, updateAppSettings }: SettingsProps) => {
  const snackbarContext = useSnackbar()
  const classes = useStyles()

  const [adminOptions, setAdminOptions] = useState<IUser[]>(null)
  const [fetching, setFetching] = useState(false)

  useEffect(() => {
    getAppSettings(getNotifier('error', snackbarContext))
    // eslint-disable-next-line
  }, [])

  useEffect(() => {
    if (appSettings) {
      setAdminOptions(appSettings.users)
    }
    // eslint-disable-next-line
  }, [appSettings])

  if (!appSettings || !adminOptions) {
    return <></>
  }

  const handleSubmit = ({ admins }: any) => {
    if (!fetching) {
      setFetching(true)
      updateAppSettings(
        {
          adminIds: admins.map(a => a.id)
        },
        () => {
          getNotifier('success', snackbarContext)('App settings has been successfully updated')
          setFetching(false)
        },
        msg => {
          getNotifier('error', snackbarContext)(msg)
          setFetching(false)
        }
      )
    }
  }

  return (
    <div className={classes.root}>
      <Typography className={classes.title} gutterBottom>
        App settings
      </Typography>
      <Formik
        initialValues={{
          admins: adminOptions.filter(u => appSettings.adminIds.includes(u.id))
        }}
        onSubmit={handleSubmit}
      >
        {({ values, setFieldValue }) => (
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
                  filterSelectedOptions
                  defaultValue={adminOptions.filter(u => appSettings.adminIds.includes(u.id))}
                  filterOptions={options => options}
                  getOptionSelected={(option, value) => option.id === value.id}
                  value={values.admins}
                  disableCloseOnSelect
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
                      label="App admins"
                      color="secondary"
                      placeholder="App admins"
                    />
                  )}
                />
                <Typography className={classes.description} variant="body2" component="p">
                  App admin can create new projects
                </Typography>
                <div className={classes.saveButtonContainer}>
                  <LoadingButton
                    text="Save"
                    submitting={fetching}
                    onClick={() => handleSubmit(values)}
                  />
                </div>
              </CardContent>
            </Card>
          </Form>
        )}
      </Formik>
    </div>
  )
}

const mapStateToProps = (state: AppState) => ({
  appSettings: state.settings.appSettings.value
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getAppSettings: bindActionCreators(getAppSettingsAction, dispatch),
  updateAppSettings: bindActionCreators(updateAppSettingsAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(Settings)
