import packageJson from '../../package.json'

interface PackageJson
{
    name: string
    description: string
    version: string
}

export class PackageUtils
{
    public static get(key: keyof PackageJson): string
    {
        const pg = packageJson as PackageJson
        return pg[key]
    }
}
