import { formatNumber } from '@/lib/format'

/** 1284 -> "1,284" / "1 284" in the UI language. Dashboard counts are small; no compacting needed yet. */
export function formatCount(value) {
  return formatNumber(Number(value ?? 0))
}
