export class ErDiagramOptions
{
    public table: any
    public columns: any[] = []
}

export class ErDiagramData
{
    public id: any
    public shape = 'er-rect'
    public label: string = ''
    public width = 150
    public height = 24
    public position = { x: 24, y: 150 }
    public ports = Array<ErDiagramPort>()
}

export class ErDiagramPort
{
    public id: any
    public group = 'list'
    public attrs = {
        portNameLabel: { text: null },
        portTypeLabel: { text: null }
    }
}
