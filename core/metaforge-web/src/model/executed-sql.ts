/** What a DDL/DML operation ran against the database, for the "executed SQL" dialog. */
export interface ExecutedSql {
  /** Translated operation label, e.g. "Add field". */
  operation: string
  table: string
  /** Optional extra row in the summary, e.g. the field name or the number of rows. */
  target?: { label: string; value: string }
  /** Local time, YYYY-MM-DD HH:mm:ss. */
  executedAt: string
  success: boolean
  /** Total execution time over every statement, in milliseconds. */
  elapsed: number
  statements: string[]
  message: string
  result: { headers: string[]; rows: Record<string, unknown>[] } | null
}

export type SqlTokenKind = 'keyword' | 'type' | 'string' | 'identifier' | 'number' | 'comment' | 'space' | 'plain'

export interface SqlToken {
  kind: SqlTokenKind
  text: string
}

const KEYWORDS = new Set(`ADD AFTER ALL ALTER AND AS ASC AUTO_INCREMENT BETWEEN BY CASCADE CHANGE CHARACTER CHARSET COLLATE COLUMN COMMENT
CONSTRAINT CREATE CURRENT_TIMESTAMP DATABASE DEFAULT DELETE DESC DISTINCT DROP ENGINE EXISTS FALSE FIRST FOREIGN FROM FULLTEXT GROUP
HAVING IF IN INDEX INNER INSERT INTO IS JOIN KEY LEFT LIKE LIMIT MODIFY NOT NULL OFFSET ON OR ORDER OUTER PRIMARY REFERENCES RENAME
RIGHT SCHEMA SELECT SET TABLE TO TRUE TRUNCATE UNIQUE UNSIGNED UPDATE USING VALUES WHERE ZEROFILL
TRIGGER DEFINER BEFORE EACH ROW BEGIN END IF THEN ELSE ELSEIF CASE WHEN WHILE DO LOOP REPEAT UNTIL DECLARE CALL RETURN NEW OLD SIGNAL SQLSTATE CONSTRAINT`.split(/\s+/))

const TYPES = new Set(`BIGINT BINARY BIT BLOB BOOL BOOLEAN CHAR DATE DATETIME DECIMAL DOUBLE ENUM FLOAT INT INTEGER JSON LONGBLOB LONGTEXT
MEDIUMBLOB MEDIUMINT MEDIUMTEXT NUMERIC REAL SMALLINT TEXT TIME TIMESTAMP TINYBLOB TINYINT TINYTEXT VARBINARY VARCHAR YEAR`.split(/\s+/))

const TOKEN = /(--[^\n]*|\/\*[\s\S]*?\*\/)|('(?:[^'\\]|''|\\[\s\S])*'?)|(`(?:[^`]|``)*`?)|(\s+)|(\d+(?:\.\d+)?\b)|([A-Za-z_][A-Za-z0-9_$]*)|([\s\S])/g

export function tokenizeSql(sql: string): SqlToken[] {
  const tokens: SqlToken[] = []
  for (const match of String(sql || '').matchAll(TOKEN)) {
    const [text, comment, string, identifier, space, number, word] = match
    let kind: SqlTokenKind = 'plain'
    if (comment) kind = 'comment'
    else if (string) kind = 'string'
    else if (identifier) kind = 'identifier'
    else if (space) kind = 'space'
    else if (number) kind = 'number'
    else if (word) kind = KEYWORDS.has(word.toUpperCase()) ? 'keyword' : TYPES.has(word.toUpperCase()) ? 'type' : 'plain'
    tokens.push({ kind, text })
  }
  return tokens
}

/** Clauses that start an indented line inside a statement. */
const CLAUSES = new Set(['ADD', 'MODIFY', 'CHANGE', 'DROP', 'AFTER', 'FIRST', 'COMMENT', 'VALUES', 'SET', 'WHERE', 'AND', 'OR', 'FROM', 'ORDER', 'LIMIT'])

/**
 * Lays a statement out one clause per line, the way the prototype shows DDL: the statement head on
 * the first line, then each clause indented, with nullability and defaults on their own line.
 * Strings, identifiers and comments are never touched.
 */
export function formatSql(sql: string): string {
  const statements: SqlToken[][] = [[]]
  for (const token of tokenizeSql(sql)) {
    if (token.kind === 'space') continue
    statements[statements.length - 1].push(token)
    if (token.text === ';') statements.push([])
  }
  return statements.filter(list => list.length).map(list => {
    let out = ''
    let depth = 0
    let previous: SqlToken | null = null
    const word = (token: SqlToken | null) => token ? token.text.toUpperCase() : ''
    list.forEach((token, index) => {
      const upper = word(token)
      const prevUpper = word(previous)
      const isClause = index > 0 && depth === 0 && token.kind === 'keyword' && (
        CLAUSES.has(upper)
        // NULL / NOT NULL starts the constraint line of a column definition, but not "DEFAULT NULL" or "IS NULL".
        || (upper === 'NOT' && word(list[index + 1]) === 'NULL')
        || (upper === 'NULL' && !['NOT', 'DEFAULT', 'IS'].includes(prevUpper))
      )
      if (isClause) out += '\n  '
      else if (previous && !(token.text === ',' || token.text === ')' || token.text === ';' || token.text === '.'
          || previous.text === '(' || previous.text === '.'
          // varchar(255) and count(*) hug their parenthesis; a column list after a table name does not.
          || (token.text === '(' && (previous.kind === 'type' || previous.kind === 'plain')))) out += ' '
      out += token.text
      if (token.text === '(') depth++
      if (token.text === ')') depth = Math.max(0, depth - 1)
      previous = token
    })
    return out
  }).join('\n\n')
}

/** Statements joined for display and download; each ends with a semicolon. */
export const joinStatements = (statements: string[]) =>
  statements.map(statement => statement.trim()).filter(Boolean).map(statement => statement.endsWith(';') ? statement : `${statement};`).join('\n\n')

export function formatTimestamp(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}
