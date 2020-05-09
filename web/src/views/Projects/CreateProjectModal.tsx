import React, { useState } from 'react'
import { Button, ControlLabel, FormControl, FormGroup, Modal } from 'react-bootstrap'

const validateRepoUrl = (url: string) => {
  const githubRepoUrlRegex = /https:\/\/github.com\/[\w\d_-]+\/[\w\d_-]+$/
  if (url.length === 0) return null
  if (githubRepoUrlRegex.test(url)) return 'success'
  return 'error'
}

interface CreateProjectModalProps {
  show: boolean
  onClose: () => void
  onCreate: () => void
}

const CreateProjectModal = ({ show, onClose, onCreate }: CreateProjectModalProps) => {
  const [url, setUrl] = useState('')
  const validUrl = validateRepoUrl(url)
  return (
    <Modal show={show} onHide={onClose}>
      <Modal.Header closeButton onHide={onClose}>
        <Modal.Title>Add new project</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <FormGroup controlId="repoUrl" validationState={validUrl}>
          <ControlLabel>Link to GitHub repository</ControlLabel>
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
      </Modal.Body>
      <Modal.Footer>
        <Button
          bsStyle="info"
          className="btn-fill"
          disabled={validUrl !== 'success'}
          onClick={onCreate}
        >
          Create
        </Button>
      </Modal.Footer>
    </Modal>
  )
}

export default CreateProjectModal
