/** Sign-in and registration features an administrator turns on in system settings. */
export interface AuthSettings {
  registration: boolean
  captcha: boolean
  rememberMe: boolean
  rememberDays: number
  emailSignin: boolean
  emailVerification: boolean
  passwordReset: boolean
  termsUrl: string
  privacyUrl: string
  /** Where MetaForge is opened, for third-party sign-in callbacks; empty to take it from each request. */
  siteUrl: string
}

/** The SMTP server; the password is never sent back, `passwordSet` tells whether one is stored. */
export interface MailSettings {
  host: string
  port: number
  ssl: boolean
  username: string
  password: string
  from: string
  passwordSet?: boolean
}

export type OAuthProviderId = 'github' | 'google' | 'oidc'
export const OAUTH_PROVIDERS: readonly OAuthProviderId[] = ['github', 'google', 'oidc']

/**
 * A third-party sign-in provider. The secret is never sent back, `secretSet` tells whether one is stored;
 * the endpoints and scopes are only set for OpenID Connect, GitHub and Google have fixed ones.
 */
export interface OAuthSettings {
  enabled: boolean
  clientId: string
  clientSecret: string
  secretSet?: boolean
  name: string
  issuer: string
  authorizationUrl: string
  tokenUrl: string
  userinfoUrl: string
  scopes: string
}

export type OAuthGroup = Record<OAuthProviderId, OAuthSettings>

/** A provider the sign-in page shows: only the ones that are on and fully configured are listed. */
export interface OAuthOption {
  id: OAuthProviderId
  name: string
}

/** What the sign-in pages offer: the switches as they take effect (email ones need a mail server). */
export interface AuthOptions extends Omit<AuthSettings, 'siteUrl'> {
  providers: OAuthOption[]
}

/** Why a third-party sign-in came back without signing in (`expired`: the ticket was used or ran out); anything else counts as `failed`. */
export const OAUTH_ERRORS = ['disabled', 'state', 'denied', 'failed', 'registration', 'inactive', 'expired'] as const
export type OAuthError = typeof OAUTH_ERRORS[number]

export type CodePurpose = 'SIGNUP' | 'RESET'

export const EMAIL_PATTERN = /^[^@\s]+@[^@\s]+\.[^@\s]+$/
export const USERNAME_PATTERN = /^[A-Za-z0-9_]{3,32}$/
export const URL_PATTERN = /^https?:\/\/\S+$/i

export const defaultAuthSettings = (): AuthSettings => ({
  registration: true, captcha: true, rememberMe: false, rememberDays: 30, emailSignin: false, emailVerification: false, passwordReset: false, termsUrl: '', privacyUrl: '', siteUrl: '',
})

export const defaultMailSettings = (): MailSettings => ({ host: '', port: 465, ssl: true, username: '', password: '', from: '', passwordSet: false })

/** Email verification and password reset only work once mail can be sent. */
export const mailReady = (mail: MailSettings) => !!mail.host.trim() && !!mail.from.trim()

/** The switches that need a mail server but have none yet. */
export function waitingForMail(auth: AuthSettings, mail: MailSettings): ('emailVerification' | 'passwordReset')[] {
  if (mailReady(mail)) return []
  return (['emailVerification', 'passwordReset'] as const).filter(key => auth[key])
}

/** The agreement links shown on sign-up, in order; the box is required once there is one. */
export function agreements(options: Pick<AuthSettings, 'termsUrl' | 'privacyUrl'>): { key: 'terms' | 'privacy'; url: string }[] {
  return [{ key: 'terms' as const, url: options.termsUrl }, { key: 'privacy' as const, url: options.privacyUrl }].filter(item => URL_PATTERN.test(item.url || ''))
}

export const defaultOAuthSettings = (): OAuthSettings => ({
  enabled: false, clientId: '', clientSecret: '', secretSet: false, name: '', issuer: '', authorizationUrl: '', tokenUrl: '', userinfoUrl: '', scopes: 'openid email profile',
})

export const defaultOAuthGroup = (): OAuthGroup => ({ github: defaultOAuthSettings(), google: defaultOAuthSettings(), oidc: defaultOAuthSettings() })

/** The client is set, a secret is entered or stored, and OpenID Connect has its three endpoints. */
export function oauthConfigured(id: OAuthProviderId, settings: OAuthSettings): boolean {
  const client = !!settings.clientId.trim() && (!!settings.clientSecret.trim() || !!settings.secretSet)
  if (id !== 'oidc') return client
  return client && [settings.authorizationUrl, settings.tokenUrl, settings.userinfoUrl].every(url => URL_PATTERN.test(url.trim()))
}

/** The providers that are turned on but still miss something, in display order. */
export const incompleteProviders = (group: OAuthGroup) => OAUTH_PROVIDERS.filter(id => group[id].enabled && !oauthConfigured(id, group[id]))

/** The callback address to register at the provider. */
export function oauthCallbackUrl(siteUrl: string, origin: string, id: OAuthProviderId): string {
  return `${ (siteUrl.trim() || origin).replace(/\/+$/, '') }/api/auth/oauth/${ id }/callback`
}

/** Only the providers the page knows how to show. */
export const knownProviders = (providers: unknown): OAuthOption[] => Array.isArray(providers)
  ? providers.filter((item): item is OAuthOption => !!item && OAUTH_PROVIDERS.includes(item.id) && typeof item.name === 'string')
  : []

export const oauthError = (value: unknown): OAuthError => (OAUTH_ERRORS as readonly unknown[]).includes(value) ? value as OAuthError : 'failed'
