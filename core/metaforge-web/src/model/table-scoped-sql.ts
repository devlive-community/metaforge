/**
 * Client-side mirror of the server's table scope check (TableScopedSql) for the SQL pane of a
 * table's workbench: one SELECT, INSERT, UPDATE or DELETE whose every table reference is that
 * table. The server enforces it; this gives an immediate, translated message and lets dataset
 * creation qualify the table with its database.
 */
export type ScopeIssue = 'empty' | 'multiple' | 'statement' | 'otherTable' | 'noTable' | 'into' | 'tableFunction' | 'lateral' | 'function' | 'missingTable'

export type ScopeResult = { ok: true; qualified: string } | { ok: false; issue: ScopeIssue; detail?: string }

interface Token {
  text: string
  /** Comments and whitespace keep their text for rewriting but are skipped by the check. */
  significant: boolean
  word: boolean
  quoted: boolean
}

const TOKEN = /(--[^\n]*|#[^\n]*|\/\*[\s\S]*?\*\/)|('(?:[^'\\]|''|\\[\s\S])*'?)|("(?:[^"\\]|""|\\[\s\S])*"?|`(?:[^`]|``)*`?)|(\s+)|([A-Za-z_][A-Za-z0-9_$]*)|([\s\S])/g
const STATEMENTS = new Set(['SELECT', 'INSERT', 'UPDATE', 'DELETE'])
const CLAUSE_WORDS = new Set(`WHERE GROUP HAVING ORDER LIMIT JOIN INNER LEFT RIGHT CROSS NATURAL STRAIGHT_JOIN ON USING UNION SET VALUES VALUE
SELECT PARTITION FOR LOCK WINDOW USE IGNORE FORCE AS INTO OUTER FULL EXCEPT INTERSECT DUPLICATE RETURNING LATERAL FROM TABLE`.split(/\s+/))

function tokenize(sql: string): Token[] {
  return [...String(sql || '').matchAll(TOKEN)].map(([text, comment, string, quoted, space, word]) => ({
    text, significant: !comment && !space, word: !!word, quoted: !!quoted && !string,
  }))
}

const upper = (token?: Token) => token?.word ? token.text.toUpperCase() : ''
const is = (token: Token | undefined, value: string) => !!token && !token.quoted && token.text === value
const nameOf = (token: Token) => token.quoted ? token.text.slice(1, -1).replace(new RegExp(token.text[0] + token.text[0], 'g'), token.text[0]) : token.text
const quote = (name: string) => '`' + name.replace(/`/g, '``') + '`'

export function checkTableScope(sql: string, database: string, table: string): ScopeResult {
  const all = tokenize(sql)
  const positions = all.map((token, index) => token.significant ? index : -1).filter(index => index >= 0)
  while (positions.length && is(all[positions[positions.length - 1]], ';')) positions.pop()
  const tokens = positions.map(index => all[index])
  if (!tokens.length) return { ok: false, issue: 'empty' }
  if (tokens.some(token => is(token, ';'))) return { ok: false, issue: 'multiple' }
  const first = upper(tokens[0])
  if (!STATEMENTS.has(first)) return { ok: false, issue: 'statement' }
  const insert = first === 'INSERT'
  let seenInto = false
  let references = 0
  // Unqualified references to the table, as positions in `all`, for the qualified rewrite.
  const unqualified: number[] = []
  for (let index = 0; index < tokens.length; index++) {
    const word = upper(tokens[index])
    if (word === 'LOAD_FILE' && is(tokens[index + 1], '(')) return { ok: false, issue: 'function', detail: word }
    if (word === 'LATERAL') return { ok: false, issue: 'lateral' }
    let list: boolean
    if (word === 'FROM' || (word === 'UPDATE' && index === 0)) list = true
    else if (word === 'JOIN' || (word === 'STRAIGHT_JOIN' && index > 0)) list = false
    else if (word === 'INTO') {
      if (!insert || seenInto) return { ok: false, issue: 'into' }
      seenInto = true
      list = false
    }
    else continue
    let cursor = index + 1
    for (;;) {
      const part = tokens[cursor]
      if (!part) return { ok: false, issue: 'missingTable', detail: word }
      // A derived table: its own FROM clauses are checked as the scan reaches them.
      if (is(part, '(')) break
      if (!part.word && !part.quoted) return { ok: false, issue: 'missingTable', detail: word }
      let schema: string | null = null
      let name = nameOf(part)
      let nameAt = cursor
      cursor++
      const next = tokens[cursor + 1]
      if (is(tokens[cursor], '.') && next && (next.word || next.quoted)) {
        schema = name
        name = nameOf(next)
        nameAt = cursor + 1
        cursor += 2
      }
      // After INTO a parenthesis is the column list; elsewhere it makes the name a table function.
      if (word !== 'INTO' && is(tokens[cursor], '(')) return { ok: false, issue: 'tableFunction' }
      if (name.toLowerCase() !== table.toLowerCase() || (schema !== null && schema.toLowerCase() !== database.toLowerCase())) {
        return { ok: false, issue: 'otherTable', detail: schema === null ? name : `${schema}.${name}` }
      }
      references++
      if (schema === null) unqualified.push(positions[nameAt])
      if (upper(tokens[cursor]) === 'AS') cursor++
      const alias = tokens[cursor]
      if (alias && (alias.quoted || (alias.word && !CLAUSE_WORDS.has(upper(alias))))) cursor++
      if (list && is(tokens[cursor], ',')) { cursor++; continue }
      break
    }
    index = cursor - 1
  }
  if (!references) return { ok: false, issue: 'noTable' }
  const qualified = all.map((token, index) => unqualified.includes(index) ? `${quote(database)}.${quote(nameOf(token))}` : token.text).join('')
  return { ok: true, qualified }
}
