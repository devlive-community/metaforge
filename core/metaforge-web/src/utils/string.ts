export class StringUtils
{
    /**
     * Replaces all occurrences of the target string in the source string with the replacement string.
     *
     * @param {string} source - The original string
     * @param {string} target - The string to be replaced
     * @param {string} replacement - The string to replace the target
     * @return {string} The modified string after replacement
     */
    public static replace(source: string, target: string, replacement: string): string
    {
        return source.replace(target, replacement)
    }
}