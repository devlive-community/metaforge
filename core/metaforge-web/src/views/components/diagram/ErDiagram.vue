<template>
  <div id="container"/>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import { Cell, Graph, Shape } from '@antv/x6'
import { ErDiagramData, ErDiagramOptions, ErDiagramPort } from '@/views/components/diagram/ErDiagramOptions'

const HEIGHT = 24
const WIDTH = 150

export default defineComponent({
  name: 'ErDiagram',
  props: {
    options: {
      type: ErDiagramOptions,
      required: true
    }
  },
  mounted()
  {
    Graph.registerPortLayout(
        'erPortPosition',
        (portsPositionArgs: any) => {
          return portsPositionArgs.map((_: any, index: number) => {
            return {
              position: {
                x: 0,
                y: (index + 1) * HEIGHT
              },
              angle: 0
            }
          })
        },
        true
    )
    Graph.registerNode(
        'er-rect',
        {
          inherit: 'rect',
          markup: [
            {
              tagName: 'rect',
              selector: 'body'
            },
            {
              tagName: 'text',
              selector: 'label'
            }
          ],
          attrs: {
            rect: {
              strokeWidth: 1,
              stroke: '#5F95FF',
              fill: '#5F95FF'
            },
            label: {
              fontWeight: 'bold',
              fill: '#ffffff',
              fontSize: 12
            }
          },
          ports: {
            groups: {
              list: {
                markup: [
                  {
                    tagName: 'rect',
                    selector: 'portBody'
                  },
                  {
                    tagName: 'text',
                    selector: 'portNameLabel'
                  },
                  {
                    tagName: 'text',
                    selector: 'portTypeLabel'
                  }
                ],
                attrs: {
                  portBody: {
                    width: WIDTH,
                    height: HEIGHT,
                    strokeWidth: 1,
                    stroke: '#5F95FF',
                    fill: '#EFF4FF',
                    magnet: true
                  },
                  portNameLabel: {
                    ref: 'portBody',
                    refX: 6,
                    refY: 6,
                    fontSize: 10
                  },
                  portTypeLabel: {
                    ref: 'portBody',
                    refX: 95,
                    refY: 6,
                    fontSize: 10
                  }
                },
                position: 'erPortPosition'
              }
            }
          }
        },
        true
    )
  },
  created()
  {
    this.handlerInitialize()
  },
  methods: {
    handlerInitialize()
    {
      setTimeout(() => {
        const graph = new Graph({
          container: document.getElementById('container')!,
          connecting: {
            router: {
              name: 'er',
              args: {
                offset: 25,
                direction: 'H'
              }
            },
            createEdge()
            {
              return new Shape.Edge({
                attrs: {
                  line: {
                    stroke: '#A2B1C3',
                    strokeWidth: 2
                  }
                }
              })
            }
          }
        })
        const data = []
        const erDiagramData: ErDiagramData = new ErDiagramData()
        erDiagramData.id = this.options.table.id
        erDiagramData.label = this.options.table.name
        const ports: ErDiagramPort[] = []
        this.options.columns.forEach((item: { id: any; name: any; type: any; }) => {
          const erDiagramPort: ErDiagramPort = new ErDiagramPort()
          erDiagramPort.id = item.id
          erDiagramPort.attrs.portNameLabel.text = item.name
          erDiagramPort.attrs.portTypeLabel.text = item.type
          ports.push(erDiagramPort)
        })
        erDiagramData.ports = ports
        data.push(erDiagramData)

        const cells: Cell[] = []
        data.forEach((item: any) => {
          if (item.shape === 'edge') {
            cells.push(graph.createEdge(item))
          }
          else {
            cells.push(graph.createNode(item))
          }
        })
        graph.resetCells(cells)
        graph.zoomToFit({ padding: 10, maxScale: 1 })
      })
    }
  }
})
</script>

<style scoped>
#container {
  width: 100%;
  height: 400px;
  justify-content: center;
  overflow-y: auto;
}
</style>
