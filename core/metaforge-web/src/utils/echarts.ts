/** Share the engine download while each chart keeps its own loading and lifecycle state. */
let engine: Promise<typeof import('echarts')> | undefined
export function loadECharts() {
  if (!engine) engine = import('echarts').then(async module => {
    await import('echarts-wordcloud')
    return module
  }).catch(error => { engine = undefined; throw error })
  return engine
}
