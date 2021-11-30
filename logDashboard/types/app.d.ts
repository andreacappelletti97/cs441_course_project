declare module 'app-types' {
  import('./log-types');
  import { LogLevel } from './log-types';
  interface LogData {
    log_id: string;
    log_message: string;
    timestamp: string;
    log_type: LogLevel;
  
  }
}
