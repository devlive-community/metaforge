import { BaseService } from '@/services/base'

const DEFAULT_PATH = '/api/v1/snippet'

class SnippetService
    extends BaseService
{
    constructor()
    {
        super(DEFAULT_PATH)
    }
}

export default new SnippetService()
