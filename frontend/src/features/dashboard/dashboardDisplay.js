/** 1284 -> "1,284". Dashboard counts are small; no compacting needed yet. */
export function formatCount(value) {
  return Number(value ?? 0).toLocaleString()
}
