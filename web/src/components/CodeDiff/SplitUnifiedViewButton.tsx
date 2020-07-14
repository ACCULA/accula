import React from 'react'
import { Button } from 'react-bootstrap'

interface SplitUnifiedViewButtonProps {
  splitView: boolean
  setSplitView: (boolean) => void
}

export const SplitUnifiedViewButton = ({
  splitView, //
  setSplitView
}: SplitUnifiedViewButtonProps) => {
  return (
    <Button
      bsStyle="info" //
      className="split-unified-view-button"
      onClick={() => setSplitView(!splitView)}
    >
      {splitView ? (
        <>
          <i className="fas fa-fw fa-arrows-alt-v" /> Unified view
        </>
      ) : (
        <>
          <i className="fas fa-fw fa-arrows-alt-h" /> Split view
        </>
      )}
    </Button>
  )
}
