import React from 'react'
import clsx from 'clsx'
import { useStyles } from './styles'
import { ICodeSnippet } from "types";
import Link from "../Link";

interface CloneLabel {
  className?: string
  type?: 'added' | 'removed'
  snippet: ICodeSnippet
}

const CloneLabel = ({ className, type, snippet }: CloneLabel) => {
  const classes = useStyles()
  let labelClassName: string
  if (type === 'added') {
    labelClassName = classes.addedLabel
  } else if (type === 'removed') {
    labelClassName = classes.removedLabel
  }
  return (
    <code className={clsx(className, classes.label, labelClassName)}>
      <Link to={snippet.commitUrl}>{snippet.sha.substr(0, 7)}</Link>
      {':'}
      <Link to={snippet.pullUrl}>#{snippet.pullNumber}</Link>
      {'@'}
      <Link to={`https://github.com/${snippet.owner}/${snippet.repo}`}>
        {snippet.owner}/{snippet.repo}
      </Link>
      {':'}
      <Link to={snippet.fileUrl}>{snippet.file}</Link>
    </code>
  )
}

export default CloneLabel
