export enum Expression
{
  SUM = 'SUM',
  COUNT = 'COUNT',
  MAX = 'MAX',
  MIN = 'MIN',
  AVG = 'AVG',
  // Filter Expression
  EQ = 'EQ',
  NE = 'NEQ',
  IS_NULL = 'NULL',
  IS_NOT_NULL = 'NNULL',
  IS_LIKE = 'LIKE',
  IS_NOT_LIKE = 'NLIKE',
  GT = 'GT',
  GTE = 'GTE',
  LT = 'LT',
  LTE = 'LTE',
}
