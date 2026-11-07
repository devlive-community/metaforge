// Imported first by main.ts: the router reads the address when its module is created, so an old
// hash route (/#/query) must become its path (/query) before that.
import { upgradeLegacyHashRoute } from '@/router/legacy-hash'

upgradeLegacyHashRoute(window.location, window.history)
