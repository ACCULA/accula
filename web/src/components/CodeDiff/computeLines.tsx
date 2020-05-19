import * as diff from 'diff'

import {
  ComputedDiffInformation,
  ComputedLineInformation,
  DiffInformation,
  DiffMethod,
  DiffType,
  JsDiffChangeObject,
  LineInformation
} from './types'

const jsDiff: { [key: string]: any } = diff

/**
 * Splits diff text by new line and computes final list of diff lines based on
 * conditions.
 *
 * @param value Diff text from the js diff module.
 */
const constructLines = (value: string): string[] => {
  const lines = value.split('\n')
  const isAllEmpty = lines.every((val): boolean => !val)
  if (isAllEmpty) {
    // This is to avoid added an extra new line in the UI.
    if (lines.length === 2) {
      return []
    }
    lines.pop()
    return lines
  }

  const lastLine = lines[lines.length - 1]
  const firstLine = lines[0]
  // Remove the first and last element if they are new line character. This is
  // to avoid addition of extra new line in the UI.
  if (!lastLine) {
    lines.pop()
  }
  if (!firstLine) {
    lines.shift()
  }
  return lines
}

const computeDiff = (
  oldValue: string,
  newValue: string,
  compareMethod: string = DiffMethod.CHARS
): ComputedDiffInformation => {
  const diffArray: JsDiffChangeObject[] = jsDiff[compareMethod](oldValue, newValue)
  const computedDiff: ComputedDiffInformation = {
    left: [],
    right: []
  }
  diffArray.forEach(
    ({ added, removed, value }): DiffInformation => {
      const diffInformation: DiffInformation = {}
      if (added) {
        diffInformation.type = DiffType.ADDED
        diffInformation.value = value
        computedDiff.right.push(diffInformation)
      }
      if (removed) {
        diffInformation.type = DiffType.REMOVED
        diffInformation.value = value
        computedDiff.left.push(diffInformation)
      }
      if (!removed && !added) {
        diffInformation.type = DiffType.DEFAULT
        diffInformation.value = value
        computedDiff.right.push(diffInformation)
        computedDiff.left.push(diffInformation)
      }
      return diffInformation
    }
  )
  return computedDiff
}

export const computeLineInformation = (
  oldString: string,
  newString: string,
  disableWordDiff: boolean = false,
  compareMethod: string = DiffMethod.CHARS,
  leftOffset: number = 0,
  rightOffset: number = 0
): ComputedLineInformation => {
  const diffArray = diff.diffLines(oldString.trimRight(), newString.trimRight(), {
    newlineIsToken: true,
    ignoreWhitespace: false,
    ignoreCase: false
  })
  let rightLineNumber = leftOffset
  let leftLineNumber = rightOffset
  let lineInformation: LineInformation[] = []
  let counter = 0
  const diffLines: number[] = []
  const ignoreDiffIndexes: string[] = []
  const getLineInformation = (
    value: string,
    diffIndex: number,
    added?: boolean,
    removed?: boolean,
    evaluateOnlyFirstLine?: boolean
  ): LineInformation[] => {
    const lines = constructLines(value)

    return lines
      .map(
        (line: string, lineIndex): LineInformation => {
          const left: DiffInformation = {}
          const right: DiffInformation = {}
          if (
            ignoreDiffIndexes.includes(`${diffIndex}-${lineIndex}`) ||
            (evaluateOnlyFirstLine && lineIndex !== 0)
          ) {
            return undefined
          }
          if (added || removed) {
            if (!diffLines.includes(counter)) {
              diffLines.push(counter)
            }
            if (removed) {
              leftLineNumber += 1
              left.lineNumber = leftLineNumber
              left.type = DiffType.REMOVED
              left.value = line || ' '
              // When the current line is of type REMOVED, check the next item in
              // the diff array whether it is of type ADDED. If true, the current
              // diff will be marked as both REMOVED and ADDED. Meaning, the
              // current line is a modification.
              const nextDiff = diffArray[diffIndex + 1]
              if (nextDiff && nextDiff.added) {
                const nextDiffLines = constructLines(nextDiff.value)[lineIndex]
                if (nextDiffLines) {
                  const { value: rightValue, lineNumber, type } = getLineInformation(
                    nextDiff.value,
                    diffIndex,
                    true,
                    false,
                    true
                  )[0].right
                  // When identified as modification, push the next diff to ignore
                  // list as the next value will be added in this line computation as
                  // right and left values.
                  ignoreDiffIndexes.push(`${diffIndex + 1}-${lineIndex}`)
                  right.lineNumber = lineNumber
                  right.type = type
                  // Do word level diff and assign the corresponding values to the
                  // left and right diff information object.
                  if (disableWordDiff) {
                    right.value = rightValue
                  } else {
                    const computedDiff = computeDiff(line, rightValue as string, compareMethod)
                    right.value = computedDiff.right
                    left.value = computedDiff.left
                  }
                }
              }
            } else {
              rightLineNumber += 1
              right.lineNumber = rightLineNumber
              right.type = DiffType.ADDED
              right.value = line
            }
          } else {
            leftLineNumber += 1
            rightLineNumber += 1

            left.lineNumber = leftLineNumber
            left.type = DiffType.DEFAULT
            left.value = line
            right.lineNumber = rightLineNumber
            right.type = DiffType.DEFAULT
            right.value = line
          }

          counter += 1
          return { right, left }
        }
      )
      .filter(Boolean)
  }

  diffArray.forEach(({ added, removed, value }: diff.Change, index): void => {
    lineInformation = [...lineInformation, ...getLineInformation(value, index, added, removed)]
  })

  return {
    lineInformation,
    diffLines
  }
}
