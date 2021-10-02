import React, { useEffect, useState } from 'react'
import clsx from 'clsx'
import {
  Chip,
  Avatar,
  TextField,
  Card,
  CardContent,
  Typography,
  IconButton,
  TableRow,
  TableCell
} from '@material-ui/core'
import Table from 'components/Table'

import DeleteRoundedIcon from '@material-ui/icons/DeleteRounded'
import { IGithubUser, IProject, IShortProject, IUser } from 'types'
import Autocomplete from '@material-ui/lab/Autocomplete'
import { Field, Form, Formik } from 'formik'
import * as Yup from 'yup'
import LoadingButton from 'components/LoadingButton'
import { connect, ConnectedProps } from 'react-redux'
import { bindActionCreators } from 'redux'
import { AppDispatch, AppState } from 'store'
import { getProjectConfAction, updateProjectConfAction } from 'store/projects/actions'
import Button from '@material-ui/core/Button'
import { useSnackbar } from 'notistack'
import { getNotifier } from 'App'
import { AddBoxOutlined } from '@material-ui/icons'
import { useStyles } from './styles'
import DeleteProjectDialog from '../DeleteProjectDialog'
import AddRepoToProjectDialog from '../AddRepoToProjectDialog'
import BreadCrumbs from '../../../components/BreadCrumbs'

const minLanguageCount = 1
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
  projectConf,
  updateProjectConf,
  getProjectConf
}: ProjectSettingsTabProps) => {
  const classes = useStyles()
  const snackbarContext = useSnackbar()
  const [adminOptions, setAdminOptions] = useState<IUser[]>(null)
  const [excludedFilesOptions, setExcludedFilesOptions] = useState(null)
  const [excludedSourceAuthorsOptions, setExcludedSourceAuthorsOptions] = useState(null)
  const [languagesOptions, setLanguagesOptions] = useState<string[]>(null)
  const [fetching, setFetching] = useState(false)
  const [isDeleteProjectDialogOpen, setDeleteProjectDialogOpen] = useState(false)
  const [isAddRepoToProjectDialogOpen, setAddRepoToProjectDialogOpen] = useState(false)

  useEffect(() => {
    getProjectConf(project.id, getNotifier('error', snackbarContext))
    // eslint-disable-next-line
  }, [])

  useEffect(() => {
    if (projectConf) {
      const options = projectConf.admins.suggestion.filter(u => u.id !== project.creatorId)
      setAdminOptions(options)

      const headFiles = projectConf.clones.excludedFiles.suggestion
      setExcludedFilesOptions(['All', ...headFiles])

      const allPullAuthors = projectConf.clones.excludedSourceAuthors.suggestion
      setExcludedSourceAuthorsOptions(allPullAuthors)

      const supportedLanguages = projectConf.code.languages.suggestion
      setLanguagesOptions(supportedLanguages)
    }
    // eslint-disable-next-line
  }, [projectConf])

  if (
    !project ||
    !projectConf ||
    !excludedFilesOptions ||
    !excludedSourceAuthorsOptions ||
    !languagesOptions ||
    !adminOptions
  ) {
    return <></>
  }

  const handleSubmit = ({
    admins,
    languages,
    excludedFiles,
    excludedSourceAuthors,
    fileMinSimilarityIndex,
    cloneMinTokenCount
  }: any) => {
    if (!fetching) {
      setFetching(true)
      updateProjectConf(
        project.id,
        {
          admins: {
            value: admins.map(a => a.id)
          },
          code: {
            fileMinSimilarityIndex:
              fileMinSimilarityIndex === '' ? minFileMinSimilarityIndex : fileMinSimilarityIndex,
            languages: {
              value: languages
            }
          },
          clones: {
            minTokenCount: cloneMinTokenCount === '' ? minCloneTokenCount : cloneMinTokenCount,
            excludedFiles: {
              value: excludedFiles
            },
            excludedSourceAuthors: {
              value: excludedSourceAuthors ? excludedSourceAuthors.map(u => u.id) : []
            }
          }
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

  return (
    <div className={classes.root}>
      <Typography className={classes.title} gutterBottom>
        Project configuration
      </Typography>
      <Formik
        validationSchema={validationSchema}
        initialValues={{
          admins: adminOptions.filter(u => projectConf.admins.value.includes(u.id)),
          languages: projectConf.code.languages.value,
          excludedFiles:
            excludedFilesOptions.length === projectConf.clones.excludedFiles.value.length
              ? excludedFilesOptions.slice(1)
              : excludedFilesOptions.filter((f: string) =>
                  projectConf.clones.excludedFiles.value.includes(f)
                ),
          excludedSourceAuthors: excludedSourceAuthorsOptions.filter(u =>
            projectConf.clones.excludedSourceAuthors.value.includes(u.id)
          ),
          fileMinSimilarityIndex: projectConf ? projectConf.code.fileMinSimilarityIndex : '',
          cloneMinTokenCount: projectConf ? projectConf.clones.minTokenCount : ''
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
                  defaultValue={adminOptions.filter(u => projectConf.admins.value.includes(u.id))}
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
                  name="languages"
                  component={Autocomplete}
                  multiple
                  limitTags={5}
                  id="languages-select"
                  options={languagesOptions}
                  getOptionLabel={(option: string) => option}
                  filterSelectedOptions
                  disableCloseOnSelect
                  disableClearable
                  defaultValue={languagesOptions.filter(l =>
                    projectConf.code.languages.value.includes(l)
                  )}
                  value={values.languages}
                  onChange={(_, value: string[]) => setFieldValue('languages', value)}
                  renderTags={(value: string[], getTagProps: any) =>
                    value.map((option, index) => (
                      <Chip
                        className={classes.chip}
                        key={option}
                        label={option}
                        color="secondary"
                        {...getTagProps({ index })}
                        disabled={values.languages.length === minLanguageCount}
                      />
                    ))
                  }
                  renderInput={params => (
                    <TextField
                      {...params}
                      variant="outlined"
                      label="Languages"
                      color="secondary"
                      placeholder="Languages"
                    />
                  )}
                />
                <Typography className={classes.description} variant="body2" component="p">
                  Files written in the specified languages will be taken into account during clone
                  detection and diff computing
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
                    projectConf.clones.excludedFiles.value.includes(f)
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
                  name="excludedSourceAuthors"
                  component={Autocomplete}
                  multiple
                  limitTags={5}
                  id="excluded-source-authors-select"
                  options={excludedSourceAuthorsOptions}
                  getOptionLabel={(option: IGithubUser) => option.login}
                  filterSelectedOptions
                  defaultValue={excludedSourceAuthorsOptions.filter(u =>
                    projectConf.clones.excludedSourceAuthors.value.includes(u.id)
                  )}
                  onChange={(_, value: IGithubUser[]) =>
                    setFieldValue('excludedSourceAuthors', value)
                  }
                  renderTags={(value: IGithubUser[], getTagProps: any) =>
                    value.map((option, index) => (
                      <Chip
                        className={classes.chip}
                        key={option.login}
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
                      label="Excluded source authors"
                      color="secondary"
                      placeholder="Excluded source authors"
                    />
                  )}
                />
                <Typography className={classes.description} variant="body2" component="p">
                  Authors that will not be considered as clone sources
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
                {project.secondaryRepos.length === 0 ? (
                  <>
                    <Button
                      className={classes.addRepoToProjectBtn}
                      variant="contained"
                      color="secondary"
                      onClick={() => setAddRepoToProjectDialogOpen(true)}
                    >
                      Add repo to project
                    </Button>
                    <Typography className={classes.description} variant="body2" component="p">
                      No previous years repos
                    </Typography>
                  </>
                ) : (
                  <div>
                    <BreadCrumbs breadcrumbs={[{ text: 'Previous years repos' }]} />
                    <Table<IShortProject>
                      headCells={[]}
                      count={project.secondaryRepos.length}
                      toolBarTitle=""
                      toolBarButtons={[
                        {
                          toolTip: 'Add repo to project',
                          iconButton: <AddBoxOutlined />,
                          onClick: () => setAddRepoToProjectDialogOpen(true)
                        }
                      ]}
                    >
                      {() => (
                        <>
                          {project.secondaryRepos.map(repo => (
                            <TableRow key={repo.id}>
                              <TableCell align="left">
                                <div className={classes.repoInfo}>
                                  <div className={classes.repoFullName}>
                                    <span
                                      className={classes.cellText}
                                    >{`${repo.owner}/${repo.name}`}</span>
                                  </div>
                                </div>
                              </TableCell>
                            </TableRow>
                          ))}
                        </>
                      )}
                    </Table>
                  </div>
                )}
                <AddRepoToProjectDialog
                  open={isAddRepoToProjectDialogOpen}
                  onClose={() => setAddRepoToProjectDialogOpen(false)}
                />
                <div className={classes.saveButtonContainer}>
                  <LoadingButton
                    text="Save"
                    submitting={fetching}
                    disabled={
                      errors.fileMinSimilarityIndex !== undefined ||
                      values.languages.length < minLanguageCount
                    }
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
              <IconButton
                className={classes.dangerButton}
                onClick={() => setDeleteProjectDialogOpen(true)}
              >
                <DeleteRoundedIcon />
              </IconButton>
            </CardContent>
          </Card>
          <DeleteProjectDialog
            project={project}
            open={isDeleteProjectDialogOpen}
            onClose={() => setDeleteProjectDialogOpen(false)}
          />
        </>
      )}
    </div>
  )
}

const mapStateToProps = (state: AppState) => ({
  projectConf: state.projects.projectConf.value
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  updateProjectConf: bindActionCreators(updateProjectConfAction, dispatch),
  getProjectConf: bindActionCreators(getProjectConfAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(ProjectSettingsTab)
