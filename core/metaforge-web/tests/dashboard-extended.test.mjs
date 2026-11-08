import test from 'node:test'
import assert from 'node:assert/strict'
import {readFile} from 'node:fs/promises'
import {sourceModule} from '../../metaforge-ui/tests/component.mjs'
const layout=await sourceModule('../../metaforge-web/src/model/dashboard-layout.ts')
const {dashboardTools,dashboardWidget,dashboardWidgetLabelKey,emptyDashboard,readDashboardDocument,dashboardContentSettings,dashboardLayoutSettings,dashboardDataSettings,dashboardScreenEffectTool,dashboardScreenEffectSettings}=layout
const {groupedMetrics,numericStatistics,calendarMetrics,markdownBlocks,markdownInline}=await sourceModule('../../metaforge-web/src/model/dashboard-extended.ts')
const {dashboardDateRangeValid,dashboardFilterChoices,dashboardFilteredReports,dashboardFilterMatchesWidget}=await sourceModule('../../metaforge-web/src/model/dashboard-filters.ts')
const {defaultQueryChart}=await sourceModule('../../metaforge-web/src/model/query-chart.ts')
const added=['rollingTable','comparisonTable','statisticList','calendarHeatmap','radialProgress','thresholdCards','markdown','codeBlock','imageCompare','iconLinks','stepGuide','worldClock','footerContainer','sidebarContainer','scrollContainer','carouselContainer','cornerFrame','sectionBanner','techRuler','bezierConnector','matrixGrid','searchBeams','floatingHexagons','digitalScan',...layout.dashboardFilterTools]

test('every new tool has an identity, a translated name and survives saving both layouts',async()=>{
  const locales=await Promise.all(['en','zh-cn'].map(name=>readFile(new URL('../src/i18n/locales/'+name+'.json',import.meta.url),'utf8').then(JSON.parse)))
  assert.equal(new Set(dashboardTools).size,dashboardTools.length)
  const draft=emptyDashboard('New screen')
  for(const mode of ['dashboard','wall'])for(const [i,kind] of added.entries()){
    assert.ok(dashboardTools.includes(kind),kind)
    for(const locale of locales)assert.ok(locale[dashboardWidgetLabelKey(kind)]&&!locale[dashboardWidgetLabelKey(kind)].startsWith('dashboard.'),kind+' has a display name')
    draft.boards[mode].widgets.push({...dashboardWidget('w'+i,kind,kind,mode),w:mode==='wall'?300:6,h:mode==='wall'?200:4,
      ...(layout.dashboardFilterTool(kind) ? {reportCode:'chart-a',field:'order_date'} : {}),
      dataOptions:{year:2024,baselineField:'baseline',limit:5},contentOptions:{timezone:'Asia/Tokyo',language:'json',activeIndex:2,direction:'horizontal',comparePosition:35,autoplay:false},layoutOptions:{itemHeight:220,columns:3}})
  }
  const document=readDashboardDocument(JSON.stringify({version:3,draft,published:draft}))
  for(const version of [document.draft,document.published])for(const mode of ['dashboard','wall']){
    assert.deepEqual(version.boards[mode].widgets.map(widget=>widget.kind),added)
    for(const widget of version.boards[mode].widgets){assert.equal(widget.contentOptions.timezone,'Asia/Tokyo');assert.equal(widget.contentOptions.comparePosition,35);assert.equal(widget.contentOptions.activeIndex,2);assert.equal(widget.layoutOptions.itemHeight,220);assert.equal(widget.dataOptions.year,2024)}
    for(const widget of version.boards[mode].widgets.filter(widget=>layout.dashboardFilterTool(widget.kind))){
      assert.equal(widget.reportCode,'chart-a');assert.equal(widget.field,'order_date')
    }
  }
})
test('new configuration is bounded and old configuration keeps its defaults',()=>{
  const content=dashboardContentSettings({timezone:'not/a/zone',activeIndex:-2,comparePosition:999,language:'javascript',direction:'diagonal'})
  assert.equal(content.timezone,'UTC');assert.equal(content.activeIndex,0);assert.equal(content.comparePosition,100);assert.equal(content.language,'text');assert.equal(content.direction,'vertical')
  assert.equal(dashboardContentSettings().fontSize,16);assert.equal(dashboardContentSettings().interval,5)
  assert.equal(dashboardLayoutSettings({itemHeight:99999}).itemHeight,800)
  assert.equal(dashboardDataSettings({year:-1}).year,0)
})
test('grouped comparisons preserve missing data and never divide by a zero baseline',()=>{
  const data=groupedMetrics([{group:'A',value:10,base:0},{group:'A',value:5,base:0},{group:'B',value:-5,base:-10},{group:'C',value:null,base:3}],'group','value','sum','base')
  assert.deepEqual(data.map(row=>[row.name,row.value,row.baseline,row.difference,row.percent]),[['A',15,0,15,null],['B',-5,-10,5,50],['C',null,3,null,null]])
  assert.equal(groupedMetrics([{group:'A',value:0}],'group','value','sum')[0].value,0)
})
test('grouped widgets reuse the saved chart date grain and sorting',()=>{
  const options={...defaultQueryChart(['time','value']),category:'time',metric:'value',grain:'day',timezone:'UTC',sort:'asc'}
  const rows=groupedMetrics([{time:'2024-01-01T01:00:00Z',value:2},{time:'2024-01-01T12:00:00Z',value:3},{time:'2024-01-02T01:00:00Z',value:4}],'time','value','sum','',options)
  assert.equal(rows.length,2);assert.equal(rows[0].value,5);assert.equal(rows[1].value,4)
})
test('descriptive statistics ignore blank and nonnumeric cells, retaining real zero values',()=>{
  const [stats]=numericStatistics([{value:0},{value:'10'},{value:-4},{value:null},{value:''},{value:true},{value:'oops'}],['value'])
  assert.deepEqual(stats,{field:'value',count:3,min:-4,max:10,sum:6,avg:2})
  assert.deepEqual(numericStatistics([{value:null}],['value']),[])
})
test('heatmaps use valid dates, the chosen or latest data year, and actual day aggregates',()=>{
  const data=[{date:'2024-02-29T12:00:00',value:2},{date:'2024-02-29',value:3},{date:'2024-02-30',value:900},{date:'2026-01-01',value:0},{date:'no date',value:9}]
  assert.deepEqual(calendarMetrics(data,'date','value','sum',2024),{year:2024,points:[['2024-02-29',5]]})
  assert.deepEqual(calendarMetrics(data,'date','value','sum'),{year:2026,points:[['2026-01-01',0]]})
  assert.deepEqual(calendarMetrics(data,'date','value','sum',2025),{year:2025,points:[]})
})
test('Markdown is structured text with safe links, without executing raw HTML',()=>{
  const blocks=markdownBlocks('# Title\n- first\n- second\n> Quote\n```html\n<script>alert(1)</script>\n```')
  assert.equal(blocks[0].kind,'heading');assert.deepEqual(blocks[1].items,['first','second']);assert.equal(blocks[3].text,'<script>alert(1)</script>')
  const tokens=markdownInline('**Bold** `code` [safe](https://example.com) [unsafe](javascript:alert) <img onerror=x>')
  assert.equal(tokens.find(token=>token.kind==='link').href,'https://example.com/')
  assert.equal(tokens.filter(token=>token.kind==='link').length,1)
  assert.ok(tokens.some(token=>token.kind==='text'&&token.text.includes('<img')))
})
test('new full-screen effects are bounded overlays with working paint functions',async()=>{
  const {createAdditionalScreenPainters}=await sourceModule('../../metaforge-web/src/views/components/visual/effects/screen-effect-painters.ts')
  const calls=[],gradient={addColorStop(){}}
  const ctx=new Proxy({}, {get:(_target,key)=>String(key).startsWith('create')?()=>gradient:(...args)=>calls.push([key,...args]),set:()=>true})
  const env={width:800,height:450,size:12,wind:0,color:'#16A66A',customColor:''}
  const painters=createAdditionalScreenPainters(env,particle=>({x:particle.x*800,y:particle.y*450}))
  for(const kind of ['matrixGrid','searchBeams','floatingHexagons','digitalScan',
    'rainRipples','pollenDrift','orbitalTrails','floatingLanterns','sonarSweeps','shimmerCurtain','paperKites','inkBloom',
    'fallingFeathers','prismShards','concentricWaves','signalBars','isometricMesh','wovenRibbons',
    'constellationMarks','spiralDust','horizonGlow','mosaicFade']){
    assert.equal(dashboardScreenEffectTool(kind),true)
    const widget=dashboardWidget(kind,kind,kind,'wall');assert.equal(widget.movable,false);assert.equal(widget.showCard,false)
    assert.ok(dashboardScreenEffectSettings(kind,{quantity:99999}).quantity<=200)
    const before=calls.length;painters[kind](ctx,{x:.4,y:.3,depth:.8,phase:1,spin:1},1,0);assert.ok(calls.length>before)
  }
})
test('gallery thumbnails never start the new paging and carousel playback',async()=>{
  const {screenPreview}=await sourceModule('../../metaforge-web/src/model/screen-preview.ts')
  const draft=emptyDashboard();draft.boards.dashboard.widgets=['rollingTable','carouselContainer','dateRangeFilter'].map(kind=>({...dashboardWidget(kind,kind,kind),contentOptions:{autoplay:true,interval:2}}))
  const screen={name:'Static',configure:JSON.stringify({version:3,draft})},before=screen.configure
  const preview=screenPreview(screen)
  const [rolling,carousel,filter]=preview.design.boards.dashboard.widgets
  for(const widget of [rolling,carousel])assert.equal(widget.contentOptions.autoplay,false)
  assert.equal(filter.kind,'text')
  assert.equal(screen.configure,before)
})
test('board filter clauses intersect on the same report, retaining original row references',()=>{
  const matching={group:0, text:'EAST dock', day:'2024-02-29 08:00:00'}
  const rows=[matching,{group:1,text:'east dock',day:'2024-02-29'},{group:0,text:'west dock',day:'2024-02-29'},
    {group:0,text:'east dock',day:'2024-02-30'},{group:0,text:'east dock',day:'2024-02-28'}]
  const reads=[]
  const result=dashboardFilteredReports([
    {kind:'selectFilter',reportCode:'chart-a',field:'group',value:'0'},
    {kind:'searchFilter',reportCode:'chart-a',field:'text',value:'  eAst '},
    {kind:'dateRangeFilter',reportCode:'chart-a',field:'day',start:'2024-02-29',end:'2024-02-29'},
    {kind:'searchFilter',reportCode:'chart-b',field:'text',value:'  '},
  ], code=>{reads.push(code);return rows})
  assert.deepEqual(reads,['chart-a'])
  assert.equal(result.get('chart-a').size,1)
  assert.ok(result.get('chart-a').has(matching))
  assert.equal(result.has('chart-b'),false)
})
test('filter options retain zero and false and date filters allow either bound',()=>{
  assert.deepEqual(dashboardFilterChoices([{key:10},{key:2},{key:0},{key:false},{key:null},{key:{}},{key:2}], 'key', 'en').map(option=>option.value),['0','2','10','false'])
  assert.equal(dashboardDateRangeValid('2026-11-02','2026-11-01'),false)
  assert.equal(dashboardDateRangeValid('2025-02-29',''),false)
  const late={date:'2026-11-06T08:00:00Z'}, early={date:'2026-11-04'}, rows=[early,late,{date:null}]
  const bound={kind:'dateRangeFilter',reportCode:'chart-a',field:'date',start:'2026-11-05',end:''}
  assert.deepEqual([...dashboardFilteredReports([bound],()=>rows).get('chart-a')],[late])
  assert.deepEqual([...dashboardFilteredReports([{...bound,start:'',end:'2026-11-04'}],()=>rows).get('chart-a')],[early])
  assert.equal(dashboardFilteredReports([{...bound,end:'2026-11-01'}],()=>rows).size,0)
  assert.equal(dashboardFilterMatchesWidget(bound,{...bound,field:'changed'}),false)
  assert.equal(dashboardFilterMatchesWidget(bound,{...bound,reportCode:'chart-b'}),false)
})
