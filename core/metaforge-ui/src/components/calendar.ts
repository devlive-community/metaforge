export function dateKey(date: Date): string {
    return `${String(date.getFullYear()).padStart(4, '0')}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

export function parseDate(value: string): Date | undefined {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return undefined
    const [year, month, day] = value.split('-').map(Number)
    if (year < 1000 || year > 9999) return undefined
    const date = new Date(year, month - 1, day, 12)
    return dateKey(date) === value ? date : undefined
}

export function monthDays(date: Date): Date[] {
    const first = new Date(date.getFullYear(), date.getMonth(), 1, 12)
    const offset = (first.getDay() + 6) % 7
    return Array.from({ length: 42 }, (_, index) => new Date(first.getFullYear(), first.getMonth(), 1 - offset + index, 12))
}

export function moveMonth(date: Date, offset: number): Date {
    const last = new Date(date.getFullYear(), date.getMonth() + offset + 1, 0, 12).getDate()
    return new Date(date.getFullYear(), date.getMonth() + offset, Math.min(date.getDate(), last), 12)
}
