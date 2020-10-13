import React from 'react'
import ReactDiffViewer, { ReactDiffViewerProps } from 'react-diff-viewer'
import { Accordion, AccordionDetails, AccordionSummary } from '@material-ui/core'
// import ExpandMoreRoundedIcon from '@material-ui/icons/ExpandMoreRounded'
import Prism from 'prismjs'
import 'prismjs/components/prism-java'
import { codeDiffStyles, useStyles } from './styles'

interface CodeDiffProps extends ReactDiffViewerProps {
  language: string
  title: React.ReactNode
  defaultExpanded?: boolean
}

const CodeDiff = ({ title, language, defaultExpanded, ...props }: CodeDiffProps) => {
  const classes = useStyles()

  const highlight = (code: string): JSX.Element => {
    if (!language || code === undefined) {
      return <>{code}</>
    }
    if (typeof code === 'undefined') {
      return <></>
    }
    return (
      <pre
        style={{ display: 'inline' }}
        /* eslint-disable-next-line react/no-danger */
        dangerouslySetInnerHTML={{
          __html: Prism.highlight(code, Prism.languages[language], language)
        }}
      />
    )
  }

  return (
    <Accordion classes={{ expanded: classes.panel }} disabled defaultExpanded>
      <AccordionSummary
        className={classes.panelHeader}
        // expandIcon={<ExpandMoreRoundedIcon />}
        aria-controls="code-panel"
        classes={{
          content: classes.panelHeaderContent,
          expandIcon: classes.expandIcon,
          disabled: classes.disabledHeader
        }}
      >
        {title}
      </AccordionSummary>
      <AccordionDetails className={classes.panelData}>
        <ReactDiffViewer
          styles={props.styles || codeDiffStyles}
          renderContent={props.renderContent || highlight}
          {...props}
        />
      </AccordionDetails>
    </Accordion>
  )
}

export default CodeDiff
