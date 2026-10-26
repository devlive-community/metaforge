export interface UserRequest
{
    username: string | null | undefined
    password: string | null | undefined
    confirmPassword?: string
    captcha?: string
    timestamp?: number
    // Marks the error message returned after an operation
    message?: string
    loading?: boolean
}
