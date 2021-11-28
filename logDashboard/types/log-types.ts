export const logLevelList = [
  'WARN',
  'ERROR',
  'SEVERE',
  'WARNING',
  'DEBUG',
  'INFO',
] as const;
export type LogLevel = typeof logLevelList[number];
export const logLevelColors: Record<LogLevel, string> = {
  DEBUG: '#1c5253',
  ERROR: '#c3423f',
  INFO: '#453643',
  SEVERE: '#fabc2a',
  WARN: '#f64740',
  WARNING: '#5c80bc',
};
