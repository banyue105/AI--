import type { SkillNode } from '../types'

export interface NodePosition {
  x: number
  y: number
}

const NODE_WIDTH = 144
const NODE_HEIGHT = 54
const HORIZONTAL_GAP = 24
const VERTICAL_GAP = 32
const COLUMNS = [48, 248, 450, 650, 850]
const BASE_ROWS = [332, 72, 202]
const EXTRA_ROW_START = 462
const ROW_STEP = 130

function overlaps(a: NodePosition, b: NodePosition) {
  return (
    Math.abs(a.x - b.x) < NODE_WIDTH + HORIZONTAL_GAP &&
    Math.abs(a.y - b.y) < NODE_HEIGHT + VERTICAL_GAP
  )
}

export function findFreeNodePosition(nodes: Array<Pick<SkillNode, 'x' | 'y'>>): NodePosition {
  const rows = [...BASE_ROWS]
  const requiredExtraRows = Math.ceil((nodes.length + 1) / COLUMNS.length) + 1
  for (let index = 0; index < requiredExtraRows; index += 1) {
    rows.push(EXTRA_ROW_START + index * ROW_STEP)
  }

  for (const y of rows) {
    for (const x of COLUMNS) {
      const candidate = { x, y }
      if (!nodes.some((node) => overlaps(candidate, node))) return candidate
    }
  }

  return { x: COLUMNS[0], y: EXTRA_ROW_START + requiredExtraRows * ROW_STEP }
}

export function resolveNodeOverlaps(nodes: SkillNode[]): SkillNode[] {
  const placed: SkillNode[] = []

  for (const node of nodes) {
    const hasValidPosition = Number.isFinite(node.x) && Number.isFinite(node.y)
    const collides = hasValidPosition && placed.some((item) => overlaps(node, item))
    const position = !hasValidPosition || collides ? findFreeNodePosition(placed) : { x: node.x, y: node.y }
    placed.push({ ...node, ...position })
  }

  return placed
}
