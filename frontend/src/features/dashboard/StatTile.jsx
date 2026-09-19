import { Link } from 'react-router-dom'

/**
 * One figure on a dashboard: a sentence-case label, the number itself, and
 * an optional line saying what it means.
 *
 * `tone` is for a figure that is a state rather than a quantity - overdue
 * invoices, sessions still to mark. It never carries the meaning on its own:
 * the label and the note say it in words, and the colour only makes it
 * findable. Everything else stays in text tokens, so a tile grid reads as
 * one surface rather than a set of competing signals.
 */
export function StatTile({ label, value, note, to, tone = 'default' }) {
  const body = (
    <>
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className={`text-2xl font-semibold tabular-nums ${tone === 'attention' ? 'text-destructive' : ''}`}>
        {value}
      </p>
      {note && <p className="text-xs text-muted-foreground">{note}</p>}
    </>
  )

  if (!to) {
    return <div className="rounded-lg border p-4">{body}</div>
  }

  return (
    <Link
      to={to}
      className="rounded-lg border p-4 transition-colors hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
    >
      {body}
    </Link>
  )
}
