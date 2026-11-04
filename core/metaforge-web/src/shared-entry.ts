// Imported first by main.ts. The hash router captures the address when its module is evaluated,
// so shared report paths must become the public report route before any module creates it.
import { routeSharedReportPath } from '@/router/shared'

routeSharedReportPath(window.location, window.history)
