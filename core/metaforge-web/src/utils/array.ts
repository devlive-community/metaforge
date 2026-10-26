export class ArrayUtils
{
    static findDuplicates(array: any[]): any[]
    {
        const counts: { [key: string]: number } = {}
        const duplicates: string[] = []
        array.forEach(column => {
            counts[column.name] = (counts[column.name] || 0) + 1
            if (counts[column.name] === 2) {
                duplicates.push(column.name)
            }
        })
        return duplicates
    }
}