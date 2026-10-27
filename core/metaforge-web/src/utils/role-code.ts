export function normalizeRoleCode(code?: string): string {
    return (code || '').trim()
}

export function validRoleCode(code?: string): boolean {
    const value = normalizeRoleCode(code)
    return !value || /^[A-Za-z0-9][A-Za-z0-9_-]{0,63}$/.test(value)
}

export function roleSaveTarget(role: { id?: number; code?: string }, originalCode?: string): { method: 'post' | 'put'; suffix: string } {
    if (originalCode) return { method: 'put', suffix: `/${encodeURIComponent(originalCode)}` }
    return { method: role.id && role.id > 0 ? 'put' : 'post', suffix: '' }
}

export function roleErrorKey(code: number): string | undefined {
    return ({ 4028: 'role.validate.codeFormat', 4029: 'role.validate.codeExists', 4030: 'role.tip.codeReadonly', 4031: 'role.validate.identityMismatch', 4032: 'role.validate.codeConstraint', 4033: 'role.validate.invalidMenus', 4034: 'role.validate.menuSaveFailed', 4035: 'role.validate.menuLoadFailed' } as Record<number, string>)[code]
}
