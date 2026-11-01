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

/** What the sign-in pages offer: the switches as they take effect (email ones need a mail server). */
export interface AuthOptions extends AuthSettings {
  providers: string[]
}

export type CodePurpose = 'SIGNUP' | 'RESET'

export const EMAIL_PATTERN = /^[^@\s]+@[^@\s]+\.[^@\s]+$/
export const USERNAME_PATTERN = /^[A-Za-z0-9_]{3,32}$/
export const URL_PATTERN = /^https?:\/\/\S+$/i

export const defaultAuthSettings = (): AuthSettings => ({
  registration: true, captcha: true, rememberMe: false, rememberDays: 30, emailSignin: false, emailVerification: false, passwordReset: false, termsUrl: '', privacyUrl: '',
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
