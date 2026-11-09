export interface UserRequest
{
    username: string | null | undefined
    password: string | null | undefined
    confirmPassword?: string
    captcha?: string | number
    timestamp?: number
    email?: string
    /** The mailed code on sign-up. */
    code?: string
    /** Keep the sign-in for the remembered days. */
    remember?: boolean
    // Marks the error message returned after an operation
    message?: string
    loading?: boolean
}
