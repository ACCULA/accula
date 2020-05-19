import React from 'react'
import { ReactDiffViewerStylesOverride } from 'components/CodeDiff/styles'

export enum LineNumberPrefix {
  LEFT = 'L',
  RIGHT = 'R'
}

export interface ReactDiffViewerProps {
  oldValue: string
  newValue: string
  splitView?: boolean
  disableWordDiff?: boolean
  compareMethod?: DiffMethod
  extraLinesSurroundingDiff?: number
  hideLineNumbers?: boolean
  showDiffOnly?: boolean
  renderContent?: (source: string) => JSX.Element
  codeFoldMessageRenderer?: (
    totalFoldedLines: number,
    leftStartLineNumber: number,
    rightStartLineNumber: number
  ) => JSX.Element
  onLineNumberClick?: (lineId: string, event: React.MouseEvent<HTMLTableCellElement>) => void
  highlightLines?: string[]
  styles?: ReactDiffViewerStylesOverride
  useDarkTheme?: boolean
  leftTitle?: string | JSX.Element
  rightTitle?: string | JSX.Element
  leftOffset?: number
  rightOffset?: number
}

export interface ReactDiffViewerState {
  // Array holding the expanded code folding.
  expandedBlocks?: number[]
  isShow?: boolean
}

export enum DiffType {
  DEFAULT = 0,
  ADDED = 1,
  REMOVED = 2
}

// See https://github.com/kpdecker/jsdiff/tree/v4.0.1#api for more info on the below JsDiff methods
export enum DiffMethod {
  CHARS = 'diffChars',
  WORDS = 'diffWords',
  WORDS_WITH_SPACE = 'diffWordsWithSpace',
  LINES = 'diffLines',
  TRIMMED_LINES = 'diffTrimmedLines',
  SENTENCES = 'diffSentences',
  CSS = 'diffCss'
}

export interface DiffInformation {
  value?: string | DiffInformation[]
  lineNumber?: number
  type?: DiffType
}

export interface LineInformation {
  left?: DiffInformation
  right?: DiffInformation
}

export interface ComputedLineInformation {
  lineInformation: LineInformation[]
  diffLines: number[]
}

export interface ComputedDiffInformation {
  left?: DiffInformation[]
  right?: DiffInformation[]
}

// See https://github.com/kpdecker/jsdiff/tree/v4.0.1#change-objects for more info on JsDiff
// Change Objects
export interface JsDiffChangeObject {
  added?: boolean
  removed?: boolean
  value?: string
}
