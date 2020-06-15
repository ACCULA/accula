import React, { useEffect, useState } from 'react'
import { Alert, ControlLabel, FormControl, FormGroup, Modal } from 'react-bootstrap'

import { LoadingButton } from 'components/LoadingButton'

const validateRepoUrl = (url: string) => {
  const githubRepoUrlRegex = /https:\/\/github.com\/[\w\d_-]+\/[\w\d_-]+\/?$/
  if (url.length === 0) return null
  if (githubRepoUrlRegex.test(url)) return 'success'
  return 'error'
}

const messageFromError = (error: string): string => {
  switch (error) {
    case 'NO_PERMISSION':
      return 'Only the admin of the repository can create a project for it!'
    case 'WRONG_URL':
      return 'URL to the repository is wrong!'
    case 'ALREADY_EXISTS':
      return 'Project for this repository is already exists!'
    case 'INVALID_URL':
      return 'URL to the repository is invalid!'
    default:
      return 'Unknown error has occurred'
  }
}

interface CreateProjectModalProps {
  show: boolean
  error: string
  isCreating: boolean
  resetError: () => void
  onClose: () => void
  onSubmit: (string) => void
}

export const CreateProjectModal = ({
  show, //
  error,
  isCreating,
  resetError,
  onClose,
  onSubmit
}: CreateProjectModalProps) => {
  const [url, setUrl] = useState('')
  const [lastAttempt, setLastAttempt] = useState('')
  const validUrl = validateRepoUrl(url)

  useEffect(() => {
    if (error) {
      setUrl('')
    }
  }, [error, setUrl])

  useEffect(() => {
    if (!show) {
      setUrl('')
    }
  }, [show, setUrl])

  return (
    <Modal show={show} onHide={onClose}>
      <Modal.Header closeButton onHide={onClose}>
        <Modal.Title>Add new project</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <FormGroup controlId="repoUrl" validationState={validUrl}>
          <ControlLabel>Enter a link to a GitHub repository</ControlLabel>
          <FormControl
            type="text"
            value={url}
            placeholder="For example, https://github.com/organization/repository"
            onChange={(e: React.FormEvent<FormControl>) =>
              setUrl((e.target as HTMLInputElement).value)
            }
          />
          <FormControl.Feedback />
        </FormGroup>
        {error && (
          <Alert bsStyle="danger" onDismiss={resetError}>
            <strong>Cannot create a project for {lastAttempt}:</strong>
            <br />
            {messageFromError(error)}
          </Alert>
        )}
      </Modal.Body>
      <Modal.Footer>
        <LoadingButton
          bsStyle="info"
          className="btn-fill"
          disabled={validUrl !== 'success'}
          isLoading={isCreating}
          onClick={() => {
            onSubmit(url)
            setLastAttempt(url)
          }}
        >
          Create
        </LoadingButton>
      </Modal.Footer>
    </Modal>
  )
}
