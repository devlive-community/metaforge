<template>
  <aside class="flex h-full min-h-0 min-w-0 flex-col overflow-hidden border-l border-line bg-surface shadow-mf-sm">
    <DashboardPanelHeader :title="$t(groupRect ? 'dashboard.studio.groupProperties' : widget ? 'dashboard.studio.properties' : 'dashboard.studio.pageSettings')" :pinned="pinned" :pinnable="pinnable"
                          @pin="emit('pin')" @close="emit('close')"/>
    <div v-if="groupRect" class="inspector-section min-h-0 flex-1 content-start overflow-auto">
      <DashboardInspectorGroup icon="layout" :title="$t('dashboard.studio.layerGroup', { count: groupCount })">
        <div class="grid grid-cols-2 gap-3">
          <MfFormItem v-for="key in dimensions" :key="key" :name="key" :label="$t('dashboard.studio.rect.' + key)">
            <MfInputNumber :model-value="groupRect[key]" :min="key === 'w' || key === 'h' ? 1 : 0" :max="limit(key)"
                           :aria-label="$t('dashboard.studio.rect.' + key)" :precision="0" decimal-only commit-on-change
                           :disabled="key === 'w' || key === 'h' ? !groupResizable : !groupMovable"
                           @update:model-value="value => value !== undefined && emit('group', { [key]: value })"/>
          </MfFormItem>
        </div>
        <MfButton size="sm" @click="emit('ungroup')">{{ $t('dashboard.studio.ungroup') }}</MfButton>
      </DashboardInspectorGroup>
    </div>
    <MfTabs v-else v-model:active-key="tab" fill class="inspector-tabs">
      <MfTabPane item-key="data" :tab="$t(widget ? 'dashboard.studio.data' : 'dashboard.studio.pageSettings')">
        <template #tab><span class="flex items-center justify-center gap-1.5"><Database :size="15"/>{{ $t(widget ? 'dashboard.studio.data' : 'dashboard.studio.pageSettings') }}</span></template>
        <div class="inspector-section">
          <DashboardInspectorGroup v-if="widget" :title="$t('dashboard.studio.widgetInfo')">
            <MfFormItem :label="$t('dashboard.studio.widgetName')">
              <MfInput :model-value="widget.name" :maxlength="100" @update:model-value="emit('widget', { name: $event })"/>
            </MfFormItem>
            <MfFormItem :label="$t('common.description')">
              <div class="grid w-full gap-1">
                <MfTextarea :model-value="widget.description" :rows="3" :maxlength="200"
                            :placeholder="$t('dashboard.studio.widgetDescription')" @update:model-value="emit('widget', { description: $event })"/>
                <span class="text-right text-[11px] tabular-nums text-content-tertiary">{{ widget.description.length }} / 200</span>
              </div>
            </MfFormItem>
            <MfFormItem :label="$t('dashboard.studio.componentType')">
              <MfSelect :model-value="widget.kind" :options="widgetKinds" searchable @update:model-value="changeKind">
                <template v-if="isChartType(widget.kind)" #prefix>
                  <QueryChartTypeIcon :type="widget.kind" class="text-primary"/>
                </template>
              </MfSelect>
            </MfFormItem>
          </DashboardInspectorGroup>
          <DashboardInspectorGroup v-else ref="pageInfo" :title="$t('dashboard.studio.pageInfo')">
            <MfForm ref="settingsForm" class="grid gap-3" :model="design" @finish="emit('save')">
              <MfFormItem name="name" :label="$t('common.name')" :rules="[{ required: true, validator: value => !!String(value ?? '').trim(), message: $t('dashboard.validator.name.required') }]">
                <MfInput :model-value="design.name" :maxlength="100" :placeholder="$t('dashboard.placeholder.name')" @update:model-value="emit('page', { name: $event })"/>
              </MfFormItem>
              <MfFormItem name="description" :label="$t('common.description')">
                <MfTextarea :model-value="design.description" :rows="3" :maxlength="500" @update:model-value="emit('page', { description: $event })"/>
              </MfFormItem>
            </MfForm>
          </DashboardInspectorGroup>

          <DashboardInspectorGroup v-if="widget && dashboardContainerTool(widget.kind)" icon="layout" :title="$t('dashboard.studio.containerSettings')">
            <MfFormItem name="children" :label="$t('dashboard.studio.containerChildren')">
              <MfSelect :model-value="containerChildren.map(child => child.id)" :options="containerCandidates" multiple searchable @update:model-value="setContainerChildren"/>
            </MfFormItem>
            <MfFormItem name="addChild" :label="$t('dashboard.studio.addContainerChild')">
              <MfSelect :options="childKinds" searchable :placeholder="$t('dashboard.studio.chooseComponent')" @update:model-value="value => emit('add-child', String(value))"/>
            </MfFormItem>
            <div class="grid grid-cols-2 gap-2">
              <MfFormItem v-for="field in containerFields" :key="field.key" :name="'layoutOptions.' + field.key" :label="$t(widget.kind === 'footerContainer' && field.key === 'header' ? 'dashboard.studio.footerHeight' : 'dashboard.studio.layoutField.' + field.key)">
                <MfInputNumber :model-value="layout[field.key]" :min="field.min" :max="field.max" :precision="0" decimal-only commit-on-change
                               @update:model-value="value => value !== undefined && updateLayout({ [field.key]: value })"/>
              </MfFormItem>
            </div>
            <template v-if="widget.kind === 'carouselContainer'">
              <label class="toggle-row">{{ $t('dashboard.studio.autoplay') }}<MfSwitch :model-value="contentSettings.autoplay" @update:model-value="value => updateContent({ autoplay: value })"/></label>
              <MfFormItem name="contentOptions.interval" :label="$t('dashboard.studio.playInterval')"><MfInputNumber :model-value="contentSettings.interval" :min="2" :max="60" :precision="0" decimal-only commit-on-change @update:model-value="value => value !== undefined && updateContent({ interval: value })"/></MfFormItem>
              <label class="toggle-row">{{ $t('dashboard.studio.pauseOnHover') }}<MfSwitch :model-value="contentSettings.pauseOnHover" @update:model-value="value => updateContent({ pauseOnHover: value })"/></label>
            </template>
            <div v-if="containerChildren.length" class="grid gap-1">
              <MfButton v-for="child in containerChildren" :key="child.id" variant="ghost" size="sm" class="justify-start!" @click="emit('select-child', child.id)"><span class="truncate">{{ child.name }}</span></MfButton>
            </div>
            <p class="text-xs leading-5 text-content-tertiary">{{ $t('dashboard.studio.containerHint') }}</p>
          </DashboardInspectorGroup>
          <DashboardInspectorGroup v-if="widget?.parentId" icon="layout" :title="$t('dashboard.studio.parentContainer')">
            <p class="text-sm">{{ board.widgets.find(item => item.id === widget.parentId)?.name }}</p>
            <p class="text-xs text-content-tertiary">{{ $t('dashboard.studio.childLayoutHint') }}</p>
            <MfButton size="sm" @click="emit('detach')">{{ $t('dashboard.studio.detachChild') }}</MfButton>
          </DashboardInspectorGroup>

          <DashboardInspectorGroup v-if="widget && effectSettings" icon="appearance" :title="$t('dashboard.studio.screenEffects')">
            <label class="toggle-row">{{ $t('dashboard.studio.effectEnabled') }}<MfSwitch :model-value="effectSettings.enabled" @update:model-value="updateScreenEffect({ enabled: $event })"/></label>
            <div class="grid grid-cols-2 gap-2">
              <MfFormItem v-for="key in effectFields" :key="key" :name="'screenEffect.' + key" :label="$t('dashboard.studio.effectField.' + key)">
                <MfInputNumber :model-value="effectSettings[key]" :min="effectLimit(key).min" :max="effectLimit(key).max" :precision="0"
                               :aria-label="$t('dashboard.studio.effectField.' + key)" decimal-only commit-on-change
                               @update:model-value="value => value !== undefined && updateScreenEffect({ [key]: value })"/>
              </MfFormItem>
            </div>
            <MfFormItem name="color" :label="$t('dashboard.studio.accentColor')">
              <MfInput :model-value="widget.color" :placeholder="effectDefaultColor" :maxlength="7" clearable @update:model-value="emit('widget', { color: $event })"/>
            </MfFormItem>
            <MfFormItem name="screenEffect.layer" :label="$t('dashboard.studio.effectLayerLabel')">
              <MfSelect :model-value="effectSettings.layer" :options="choices('effectLayer', ['foreground', 'background'])" @update:model-value="setEffectLayer"/>
            </MfFormItem>
            <p class="text-xs leading-5 text-content-tertiary">{{ $t('dashboard.studio.screenEffectHint') }}</p>
            <MfButton size="sm" @click="emit('preview')"><Play :size="14"/>{{ $t('dashboard.studio.previewEffect') }}</MfButton>
          </DashboardInspectorGroup>

          <DashboardInspectorGroup v-if="widget && usesAsset" :key="widget.id + ':' + widget.kind" icon="data" :default-open="!!specialSetup || dashboardDataTool(widget.kind) || filterWidget"
                                   :title="$t(specialSetup ? 'queryChart.mapping.title.' + specialSetup : 'dashboard.studio.source')">
            <MfFormItem :label="$t('dashboard.studio.chartAsset')">
              <MfSelect :model-value="widget.reportCode" :options="reportOptions" searchable :placeholder="$t('dashboard.studio.chooseAsset')"
                        @update:model-value="changeAsset"/>
            </MfFormItem>
            <MfFormItem v-if="filterWidget && snapshot && widget.kind !== 'clearFilters'" :label="$t('queryChart.selectField')" required-mark>
              <MfSelect :model-value="widget.field" :options="filterFields" searchable
                        :placeholder="$t(dateFilterWidget ? 'dashboard.studio.chooseDateField' : 'queryChart.selectField')"
                        @update:model-value="emit('widget', { field: String($event ?? '') })"/>
            </MfFormItem>
            <p v-if="filterWidget && snapshot" class="text-xs leading-5 text-content-tertiary">{{ $t(widget.kind === 'clearFilters'
              ? 'dashboard.studio.filterClearHint' : 'dashboard.studio.filterScopeHint') }}</p>
            <p v-if="snapshot && widget.kind === 'booleanFilter'" class="text-xs leading-5 text-content-tertiary">{{ $t('dashboard.studio.relation.booleanHint') }}</p>
            <QueryChartDataFields v-if="specializedMappings" :options="specializedMappings"
                                  :fields="chartFields" :numeric-fields="chartNumericFields" @change="changeMappings"/>
            <template v-if="dataAggregation && snapshot">
              <MfFormItem :label="$t('dashboard.studio.dataCategory')" required-mark>
                <MfSelect :model-value="widget.chartOptions?.category ?? snapshot.queryChart.options.category" :options="chartFields" searchable
                          :placeholder="$t('queryChart.selectField')"
                          @update:model-value="changeMappings({ category: String($event ?? '') })"/>
              </MfFormItem>
              <MfFormItem v-if="widget.kind === 'pivot'" :label="$t('dashboard.studio.pivotColumn')" required-mark>
                <MfSelect :model-value="widget.chartOptions?.series ?? snapshot.queryChart.options.series" :options="chartFields" searchable
                          :placeholder="$t('queryChart.selectField')"
                          @update:model-value="changeMappings({ series: String($event ?? '') })"/>
              </MfFormItem>
              <MfFormItem :label="$t('dashboard.studio.dataAggregation')">
                <MfSelect :model-value="dataAggregation" :options="aggregations" @update:model-value="setDataAggregation"/>
              </MfFormItem>
              <MfFormItem :label="$t('dashboard.studio.dataMetric')" :required-mark="dataAggregation !== 'count'">
                <MfSelect :model-value="dataMetric || (dataAggregation === 'count' ? 0 : undefined)" :options="dataMetricOptions" searchable
                          :placeholder="$t('queryChart.selectField')"
                          @update:model-value="changeMappings({ metric: typeof $event === 'string' ? $event : '' })"/>
              </MfFormItem>
            </template>
            <MfFormItem v-if="widget.kind === 'dataSummary' && snapshot" :label="$t('dashboard.studio.summaryField')" required-mark>
              <MfSelect :model-value="widget.field" :options="chartNumericFields" searchable
                        :placeholder="$t('queryChart.selectField')"
                        @update:model-value="emit('widget', { field: String($event ?? '') })"/>
            </MfFormItem>
            <template v-if="snapshot && (widget.kind === 'metricGrid' || widget.kind === 'recordDetail' || widget.kind === 'recordCards' || widget.kind === 'rollingTable' || widget.kind === 'statisticList')">
              <MfFormItem name="displayFields" :label="$t('dashboard.studio.displayFields')">
                <MfSelect :model-value="widget.displayFields ?? []" :options="['metricGrid','statisticList'].includes(widget.kind) ? chartNumericFields : chartFields" multiple searchable
                          :placeholder="$t('dashboard.studio.autoFields')" @update:model-value="setDisplayFields"/>
              </MfFormItem>
              <MfFormItem v-if="widget.kind === 'metricGrid'" name="operation" :label="$t('dashboard.studio.dataAggregation')">
                <MfSelect :model-value="widget.operation" :options="aggregations" @update:model-value="changeOperation"/>
              </MfFormItem>
              <MfFormItem v-else-if="widget.kind === 'recordDetail'" name="recordIndex" :label="$t('dashboard.studio.recordIndex')">
                <MfInputNumber :model-value="widget.recordIndex ?? 1" :min="1" :max="Math.max(1, snapshot.columns.length)" :precision="0"
                               decimal-only commit-on-change @update:model-value="value => value !== undefined && emit('widget', { recordIndex: value })"/>
              </MfFormItem>
            </template>
            <template v-if="snapshot && widget.kind === 'dataTimeline'">
              <MfFormItem name="timeField" :label="$t('dashboard.studio.timelineDateField')" required-mark>
                <MfSelect :model-value="widget.timeField || chartDateFields[0]?.value" :options="chartDateFields" searchable
                          @update:model-value="emit('widget', { timeField: String($event ?? '') })"/>
              </MfFormItem>
              <MfFormItem name="field" :label="$t('dashboard.studio.timelineLabelField')">
                <MfSelect :model-value="widget.field" :options="chartFields" searchable :placeholder="$t('dashboard.studio.autoFields')"
                          @update:model-value="emit('widget', { field: String($event ?? '') })"/>
              </MfFormItem>
            </template>
            <p v-if="['progressList', 'dataTags', 'dataTimeline'].includes(widget.kind)" class="text-xs text-content-tertiary">{{ $t('dashboard.studio.dataDisplayLimit') }}</p>
            <MfFormItem :label="$t('dashboard.studio.source')">
              <div class="grid w-full min-w-0 grid-cols-1 gap-1 rounded-mf-md bg-primary/8 p-2.5 text-xs leading-5 text-content-secondary">
                <EllipsisCell v-if="sourceName" :text="sourceName" class="min-w-0" tabindex="0"/>
                <template v-if="snapshot">
                  <strong class="block text-content">{{ $t('queryChart.size', { rows: snapshot.columns.length, fields: snapshot.headers.length }) }}</strong>
                  <p>{{ [snapshot.queryChart.options.category, snapshot.queryChart.options.metric].filter(Boolean).join(' · ') }}</p>
                </template>
                <p v-else-if="!asset">{{ $t('dashboard.studio.chooseAsset') }}</p>
              </div>
            </MfFormItem>
            <MfFormItem v-if="metricWidget" :label="$t('queryChart.metric')">
              <MfSelect :model-value="widget.operation" :options="aggregations" @update:model-value="changeOperation"/>
            </MfFormItem>
            <MfFormItem v-if="metricWidget && widget.operation !== 'count'" :label="$t('queryChart.selectField')">
              <MfSelect :model-value="widget.field" :options="fieldOptions" searchable @update:model-value="emit('widget', { field: String($event ?? '') })"/>
            </MfFormItem>
            <MfFormItem v-if="widget.kind === 'progress' || widget.kind === 'gauge' || ['progressList','radialProgress','thresholdCards'].includes(widget.kind)" :label="$t('dashboard.studio.targetValue')">
              <MfInputNumber :model-value="widget.target" :min="1" :max="1000000000" :precision="0" decimal-only commit-on-change @update:model-value="setTarget"/>
            </MfFormItem>
          </DashboardInspectorGroup>

          <DashboardOperationsSettings v-if="widget && operationData && snapshot" :widget="widget" :fields="chartFields" :dates="chartDateFields" :numbers="chartNumericFields"
                                       @change="emit('widget', $event)"/>
          <DashboardBusinessSettings v-if="widget && businessData && snapshot" :widget="widget" :fields="chartFields" :dates="chartDateFields" :numbers="chartNumericFields"
                                     :rows="snapshot.columns" @change="emit('widget', $event)"/>
          <DashboardRelationSettings v-if="widget && relationData && snapshot" :widget="widget" :fields="chartFields" :dates="chartDateFields" :numbers="chartNumericFields"
                                     @change="emit('widget', $event)"/>
          <DashboardAuditSettings v-if="widget && auditData && snapshot" :widget="widget" :fields="chartFields" @change="emit('widget', $event)"/>
          <DashboardTemporalSettings v-if="widget && temporalData && snapshot" :widget="widget" :fields="chartFields" :dates="chartDateFields"
                                      :numbers="chartNumericFields" @change="emit('widget', $event)"/>
          <DashboardComparativeSettings v-if="widget && comparativeData && snapshot" :widget="widget" :fields="chartFields" :numbers="chartNumericFields"
                                         :rows="snapshot.columns" @change="emit('widget', $event)"/>
          <DashboardPlanningSettings v-if="widget && planningData && snapshot" :widget="widget" :fields="chartFields" :dates="chartDateFields"
                                      @change="emit('widget', $event)"/>
          <DashboardSpatialSettings v-if="widget && spatialData && snapshot" :widget="widget" :fields="chartFields" :dates="chartDateFields"
                                     :numbers="chartNumericFields" @change="emit('widget', $event)"/>

          <DashboardInspectorGroup v-if="widget && advancedData" icon="data" :title="$t('dashboard.studio.dataDisplaySettings')">
            <MfFormItem v-if="['comparisonTable','radialProgress','thresholdCards','sparklineGrid'].includes(widget.kind)" :label="$t('dashboard.studio.dataCategory')">
              <MfSelect :model-value="widget.chartOptions?.category ?? snapshot?.queryChart.options.category" :options="chartFields" searchable @update:model-value="value => changeMappings({ category: String(value ?? '') })"/>
            </MfFormItem>
            <template v-if="widget.kind === 'calendarHeatmap'">
              <MfFormItem name="timeField" :label="$t('dashboard.studio.timelineDateField')" required-mark><MfSelect :model-value="widget.timeField || chartDateFields[0]?.value" :options="chartDateFields" @update:model-value="value => emit('widget', { timeField: String(value ?? '') })"/></MfFormItem>
              <MfFormItem name="dataOptions.year" :label="$t('dashboard.studio.calendarYear')"><MfInputNumber :model-value="dataSettings.year || undefined" :min="1" :max="9999" :precision="0" :placeholder="$t('dashboard.studio.autoLatestYear')" decimal-only commit-on-change @update:model-value="value => updateData({ year: value ?? 0 })"/></MfFormItem>
            </template>
            <MfFormItem v-if="widget.kind === 'dataQuality'" :label="$t('dashboard.studio.monitoring.qualityFields')">
              <MfSelect :model-value="widget.displayFields ?? []" :options="chartFields" multiple searchable clearable :placeholder="$t('dashboard.studio.autoFields')"
                        @update:model-value="value => emit('widget', { displayFields: Array.isArray(value) ? value.map(String).slice(0, 12) : undefined })"/>
            </MfFormItem>
            <MfFormItem v-if="['periodComparison','dataFreshness','sparklineGrid'].includes(widget.kind)" :label="$t('dashboard.studio.timelineDateField')" required-mark>
              <MfSelect :model-value="widget.timeField || chartDateFields[0]?.value" :options="chartDateFields" searchable
                        @update:model-value="value => emit('widget', { timeField: String(value ?? '') })"/>
            </MfFormItem>
            <MfFormItem v-if="widget.kind === 'periodComparison' || widget.kind === 'sparklineGrid'" :label="$t('dashboard.studio.monitoring.periodDays')">
              <MfInputNumber :model-value="dataSettings.periodDays" :min="1" :max="90" :precision="0" decimal-only commit-on-change
                             @update:model-value="value => value !== undefined && updateData({ periodDays: value })"/>
            </MfFormItem>
            <MfFormItem v-if="widget.kind === 'dataFreshness'" :label="$t('dashboard.studio.monitoring.staleHours')">
              <MfInputNumber :model-value="dataSettings.staleHours" :min="1" :max="8760" :precision="0" decimal-only commit-on-change
                             @update:model-value="value => value !== undefined && updateData({ staleHours: value })"/>
            </MfFormItem>
            <template v-if="widget.kind === 'rollingTable'">
              <label class="toggle-row">{{ $t('dashboard.studio.autoplay') }}<MfSwitch :model-value="contentSettings.autoplay" @update:model-value="value => updateContent({ autoplay: value })"/></label>
              <MfFormItem name="contentOptions.interval" :label="$t('dashboard.studio.playInterval')"><MfInputNumber :model-value="contentSettings.interval" :min="2" :max="60" :precision="0" decimal-only commit-on-change @update:model-value="value => value !== undefined && updateContent({ interval: value })"/></MfFormItem>
              <label class="toggle-row">{{ $t('dashboard.studio.pauseOnHover') }}<MfSwitch :model-value="contentSettings.pauseOnHover" @update:model-value="value => updateContent({ pauseOnHover: value })"/></label>
            </template>
            <MfFormItem v-if="['metricComparison','comparisonTable'].includes(widget.kind)" name="dataOptions.baselineField" :label="$t('dashboard.studio.baselineField')" required-mark>
              <MfSelect :model-value="dataSettings.baselineField" :options="chartNumericFields" searchable @update:model-value="value => updateData({ baselineField: String(value ?? '') })"/>
            </MfFormItem>
            <template v-if="widget.kind === 'statusGrid'">
              <MfFormItem name="dataOptions.statusField" :label="$t('dashboard.studio.statusField')"><MfSelect :model-value="dataSettings.statusField" :options="chartFields" searchable @update:model-value="value => updateData({ statusField: String(value ?? '') })"/></MfFormItem>
              <MfFormItem name="field" :label="$t('dashboard.studio.timelineLabelField')"><MfSelect :model-value="widget.field" :options="chartFields" searchable @update:model-value="value => emit('widget', { field: String(value ?? '') })"/></MfFormItem>
              <MfFormItem name="dataOptions.positiveValues" :label="$t('dashboard.studio.positiveValues')"><MfInput :model-value="dataSettings.positiveValues" @update:model-value="value => updateData({ positiveValues: value })"/></MfFormItem>
            </template>
            <MfFormItem v-if="['rankList', 'distributionList', 'statusGrid', 'recordCards', 'rollingTable', 'comparisonTable', 'radialProgress', 'thresholdCards', 'sparklineGrid'].includes(widget.kind)" name="dataOptions.limit" :label="$t('dashboard.studio.displayLimit')">
              <MfInputNumber :model-value="dataSettings.limit" :min="1" :max="widget.kind === 'radialProgress' ? 5 : widget.kind === 'sparklineGrid' ? 8 : 100" :precision="0" decimal-only commit-on-change @update:model-value="value => value !== undefined && updateData({ limit: value })"/>
            </MfFormItem>
            <template v-if="!['statusGrid','recordCards','dataFreshness'].includes(widget.kind)">
              <MfFormItem name="dataOptions.decimals" :label="$t('dashboard.studio.decimalPlaces')"><MfInputNumber :model-value="dataSettings.decimals" :min="0" :max="8" :precision="0" decimal-only commit-on-change @update:model-value="value => value !== undefined && updateData({ decimals: value })"/></MfFormItem>
              <div v-if="widget.kind !== 'dataQuality'" class="grid grid-cols-2 gap-2"><MfFormItem name="dataOptions.prefix" :label="$t('dashboard.studio.numberPrefix')"><MfInput :model-value="dataSettings.prefix" :maxlength="30" @update:model-value="value => updateData({ prefix: value })"/></MfFormItem><MfFormItem name="dataOptions.suffix" :label="$t('dashboard.studio.numberSuffix')"><MfInput :model-value="dataSettings.suffix" :maxlength="30" @update:model-value="value => updateData({ suffix: value })"/></MfFormItem></div>
            </template>
          </DashboardInspectorGroup>

          <DashboardInspectorGroup v-if="!widget" icon="canvas" :title="$t('dashboard.studio.canvasSettings')">
            <MfFormItem v-if="wall" :label="$t('dashboard.studio.resolution')">
              <MfSelect :model-value="board.resolutionMode === 'auto' ? 'auto' : board.width + 'x' + board.height" :options="resolutions" @update:model-value="changeResolution"/>
            </MfFormItem>
            <MfFormItem v-if="wall && board.resolutionMode === 'fixed'" :label="$t('dashboard.studio.fit')">
              <MfSelect :model-value="board.fit" :options="choices('fitMode', ['contain', 'cover'])" @update:model-value="changeFit"/>
            </MfFormItem>
            <label class="toggle-row">{{ $t('dashboard.studio.canvasTitle') }}<MfSwitch :model-value="board.showTitle" @update:model-value="emit('board', { showTitle: $event })"/></label>
          </DashboardInspectorGroup>

          <DashboardInspectorGroup v-if="!widget" icon="layout" :title="$t('dashboard.studio.canvasGridSettings')">
            <label class="toggle-row">{{ $t('dashboard.studio.grid') }}<MfSwitch :model-value="board.grid" @update:model-value="emit('board', { grid: $event })"/></label>
            <label class="toggle-row">{{ $t('dashboard.studio.snap') }}<MfSwitch :model-value="board.snap" @update:model-value="emit('board', { snap: $event })"/></label>
            <p class="text-xs text-content-tertiary">{{ $t(wall ? 'dashboard.studio.wallSnapHint' : 'dashboard.studio.dashboardSnapHint') }}</p>
            <MfFormItem v-if="wall" name="snapStep" :label="$t('dashboard.studio.snapStep')">
              <MfInputNumber :model-value="board.snapStep" :min="dashboardCanvasNumbers.snapStep.min" :max="dashboardCanvasNumbers.snapStep.max"
                             :aria-label="$t('dashboard.studio.snapStep')" :precision="0" decimal-only commit-on-change
                             @update:model-value="updateCanvasNumber('snapStep', $event)"/>
            </MfFormItem>
            <div v-else class="grid grid-cols-2 gap-2">
              <MfFormItem v-for="key in dashboardSpacing" :key="key" :name="key" :label="$t('dashboard.studio.canvasSpacing.' + key)">
                <MfInputNumber :model-value="board[key]" :min="dashboardCanvasNumbers[key].min" :max="dashboardCanvasNumbers[key].max"
                               :aria-label="$t('dashboard.studio.canvasSpacing.' + key)" :precision="0" decimal-only commit-on-change
                               @update:model-value="updateCanvasNumber(key, $event)"/>
              </MfFormItem>
            </div>
          </DashboardInspectorGroup>

          <DashboardInspectorGroup v-if="!widget" icon="appearance" :title="$t('dashboard.studio.canvasAppearance')">
            <MfFormItem v-if="wall" name="theme" :label="$t('dashboard.studio.theme')">
              <MfSelect :model-value="board.theme" :options="choices('wallTheme', ['ocean', 'jade'])" @update:model-value="changeTheme"/>
            </MfFormItem>
            <MfFormItem name="background" :label="$t('dashboard.studio.canvasBackground')">
              <MfInput :model-value="board.background" placeholder="#RRGGBB" :maxlength="7" clearable
                       :invalid="!!board.background && !dashboardCanvasColor(board.background)" @update:model-value="emit('board', { background: $event })"/>
            </MfFormItem>
            <p class="text-xs text-content-tertiary">{{ $t('dashboard.studio.canvasBackgroundHint') }}</p>
            <MfForm ref="backgroundForm" :model="{ backgroundImage: board.backgroundImage }" @focusout="validateBackground">
              <MfFormItem name="backgroundImage" :label="$t('dashboard.studio.backgroundImage')" :rules="[{ validator: value => !value || !!dashboardResourceUrl(String(value)), message: $t('dashboard.studio.linkUrlRequired') }]">
                <MfInput :model-value="board.backgroundImage" placeholder="https://" clearable
                         @update:model-value="emit('board', { backgroundImage: $event })"/>
              </MfFormItem>
            </MfForm>
            <MfUpload :action="backgroundUploadAction" :headers="backgroundUploadHeaders()" :data="{ mode: 'DASHBOARD' }"
                      accept="image/png,image/jpeg,image/webp,image/gif" @change="backgroundUploaded">{{ $t('dashboard.studio.uploadBackground') }}</MfUpload>
            <MfAlert v-if="backgroundUploadError" variant="danger" :title="backgroundUploadError"/>
            <template v-if="board.backgroundImage">
              <MfFormItem name="backgroundFit" :label="$t('dashboard.studio.backgroundFitLabel')"><MfSelect :model-value="board.backgroundFit" :options="choices('backgroundFit', ['cover','contain','stretch','tile'])" @update:model-value="setBackgroundFit"/></MfFormItem>
              <MfFormItem name="backgroundPosition" :label="$t('dashboard.studio.backgroundPositionLabel')"><MfSelect :model-value="board.backgroundPosition" :options="choices('backgroundPosition', ['center','top','bottom'])" @update:model-value="setBackgroundPosition"/></MfFormItem>
              <div class="grid grid-cols-2 gap-2">
                <MfFormItem name="backgroundOpacity" :label="$t('dashboard.studio.backgroundOpacity')"><MfInputNumber :model-value="board.backgroundOpacity" :min="0" :max="100" :precision="0" decimal-only commit-on-change @update:model-value="value => value !== undefined && emit('board', { backgroundOpacity: value })"/></MfFormItem>
                <MfFormItem name="backgroundBlur" :label="$t('dashboard.studio.backgroundBlur')"><MfInputNumber :model-value="board.backgroundBlur" :min="0" :max="20" :precision="0" decimal-only commit-on-change @update:model-value="value => value !== undefined && emit('board', { backgroundBlur: value })"/></MfFormItem>
              </div>
              <MfButton size="sm" @click="emit('board', { backgroundImage: '' })">{{ $t('dashboard.studio.clearBackgroundImage') }}</MfButton>
            </template>
          </DashboardInspectorGroup>

          <DashboardInspectorGroup v-if="!widget || usesAsset && !filterWidget" icon="refresh" :title="$t('dashboard.studio.dataRefresh')">
            <MfFormItem :label="$t('dashboard.studio.refreshPolicy')">
              <MfSelect :model-value="refreshConfig.refresh" :options="refreshOptions" :disabled="!canConfigureRefresh"
                        @update:model-value="updateRefresh($event, !!widget)"/>
            </MfFormItem>
            <MfFormItem :label="$t('dashboard.studio.refreshIntervalMinutes')">
              <MfInputNumber :model-value="(refreshConfig.refreshSeconds ?? 300) / 60" :min="1" :max="1440" :precision="0"
                             decimal-only commit-on-change :disabled="!canConfigureRefresh || refreshConfig.refresh !== 'custom'" @update:model-value="setRefreshMinutes"/>
            </MfFormItem>
            <p v-if="widget && !canConfigureRefresh" class="rounded-mf-md bg-primary/8 p-2 text-xs text-content-secondary">
              {{ $t('dashboard.studio.refreshNeedsLiveAsset') }}
            </p>
          </DashboardInspectorGroup>

          <DashboardInspectorGroup v-if="widget && configurableContent" :title="$t('dashboard.studio.widgetContent')">
            <MfFormItem v-if="textContent" :label="$t(widget.kind === 'copyText' ? 'dashboard.studio.copyTextPrompt' : 'dashboard.studio.tool.text')">
              <MfTextarea :model-value="widget.text" :rows="widget.kind === 'codeBlock' || widget.kind === 'markdown' ? 7 : 4" :maxlength="widget.kind === 'codeBlock' || widget.kind === 'markdown' ? 20000 : 2000" @update:model-value="emit('widget', { text: $event })"/>
            </MfFormItem>
            <MfFormItem v-else-if="widget.kind === 'image'" :label="$t('dashboard.studio.imageUrl')">
              <MfInput :model-value="widget.image" placeholder="https://" @update:model-value="emit('widget', { image: $event })"/>
            </MfFormItem>
            <template v-else-if="['link', 'video', 'audio', 'embed', 'actionButton'].includes(widget.kind)">
              <MfFormItem :label="$t(widget.kind === 'video' ? 'dashboard.studio.videoUrl' : widget.kind === 'audio' ? 'dashboard.studio.audioUrl' : 'dashboard.studio.linkUrl')">
                <MfInput :model-value="widget.url" placeholder="https://" @update:model-value="emit('widget', { url: $event })"/>
              </MfFormItem>
              <MfFormItem v-if="widget.kind === 'link'" :label="$t('dashboard.studio.linkText')">
                <MfInput :model-value="widget.text" :placeholder="widget.name" :maxlength="100" @update:model-value="emit('widget', { text: $event })"/>
              </MfFormItem>
            </template>
            <label v-else-if="widget.kind === 'clock'" class="toggle-row">
              {{ $t('dashboard.studio.showClockDate') }}<MfSwitch :model-value="widget.clockDate" @update:model-value="emit('widget', { clockDate: $event })"/>
            </label>
            <MfFormItem v-else-if="widget.kind === 'shape'" :label="$t('dashboard.studio.shape')">
              <MfSelect :model-value="widget.shape" :options="choices('shapeType', ['rectangle', 'circle'])" @update:model-value="changeShape"/>
            </MfFormItem>
            <DashboardLinkEditor v-else-if="['linkList','iconLinks'].includes(widget.kind)" :mode="widget.kind === 'iconLinks' ? 'icons' : 'links'" :model-value="widget.links ?? []" @update:model-value="emit('widget', { links: $event })"/>
            <DashboardLinkEditor v-else-if="widget.kind === 'statusLegend'" mode="legend" :model-value="widget.links ?? []" @update:model-value="emit('widget', { links: $event })"/>
            <DashboardLinkEditor v-else-if="widget.kind === 'definitionList'" mode="entries" :model-value="widget.links ?? []" @update:model-value="emit('widget', { links: $event })"/>
            <template v-else-if="widget.kind === 'inspectionChecklist'">
              <DashboardLinkEditor mode="entries" :model-value="widget.links ?? []" @update:model-value="emit('widget', { links: $event })"/>
              <p class="text-xs text-content-tertiary">{{ $t('dashboard.studio.ops.checklistHint') }}</p>
            </template>
            <DashboardLinkEditor v-else-if="['imageCarousel','imageGallery','announcement','tabsContent','accordion','stepGuide','imageCompare'].includes(widget.kind)" :model-value="widget.links ?? []"
                                 :max-items="widget.kind === 'imageCompare' ? 2 : 20" :mode="['imageCarousel','imageGallery','imageCompare'].includes(widget.kind) ? 'gallery' : 'entries'" @update:model-value="emit('widget', { links: $event })"/>
            <template v-else-if="widget.kind === 'countdown'">
              <MfFormItem name="deadline.date" :label="$t('dashboard.studio.deadlineDate')">
                <MfDatePicker :model-value="widget.deadline?.date ?? ''" :locale="locale" :label="$t('dashboard.studio.deadlineDate')"
                              :today-text="$t('dashboard.studio.today')" :clear-text="$t('common.clear')"
                              @update:model-value="setDeadline({ date: $event })"/>
              </MfFormItem>
              <div class="grid grid-cols-2 gap-2">
                <MfFormItem name="deadline.hour" :label="$t('dashboard.studio.duration.hours')">
                  <MfInputNumber :model-value="widget.deadline?.hour ?? 0" :min="0" :max="23" :precision="0" decimal-only commit-on-change
                                 @update:model-value="value => value !== undefined && setDeadline({ hour: value })"/>
                </MfFormItem>
                <MfFormItem name="deadline.minute" :label="$t('dashboard.studio.duration.minutes')">
                  <MfInputNumber :model-value="widget.deadline?.minute ?? 0" :min="0" :max="59" :precision="0" decimal-only commit-on-change
                                 @update:model-value="value => value !== undefined && setDeadline({ minute: value })"/>
                </MfFormItem>
              </div>
              <p class="text-xs text-content-tertiary">{{ $t('dashboard.studio.deadlineTimezone') }}</p>
            </template>
            <MfFormItem v-else-if="widget.kind === 'icon'" name="iconName" :label="$t('dashboard.studio.iconName')">
              <MfSelect :model-value="widget.iconName || 'Layers'" :options="iconOptions" searchable @update:model-value="emit('widget', { iconName: String($event ?? 'Layers') })"/>
            </MfFormItem>
            <template v-if="widget.kind === 'imageText'">
              <MfFormItem name="image" :label="$t('dashboard.studio.imageUrl')"><MfInput :model-value="widget.image" placeholder="https://" @update:model-value="emit('widget', { image: $event })"/></MfFormItem>
              <MfFormItem name="url" :label="$t('dashboard.studio.linkUrl')"><MfInput :model-value="widget.url" placeholder="https://" @update:model-value="emit('widget', { url: $event })"/></MfFormItem>
            </template>
            <p v-if="widget.kind === 'bulletList'" class="text-xs text-content-tertiary">{{ $t('dashboard.studio.listLineHint') }}</p>
            <MfFormItem v-if="widget.kind === 'codeBlock'" name="contentOptions.language" :label="$t('dashboard.studio.codeLanguage')"><MfSelect :model-value="contentSettings.language" :options="['text','sql','json'].map(value => ({ value, label: value.toUpperCase() }))" @update:model-value="value => updateContent({ language: value as 'text' | 'sql' | 'json' })"/></MfFormItem>
            <p v-if="widget.kind === 'markdown'" class="text-xs text-content-tertiary">{{ $t('dashboard.studio.markdownHint') }}</p>
            <template v-if="widget.kind === 'worldClock'">
              <MfFormItem name="contentOptions.timezone" :label="$t('dashboard.studio.timezone')"><MfSelect :model-value="contentSettings.timezone" :options="timezoneOptions" searchable @update:model-value="value => updateContent({ timezone: String(value) })"/></MfFormItem>
              <label class="toggle-row">{{ $t('dashboard.studio.showClockDate') }}<MfSwitch :model-value="widget.clockDate" @update:model-value="value => emit('widget', { clockDate: value })"/></label>
            </template>
            <MfFormItem v-if="widget.kind === 'imageCompare'" name="contentOptions.comparePosition" :label="$t('dashboard.studio.comparePosition')"><MfInputNumber :model-value="contentSettings.comparePosition" :min="0" :max="100" :precision="0" decimal-only commit-on-change @update:model-value="value => value !== undefined && updateContent({ comparePosition: value })"/></MfFormItem>
            <p v-if="widget.kind === 'imageCompare'" class="text-xs text-content-tertiary">{{ $t('dashboard.studio.compareImagesNeeded') }}</p>
            <template v-if="widget.kind === 'stepGuide'">
              <MfFormItem name="contentOptions.activeIndex" :label="$t('dashboard.studio.activeStep')"><MfInputNumber :model-value="contentSettings.activeIndex + 1" :min="1" :max="Math.max(1, widget.links?.length || 1)" :precision="0" decimal-only commit-on-change @update:model-value="value => value !== undefined && updateContent({ activeIndex: value - 1 })"/></MfFormItem>
              <MfFormItem name="contentOptions.direction" :label="$t('dashboard.studio.contentDirection')"><MfSelect :model-value="contentSettings.direction" :options="[{ value:'horizontal',label:$t('dashboard.studio.horizontal') },{ value:'vertical',label:$t('dashboard.studio.vertical') }]" @update:model-value="value => updateContent({ direction: value as 'horizontal' | 'vertical' })"/></MfFormItem>
            </template>
            <MfFormItem v-if="widget.kind === 'iconLinks'" name="layoutOptions.columns" :label="$t('dashboard.studio.layoutField.columns')"><MfInputNumber :model-value="layout.columns" :min="1" :max="6" :precision="0" decimal-only commit-on-change @update:model-value="value => value !== undefined && updateLayout({ columns: value })"/></MfFormItem>
            <MfFormItem v-if="widget.kind === 'actionButton'" name="url" :label="$t('dashboard.studio.linkUrl')"><MfInput :model-value="widget.url" placeholder="https://" @update:model-value="emit('widget', { url: $event })"/></MfFormItem>
            <MfFormItem v-if="widget.kind === 'quote'" name="contentOptions.attribution" :label="$t('dashboard.studio.quoteAttribution')"><MfInput :model-value="contentSettings.attribution" @update:model-value="value => updateContent({ attribution: value })"/></MfFormItem>
            <p v-if="widget.kind === 'embed'" class="text-xs text-content-tertiary">{{ $t('dashboard.studio.embedHint') }}</p>
            <template v-if="['imageCarousel','announcement','textTicker'].includes(widget.kind)">
              <label class="toggle-row">{{ $t('dashboard.studio.autoplay') }}<MfSwitch :model-value="contentSettings.autoplay" @update:model-value="value => updateContent({ autoplay: value })"/></label>
              <MfFormItem name="contentOptions.interval" :label="$t(widget.kind === 'textTicker' ? 'dashboard.studio.scrollDuration' : 'dashboard.studio.playInterval')"><MfInputNumber :model-value="contentSettings.interval" :min="2" :max="60" :precision="0" decimal-only commit-on-change @update:model-value="value => value !== undefined && updateContent({ interval: value })"/></MfFormItem>
              <label class="toggle-row">{{ $t('dashboard.studio.pauseOnHover') }}<MfSwitch :model-value="contentSettings.pauseOnHover" @update:model-value="value => updateContent({ pauseOnHover: value })"/></label>
            </template>
            <MfFormItem v-if="['imageCarousel','imageGallery'].includes(widget.kind)" name="contentOptions.fit" :label="$t('dashboard.studio.imageFitLabel')"><MfSelect :model-value="contentSettings.fit" :options="choices('imageFit', ['cover','contain'])" @update:model-value="setImageFit"/></MfFormItem>
            <MfFormItem v-if="widget.kind === 'text' || widget.kind === 'heading'" :label="$t('dashboard.studio.alignment')">
              <MfSelect :model-value="widget.alignment" :options="choices('align', ['left', 'center', 'right'])" @update:model-value="changeAlignment"/>
            </MfFormItem>
            <MfFormItem v-if="widget.kind === 'note' || widget.kind === 'badge'" :label="$t('dashboard.studio.tone')">
              <MfSelect :model-value="widget.tone" :options="choices('toneOption', ['info', 'success', 'warning', 'danger'])" @update:model-value="changeTone"/>
            </MfFormItem>
          </DashboardInspectorGroup>

          <DashboardInspectorGroup v-if="widget && !effectSettings && !widget.parentId" icon="layout" :title="$t('dashboard.studio.dimensions')">
            <div class="grid grid-cols-2 gap-3">
              <MfFormItem v-for="key in dimensions" :key="key" :label="$t('dashboard.studio.rect.' + key)">
                <MfInputNumber :key="widget.id + key" :model-value="widget[key]" :min="key === 'x' || key === 'y' ? 0 : 1" :max="limit(key)"
                               :aria-label="$t('dashboard.studio.rect.' + key)" :precision="0" decimal-only commit-on-change @update:model-value="setRect(key, $event)"/>
              </MfFormItem>
            </div>
            <label class="toggle-row">{{ $t('dashboard.studio.movable') }}<MfSwitch :model-value="widget.movable" @update:model-value="emit('widget', { movable: $event })"/></label>
            <label class="toggle-row">{{ $t('dashboard.studio.resizable') }}<MfSwitch :model-value="widget.resizable" @update:model-value="emit('widget', { resizable: $event })"/></label>
          </DashboardInspectorGroup>

          <DashboardInspectorGroup v-if="widget" icon="actions" :default-open="false" :title="$t('dashboard.studio.widgetActions')">
            <MfButton variant="danger" size="sm" @click="emit('remove')"><Trash2 :size="14"/>{{ $t('common.delete') }}</MfButton>
          </DashboardInspectorGroup>
        </div>
      </MfTabPane>

      <MfTabPane v-if="widget && !effectSettings" item-key="style" :tab="$t('dashboard.studio.style')">
        <template #tab><span class="flex items-center justify-center gap-1.5"><Palette :size="15"/>{{ $t('dashboard.studio.style') }}</span></template>
        <div v-if="widget" class="inspector-section">
          <DashboardInspectorGroup icon="appearance" :title="$t('dashboard.studio.cardAppearance')">
            <label class="toggle-row">{{ $t('dashboard.studio.showCard') }}<MfSwitch :model-value="widget.showCard" @update:model-value="emit('widget', { showCard: $event })"/></label>
            <MfFormItem v-if="widget.showCard" name="background" :label="$t('dashboard.studio.background')">
              <MfInput :model-value="widget.background" placeholder="#FFFFFF" :maxlength="7" @update:model-value="emit('widget', { background: $event })"/>
            </MfFormItem>
            <label v-if="widget.showCard" class="toggle-row">{{ $t('dashboard.studio.border') }}<MfSwitch :model-value="widget.border" @update:model-value="emit('widget', { border: $event })"/></label>
          </DashboardInspectorGroup>

          <DashboardInspectorGroup v-if="coloredContent" icon="appearance" :title="$t('dashboard.studio.contentAppearance')">
            <MfFormItem name="color" :label="$t('dashboard.studio.accentColor')"><MfInput :model-value="widget.color" placeholder="#079B70" :maxlength="7" @update:model-value="emit('widget', { color: $event })"/></MfFormItem>
            <MfFormItem v-if="widget.kind === 'gradientPanel'" name="secondaryColor" :label="$t('dashboard.studio.secondaryColor')"><MfInput :model-value="widget.secondaryColor" placeholder="#15E5D1" :maxlength="7" @update:model-value="emit('widget', { secondaryColor: $event })"/></MfFormItem>
            <MfFormItem v-if="widget.kind === 'icon'" name="iconSize" :label="$t('dashboard.studio.iconSize')">
              <MfInputNumber :model-value="widget.iconSize ?? 64" :min="16" :max="256" :precision="0" decimal-only commit-on-change
                             @update:model-value="value => value !== undefined && emit('widget', { iconSize: value })"/>
            </MfFormItem>
            <MfFormItem v-if="advancedDecoration || ['borderFrame', 'horizontalArrow', 'verticalArrow', 'icon', 'glowLine', 'glowRing', 'ripple', 'cornerAccent'].includes(widget.kind)" name="strokeWidth" :label="$t('dashboard.studio.strokeWidth')">
              <MfInputNumber :model-value="widget.strokeWidth ?? 2" :min="1" :max="12" :precision="0" decimal-only commit-on-change
                             @update:model-value="value => value !== undefined && emit('widget', { strokeWidth: value })"/>
            </MfFormItem>
            <template v-if="['cornerAccent','cornerFrame'].includes(widget.kind)">
              <MfFormItem v-if="widget.kind === 'cornerAccent'" name="cornerPosition" :label="$t('dashboard.studio.cornerPosition')">
                <MfSelect :model-value="widget.cornerPosition ?? 'topLeft'" :options="cornerPositions" @update:model-value="setCornerPosition"/>
              </MfFormItem>
              <MfFormItem name="cornerSize" :label="$t('dashboard.studio.cornerSize')">
                <MfInputNumber :model-value="widget.cornerSize ?? 64" :min="16" :max="192" :precision="0" decimal-only commit-on-change
                               @update:model-value="value => value !== undefined && emit('widget', { cornerSize: value })"/>
              </MfFormItem>
            </template>
            <MfFormItem v-if="widget.kind === 'gridPanel' || widget.kind === 'dotMatrix'" name="gridSize" :label="$t('dashboard.studio.decorationGridSize')">
              <MfInputNumber :model-value="widget.gridSize ?? 24" :min="8" :max="96" :precision="0" decimal-only commit-on-change
                             @update:model-value="value => value !== undefined && emit('widget', { gridSize: value })"/>
            </MfFormItem>
          </DashboardInspectorGroup>
          <DashboardInspectorGroup v-if="advancedContent && ['quote','textTicker','announcement','tabsContent','accordion','tagCloud','markdown','codeBlock','worldClock','imageCompare','iconLinks','stepGuide'].includes(widget.kind)" icon="appearance" :title="$t('dashboard.studio.typography')">
            <MfFormItem name="contentOptions.fontSize" :label="$t('dashboard.studio.contentFontSize')"><MfInputNumber :model-value="contentSettings.fontSize" :min="10" :max="128" :precision="0" decimal-only commit-on-change @update:model-value="value => value !== undefined && updateContent({ fontSize: value })"/></MfFormItem>
            <MfFormItem name="contentOptions.lineHeight" :label="$t('dashboard.studio.lineHeight')"><MfInputNumber :model-value="contentSettings.lineHeight" :min="1" :max="3" :step=".1" :precision="1" decimal-only commit-on-change @update:model-value="value => value !== undefined && updateContent({ lineHeight: value })"/></MfFormItem>
            <MfFormItem name="contentOptions.fontWeight" :label="$t('dashboard.studio.fontWeightLabel')"><MfSelect :model-value="contentSettings.fontWeight" :options="choices('fontWeight', ['normal','medium','bold'])" @update:model-value="setFontWeight"/></MfFormItem>
          </DashboardInspectorGroup>
          <DashboardInspectorGroup v-if="advancedDecoration" icon="appearance" :title="$t('dashboard.studio.decorationSettings')">
            <MfFormItem v-for="field in decorationNumbers" :key="field.key" :name="'decorationOptions.' + field.key" :label="$t('dashboard.studio.decorationField.' + field.key)"><MfInputNumber :model-value="decorationSettings[field.key]" :min="field.min" :max="field.max" :precision="0" decimal-only commit-on-change @update:model-value="value => value !== undefined && updateDecoration({ [field.key]: value })"/></MfFormItem>
            <label v-if="['titlePlate','badgeSeal','hexagon','sectionBanner'].includes(widget.kind)" class="toggle-row">{{ $t('dashboard.studio.fillDecoration') }}<MfSwitch :model-value="decorationSettings.filled" @update:model-value="value => updateDecoration({ filled: value })"/></label>
          </DashboardInspectorGroup>

          <template v-if="widget.showCard">
            <DashboardInspectorGroup v-for="group in insetGroups" :key="group.key" icon="appearance" :title="$t('dashboard.studio.' + group.label)">
              <div class="grid grid-cols-2 gap-3">
                <MfFormItem v-for="(part, index) in group.parts" :key="part" :label="$t('dashboard.studio.inset.' + part)">
                  <MfInputNumber :model-value="widget[group.key][index]" :min="0" :max="96" :precision="0"
                                 :aria-label="$t('dashboard.studio.inset.' + part)" decimal-only commit-on-change @update:model-value="updateInset(group.key, index, $event)"/>
                </MfFormItem>
              </div>
            </DashboardInspectorGroup>
          </template>

          <DashboardInspectorGroup icon="appearance" :title="$t('dashboard.studio.widgetTitle')">
            <MfFormItem :label="$t('dashboard.studio.titleSize')">
              <MfInputNumber :model-value="widget.titleSize" :min="10" :max="30" :precision="0" decimal-only commit-on-change @update:model-value="updateTitleSize($event)"/>
            </MfFormItem>
            <label class="toggle-row">{{ $t('dashboard.studio.showTitle') }}<MfSwitch :model-value="widget.showTitle" @update:model-value="emit('widget', { showTitle: $event })"/></label>
          </DashboardInspectorGroup>
        </div>
      </MfTabPane>

      <MfTabPane v-if="widget" item-key="animation" :tab="$t('dashboard.studio.animationTab')">
        <template #tab><span class="flex items-center justify-center gap-1.5"><Sparkles :size="15"/>{{ $t('dashboard.studio.animationTab') }}</span></template>
        <div class="inspector-section">
          <DashboardInspectorGroup v-if="widget && ['radarSweep','scanFrame','bezierConnector'].includes(widget.kind)" icon="appearance" :title="$t('dashboard.studio.internalMotion')"><label class="toggle-row">{{ $t('dashboard.studio.autoplay') }}<MfSwitch :model-value="decorationSettings.animate" @update:model-value="value => updateDecoration({ animate: value })"/></label></DashboardInspectorGroup>
          <DashboardInspectorGroup icon="appearance" :title="$t('dashboard.studio.widgetAnimation')">
            <MfFormItem name="animation.effect" :label="$t('dashboard.studio.animationType')">
              <MfSelect :model-value="animation.effect" :options="animationOptions" @update:model-value="setAnimationEffect"/>
            </MfFormItem>
            <template v-if="animation.effect !== 'none'">
              <div class="grid grid-cols-2 gap-2">
                <MfFormItem v-for="key in animationNumbers" :key="key" :name="'animation.' + key" :label="$t('dashboard.studio.animation.' + key)">
                  <MfInputNumber :model-value="animation[key]" :min="key === 'duration' ? .1 : 0" :max="60" :step=".1" :precision="1" decimal-only commit-on-change
                                 @update:model-value="value => value !== undefined && updateAnimation({ [key]: value })"/>
                </MfFormItem>
              </div>
              <MfFormItem name="animation.trigger" :label="$t('dashboard.studio.animationTriggerLabel')">
                <MfSelect :model-value="animation.trigger" :options="choices('animationTrigger', ['load', 'hover'])" @update:model-value="setAnimationTrigger"/>
              </MfFormItem>
              <MfFormItem name="animation.easing" :label="$t('dashboard.studio.animationEasingLabel')">
                <MfSelect :model-value="animation.easing" :options="choices('animationEasing', ['linear', 'ease-in', 'ease-out', 'ease-in-out'])" @update:model-value="setAnimationEasing"/>
              </MfFormItem>
              <label class="toggle-row">{{ $t('dashboard.studio.animationRepeat') }}<MfSwitch :model-value="animation.repeat" @update:model-value="updateAnimation({ repeat: $event })"/></label>
              <MfButton size="sm" @click="emit('preview')"><Play :size="14"/>{{ $t('dashboard.studio.previewAnimation') }}</MfButton>
            </template>
            <p class="text-xs text-content-tertiary">{{ $t('dashboard.studio.animationHint') }}</p>
          </DashboardInspectorGroup>

        </div>
      </MfTabPane>

      <MfTabPane v-if="widget && !effectSettings" item-key="interaction" :tab="$t('dashboard.studio.interaction')">
        <template #tab><span class="flex items-center justify-center gap-1.5"><MousePointer2 :size="15"/>{{ $t('dashboard.studio.interaction') }}</span></template>
        <div v-if="widget" class="inspector-section">
          <DashboardInspectorGroup v-if="usesAsset && !metricWidget && !filterWidget && widget.kind !== 'table' && !dashboardDataTool(widget.kind)" icon="interaction" :title="$t('dashboard.studio.chartInteraction')">
            <MfFormItem :label="$t('queryChart.pointAction')">
              <MfSelect :model-value="widget.action" :disabled="!snapshot" :options="choices('pointAction', ['saved', 'details', 'filter', 'none'])"
                        @update:model-value="changeAction"/>
            </MfFormItem>
            <p class="text-xs text-content-tertiary">{{ $t('dashboard.studio.linkHint') }}</p>
          </DashboardInspectorGroup>

          <DashboardInspectorGroup icon="interaction" :title="$t('dashboard.studio.viewOptions')">
            <label class="toggle-row">{{ $t('dashboard.studio.expandWidget') }}<MfSwitch :model-value="widget.expandable" @update:model-value="emit('widget', { expandable: $event })"/></label>
          </DashboardInspectorGroup>
        </div>
      </MfTabPane>
    </MfTabs>
  </aside>
</template>

<script setup lang="ts">
import { computed, nextTick, onScopeDispose, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Database, MousePointer2, Palette, Play, Sparkles, Trash2 } from '@lucide/vue'
import type { ReportModel } from '@/model/report'
import { chartDataSpec } from '@/model/query-chart-data'
import QueryChartDataFields from '@/views/components/visual/query/QueryChartDataFields.vue'
import QueryChartTypeIcon from '@/views/components/visual/query/QueryChartTypeIcon.vue'
import { chartFieldKind, queryChartTypes, type QueryChartType } from '@/model/query-chart'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import DashboardInspectorGroup from './DashboardInspectorGroup.vue'
import DashboardOperationsSettings from './DashboardOperationsSettings.vue'
import DashboardBusinessSettings from './DashboardBusinessSettings.vue'
import DashboardRelationSettings from './DashboardRelationSettings.vue'
import DashboardAuditSettings from './DashboardAuditSettings.vue'
import DashboardTemporalSettings from './DashboardTemporalSettings.vue'
import DashboardComparativeSettings from './DashboardComparativeSettings.vue'
import DashboardPlanningSettings from './DashboardPlanningSettings.vue'
import DashboardSpatialSettings from './DashboardSpatialSettings.vue'
import DashboardPanelHeader from './DashboardPanelHeader.vue'
import DashboardLinkEditor from './DashboardLinkEditor.vue'
import { MENU_ICONS } from '@/config/menu-icons'
import { apiBaseUrl } from '@/utils/http'
import { TokenUtils } from '@/utils/token'
import { dashboardAdvancedContentTools, dashboardAdvancedDataTools, dashboardAdvancedDecorationTools, dashboardContentSettings, dashboardDataSettings,
  dashboardDecorationSettings, dashboardScreenEffectSizeLimit, type DashboardContentOptions, type DashboardDataOptions, type DashboardDecorationOptions } from '@/model/dashboard-layout'
import { dashboardAuditSettings, dashboardAuditTool, dashboardBusinessSettings, dashboardBusinessTool, dashboardComparativeSettings, dashboardComparativeTool, dashboardFilterTool, dashboardOperationDataTool, dashboardOperationsSettings, dashboardPlanningSettings, dashboardPlanningTool, dashboardRelationSettings, dashboardRelationTool, dashboardResourceUrl, dashboardSpatialSettings, dashboardSpatialTool, dashboardTemporalSettings, dashboardTemporalTool, type DashboardMode } from '@/model/dashboard-layout'
import { dashboardBooleanValue, dashboardValueMissing } from '@/model/dashboard-filters'
import { dashboardAnimationDefaults, dashboardAnimationEffects, dashboardCanContain, dashboardCanvasColor, dashboardCanvasNumber, dashboardCanvasNumbers, dashboardChartMappings, dashboardContainerTool, dashboardDataAggregationTool, dashboardDataTool, dashboardDecorationTools, dashboardLayoutSettings, dashboardMetricTool, dashboardPaddingEdges, dashboardRadiusCorners, dashboardScreenEffectPresets, dashboardScreenEffectQuantityLimit, dashboardScreenEffectSettings, dashboardScreenEffectTool, dashboardSnapshot, dashboardTools, dashboardWallPresets, dashboardWallResolution, dashboardWidgetLabelKey, dashboardWidgetUsesAsset, type DashboardAction, type DashboardAnimation, type DashboardBoard, type DashboardChartOverrides, type DashboardDeadline, type DashboardDesign, type DashboardInsets, type DashboardLayoutOptions, type DashboardRect, type DashboardRefresh, type DashboardScreenEffectOptions, type DashboardWidget } from '@/model/dashboard-layout'

const props = defineProps<{ design: DashboardDesign; board: DashboardBoard; widget?: DashboardWidget; groupRect?: DashboardRect;
  groupCount: number; groupMovable: boolean; groupResizable: boolean; reports: ReportModel[]; pinned: boolean; pinnable: boolean }>()
const emit = defineEmits<{
  widget: [change: Partial<DashboardWidget>]; board: [change: Partial<DashboardBoard>]
  page: [change: Partial<Pick<DashboardDesign, 'name' | 'description'>>]; group: [change: Partial<DashboardRect>];
  ungroup: []; remove: []; save: []; preview: []; pin: []; close: []; children: [ids: string[]]; 'add-child': [kind: string]; 'select-child': [id: string]; detach: [];
  'background-image': [mode: DashboardMode, url: string]
}>()
const { t, locale } = useI18n()
const tab = ref<string | number>('data')
const pageInfo = ref<InstanceType<typeof DashboardInspectorGroup>>()
const settingsForm = ref<{ validate: () => Promise<unknown> }>()
watch(() => props.widget?.id, () => { tab.value = 'data' })
const wall = computed(() => props.design.mode === 'wall')
const backgroundUploadAction = apiBaseUrl() + '/api/v1/upload'
const backgroundUploadError = ref('')
const backgroundForm = ref<{ validate: () => Promise<unknown> }>()
async function validateBackground() {
  await nextTick()
  try { await backgroundForm.value?.validate() }
  catch { /* Form fields display validation errors. */ }
}
const latestBackgroundUploads = new Map<DashboardMode, string>()
let active = true
const backgroundUploadModes = new Map<string, DashboardMode>()
onScopeDispose(() => { active = false })
function backgroundUploadHeaders(): Record<string, string> {
  const auth = TokenUtils.getAuthUser()
  return auth ? { Authorization: auth.type + ' ' + auth.token } : {}
}
function backgroundUploaded(info: { file: { uid: string; status?: string; response?: unknown } }) {
  if (!active) return
  const file = info.file
  if (file.status === 'uploading') {
    if (!backgroundUploadModes.has(file.uid)) backgroundUploadModes.set(file.uid, props.design.mode)
    latestBackgroundUploads.set(backgroundUploadModes.get(file.uid)!, file.uid)
    backgroundUploadError.value = ''
    return
  }
  const mode = backgroundUploadModes.get(file.uid)
  backgroundUploadModes.delete(file.uid)
  if (!mode || file.uid !== latestBackgroundUploads.get(mode)) return
  const response = file.response as { status?: boolean; data?: { path?: unknown }; message?: string } | undefined
  const path = typeof response?.data?.path === 'string' ? dashboardResourceUrl(response.data.path) : ''
  if (file.status !== 'done' || !response?.status || !path) { backgroundUploadError.value = response?.message || t('dashboard.studio.backgroundUploadFailed'); return }
  emit('background-image', mode, path)
}
function setBackgroundFit(value: unknown) { if (value === 'cover' || value === 'contain' || value === 'stretch' || value === 'tile') emit('board', { backgroundFit: value }) }
function setBackgroundPosition(value: unknown) { if (value === 'center' || value === 'top' || value === 'bottom') emit('board', { backgroundPosition: value }) }
const advancedData = computed(() => dashboardAdvancedDataTools.some(kind => kind === props.widget?.kind))
const operationData = computed(() => dashboardOperationDataTool(props.widget?.kind))
const businessData = computed(() => dashboardBusinessTool(props.widget?.kind))
const relationData = computed(() => dashboardRelationTool(props.widget?.kind))
const auditData = computed(() => dashboardAuditTool(props.widget?.kind))
const temporalData = computed(() => dashboardTemporalTool(props.widget?.kind))
const comparativeData = computed(() => dashboardComparativeTool(props.widget?.kind))
const planningData = computed(() => dashboardPlanningTool(props.widget?.kind))
const spatialData = computed(() => dashboardSpatialTool(props.widget?.kind))
const advancedContent = computed(() => dashboardAdvancedContentTools.some(kind => kind === props.widget?.kind))
const advancedDecoration = computed(() => dashboardAdvancedDecorationTools.some(kind => kind === props.widget?.kind))
const dataSettings = computed(() => { const value = dashboardDataSettings(props.widget?.dataOptions); return props.widget?.kind === 'radialProgress' ? { ...value, limit: Math.min(5, value.limit) } : value })
const contentSettings = computed(() => dashboardContentSettings(props.widget?.contentOptions))
const decorationSettings = computed(() => dashboardDecorationSettings(props.widget?.decorationOptions))
const timezoneOptions = computed(() => {
  const supported = (Intl as typeof Intl & { supportedValuesOf?: (key: string) => string[] }).supportedValuesOf?.('timeZone') || ['Asia/Shanghai','Asia/Tokyo','America/New_York','Europe/London']
  return [...new Set(['UTC', contentSettings.value.timezone, ...supported])].map(value => ({ value, label: value }))
})
function updateData(change: Partial<DashboardDataOptions>) { emit('widget', { dataOptions: dashboardDataSettings({ ...dataSettings.value, ...change }) }) }
function updateContent(change: Partial<DashboardContentOptions>) { emit('widget', { contentOptions: dashboardContentSettings({ ...contentSettings.value, ...change }) }) }
function updateDecoration(change: Partial<DashboardDecorationOptions>) { emit('widget', { decorationOptions: dashboardDecorationSettings({ ...decorationSettings.value, ...change }) }) }
function setImageFit(value: unknown) { if (value === 'cover' || value === 'contain') updateContent({ fit: value }) }
function setFontWeight(value: unknown) { if (value === 'normal' || value === 'medium' || value === 'bold') updateContent({ fontWeight: value }) }
const decorationNumbers = computed(() => [
  ...(['chevrons','zigzag','waveLine','honeycomb','techRuler','bezierConnector'].includes(props.widget?.kind ?? '') ? [{ key: 'count' as const, min: 2, max: 24 }] : []),
  { key: 'rotation' as const, min: -180, max: 180 }, { key: 'opacity' as const, min: 0, max: 100 },
])
const textContent = computed(() => !!props.widget && ['markdown', 'codeBlock', 'worldClock', 'sectionBanner', 'text', 'heading', 'note', 'badge', 'sectionDivider', 'imageText', 'bulletList', 'ribbon', 'textTicker', 'quote', 'actionButton', 'tagCloud', 'titlePlate', 'badgeSeal', 'copyText'].includes(props.widget.kind))
const configurableContent = computed(() => !!props.widget && (advancedContent.value || textContent.value || ['image', 'video', 'audio', 'clock', 'link', 'shape', 'linkList', 'countdown', 'icon', 'statusLegend', 'inspectionChecklist', 'definitionList'].includes(props.widget.kind)))
const coloredContent = computed(() => !!props.widget && ['calendarHeatmap', 'radialProgress', 'markdown', 'worldClock', 'imageCompare', 'iconLinks', 'stepGuide', 'shape', 'panel', 'progress', 'gauge', 'progressList', 'icon', 'borderFrame', 'gridPanel', 'gradientPanel', 'horizontalArrow', 'verticalArrow', 'quote', 'textTicker', 'announcement', 'tabsContent', 'accordion', 'tagCloud', ...dashboardDecorationTools].some(kind => kind === props.widget?.kind))
const usesAsset = computed(() => !!props.widget && dashboardWidgetUsesAsset(props.widget.kind))
const filterWidget = computed(() => !!props.widget && dashboardFilterTool(props.widget.kind))
const layout = computed(() => dashboardLayoutSettings(props.widget?.layoutOptions))
const containerChildren = computed(() => props.board.widgets.filter(child => child.parentId === props.widget?.id))
const containerCandidates = computed(() => !props.widget ? [] : props.board.widgets.filter(child => (!child.parentId || child.parentId === props.widget?.id)
  && dashboardCanContain(props.board.widgets, props.widget!.id, child.id)).map(child => ({ value: child.id, label: child.name })))
const childKinds = computed(() => dashboardTools.filter(kind => !dashboardScreenEffectTool(kind)).map(value => ({ value, label: t(dashboardWidgetLabelKey(value)) })))
const containerFields = computed(() => {
  const fields: { key: keyof DashboardLayoutOptions; min: number; max: number }[] = [{ key: 'gap', min: 0, max: 48 }, { key: 'padding', min: 0, max: 64 }]
  if (props.widget?.kind === 'gridContainer') fields.push({ key: 'columns', min: 1, max: 6 })
  if (props.widget?.kind === 'pagedGridContainer') fields.push({ key: 'columns', min: 1, max: 6 }, { key: 'pageSize', min: 1, max: 24 }, { key: 'itemHeight', min: 48, max: 800 })
  if (props.widget?.kind === 'spotlightContainer') fields.push({ key: 'columns', min: 3, max: 4 }, { key: 'itemHeight', min: 48, max: 800 })
  if (['splitContainer','sidebarContainer'].includes(props.widget?.kind || '')) fields.push({ key: 'ratio', min: 10, max: 90 })
  if (['headerContainer','footerContainer'].includes(props.widget?.kind || '')) fields.push({ key: 'header', min: 10, max: 70 })
  if (props.widget?.kind === 'responsiveContainer') fields.push({ key: 'minWidth', min: 80, max: 600 })
  if (props.widget?.kind === 'scrollContainer' || props.widget?.kind === 'accordionContainer') fields.push({ key: 'itemHeight', min: 48, max: 800 })
  return fields
})
function updateLayout(change: Partial<DashboardLayoutOptions>) { emit('widget', { layoutOptions: dashboardLayoutSettings({ ...layout.value, ...change }) }) }
function setContainerChildren(value: unknown) { if (Array.isArray(value)) emit('children', value.filter((id): id is string => typeof id === 'string')) }
const effectKind = computed(() => props.widget && dashboardScreenEffectTool(props.widget.kind) ? props.widget.kind : undefined)
const effectSettings = computed(() => effectKind.value ? dashboardScreenEffectSettings(effectKind.value, props.widget?.screenEffect) : undefined)
const effectDefaultColor = computed(() => effectKind.value ? dashboardScreenEffectPresets[effectKind.value].color : '#FFFFFF')
type EffectNumber = 'quantity' | 'speed' | 'size' | 'opacity' | 'wind'
const effectFields = computed<EffectNumber[]>(() => ['stars','meteors','sparkles','lightning','fireworks','warpStars','sunRays'].includes(effectKind.value ?? '')
  ? ['quantity', 'speed', 'size', 'opacity'] : ['quantity', 'speed', 'size', 'opacity', 'wind'])
function effectLimit(key: EffectNumber) {
  if (key === 'quantity') return { min: 1, max: effectKind.value ? dashboardScreenEffectQuantityLimit(effectKind.value) : 300 }
  if (key === 'speed') return { min: 10, max: 300 }
  if (key === 'size') return { min: 1, max: effectKind.value ? dashboardScreenEffectSizeLimit(effectKind.value) : 32 }
  if (key === 'wind') return { min: -100, max: 100 }
  return { min: 0, max: 100 }
}
function updateScreenEffect(change: Partial<DashboardScreenEffectOptions>) {
  if (effectKind.value) emit('widget', { screenEffect: dashboardScreenEffectSettings(effectKind.value, { ...effectSettings.value, ...change }) })
}
function setEffectLayer(value: unknown) {
  if (value === 'foreground' || value === 'background') updateScreenEffect({ layer: value })
}
const metricWidget = computed(() => !!props.widget && dashboardMetricTool(props.widget.kind))
const asset = computed(() => props.reports.find(item => item.code === props.widget?.reportCode))
const sourceName = computed(() => asset.value?.source?.name || asset.value?.dataset?.name || asset.value?.name)
const snapshot = computed(() => dashboardSnapshot(asset.value))
const chartFields = computed(() => (snapshot.value?.queryChart.options.fields ?? []).map(value => ({ value, label: value })))
const chartNumericFields = computed(() => chartFields.value.filter(field => chartFieldKind(snapshot.value?.columns ?? [], field.value) === 'number'))
const chartDateFields = computed(() => chartFields.value.filter(field => chartFieldKind(snapshot.value?.columns ?? [], field.value) === 'date'))
const dateFilterWidget = computed(() => props.widget?.kind === 'dateRangeFilter' || props.widget?.kind === 'relativeDateFilter')
const filterFields = computed(() => {
  if (dateFilterWidget.value) return chartDateFields.value
  if (props.widget?.kind === 'numberRangeFilter') return chartNumericFields.value
  if (props.widget?.kind !== 'booleanFilter') return chartFields.value
  const rows = snapshot.value?.columns.slice(0, 200) ?? []
  return chartFields.value.filter(field => {
    const samples = rows.map(row => row[field.value]).filter(value => !dashboardValueMissing(value))
    return samples.length > 0 && samples.every(value => dashboardBooleanValue(value) !== undefined)
  })
})
const iconOptions = Object.keys(MENU_ICONS).map(value => ({ value, label: value }))
const cornerPositions = computed(() => dashboardRadiusCorners.map(value => ({ value, label: t('dashboard.studio.inset.' + value) })))
function setCornerPosition(value: unknown) {
  const cornerPosition = dashboardRadiusCorners.find(position => position === value)
  if (cornerPosition) emit('widget', { cornerPosition })
}
const animation = computed(() => props.widget?.animation ?? dashboardAnimationDefaults)
const animationOptions = computed(() => dashboardAnimationEffects.map(value => ({ value, label: t('dashboard.studio.animationEffect.' + value) })))
const animationNumbers = ['duration', 'delay'] as const
function updateAnimation(change: Partial<DashboardAnimation>) { emit('widget', { animation: { ...animation.value, ...change } }) }
function setAnimationEffect(value: unknown) {
  const effect = dashboardAnimationEffects.find(effect => effect === value)
  if (effect) updateAnimation({ effect })
}
function setAnimationTrigger(value: unknown) { if (value === 'load' || value === 'hover') updateAnimation({ trigger: value }) }
function setAnimationEasing(value: unknown) {
  if (value === 'linear' || value === 'ease-in' || value === 'ease-out' || value === 'ease-in-out') updateAnimation({ easing: value })
}
const specializedMappings = computed(() => {
  if (!props.widget || metricWidget.value || filterWidget.value || dashboardDataTool(props.widget.kind)) return
  const mappings = dashboardChartMappings(props.widget, snapshot.value)
  return chartDataSpec(mappings.type) && mappings
})
function changeMappings(change: DashboardChartOverrides) {
  emit('widget', { chartOptions: { ...props.widget?.chartOptions, ...change } })
}
const specialSetup = computed(() => {
  const type = specializedMappings.value?.type
  return type === 'map' || type === 'custom' ? type : undefined
})
const dataAggregation = computed(() => {
  const widget = props.widget
  if (!widget || !dashboardDataAggregationTool(widget.kind)) return
  return widget.chartOptions?.aggregation ?? snapshot.value?.queryChart.options.aggregation ?? 'count'
})
const dataMetric = computed(() => props.widget?.chartOptions?.metric ?? snapshot.value?.queryChart.options.metric ?? '')
const dataMetricOptions = computed(() => dataAggregation.value === 'count'
  ? [{ label: t('queryChart.records'), value: 0 }, ...chartFields.value]
  : dataAggregation.value === 'distinct' ? chartFields.value : chartNumericFields.value)
function changeAsset(value: unknown) {
  const mapGeoJson = props.widget?.chartOptions?.mapGeoJson
  emit('widget', { reportCode: typeof value === 'string' && value ? value : undefined, field: '', displayFields: undefined, recordIndex: undefined, timeField: undefined,
    dataOptions: props.widget?.dataOptions ? { ...dataSettings.value, baselineField: '', statusField: '' } : undefined,
    operationsOptions: operationData.value ? dashboardOperationsSettings(props.widget?.kind, { ...props.widget?.operationsOptions, labelField: '', statusField: '', detailsField: '', endField: '' }) : undefined,
    businessOptions: businessData.value ? dashboardBusinessSettings({ ...props.widget?.businessOptions, groupField: '', subgroupField: '', referenceField: '', entityField: '', stageOrder: [] }) : undefined,
    relationOptions: relationData.value ? dashboardRelationSettings({ dimensionLimit: props.widget?.relationOptions?.dimensionLimit }) : undefined,
    auditOptions: auditData.value ? dashboardAuditSettings({ ...props.widget?.auditOptions, fields: [] }) : undefined,
    temporalOptions: temporalData.value ? dashboardTemporalSettings({ ...props.widget?.temporalOptions, entityField: '', endField: '', labelField: '' }) : undefined,
    comparativeOptions: comparativeData.value ? dashboardComparativeSettings({ ...props.widget?.comparativeOptions,
      fields: [], secondField: '', groupField: '', leftGroup: '', rightGroup: '' }) : undefined,
    planningOptions: planningData.value ? dashboardPlanningSettings({ ...props.widget?.planningOptions,
      statusField: '', labelField: '', ownerField: '', groupField: '', completedField: '' }) : undefined,
    spatialOptions: spatialData.value ? dashboardSpatialSettings({ ...props.widget?.spatialOptions, latitudeField: '', longitudeField: '', groupField: '' }) : undefined,
    chartOptions: mapGeoJson === undefined ? undefined : { mapGeoJson } })
}
function setDataAggregation(value: unknown) {
  if (value === 'count' || value === 'distinct' || value === 'sum' || value === 'avg' || value === 'min' || value === 'max') {
    const metric = value === 'count' || value === 'distinct' ? dataMetric.value
      : chartNumericFields.value.some(field => field.value === dataMetric.value) ? dataMetric.value : chartNumericFields.value[0]?.value ?? ''
    changeMappings({ aggregation: value, metric })
  }
}
function changeAlignment(value: unknown) {
  if (value === 'left' || value === 'center' || value === 'right') emit('widget', { alignment: value })
}
function changeTone(value: unknown) {
  if (value === 'info' || value === 'success' || value === 'warning' || value === 'danger') emit('widget', { tone: value })
}
const dimensions = ['w', 'h', 'x', 'y'] as const
const dashboardSpacing = ['gap', 'rowHeight', 'padding'] as const
const insetGroups = [
  { key: 'padding', label: 'padding', parts: dashboardPaddingEdges },
  { key: 'corners', label: 'radius', parts: dashboardRadiusCorners },
] as const
const choices = (prefix: string, values: string[]) => values.map(value => ({ value, label: t('dashboard.studio.' + prefix + '.' + value) }))
const refreshOptions = computed(() => choices('refresh', ['saved', 'custom', '5m', 'off']))
const refreshConfig = computed(() => props.widget ?? props.board)
const canConfigureRefresh = computed(() => !props.widget || !!(asset.value?.realtime && snapshot.value))
function isChartType(kind: string): kind is QueryChartType { return queryChartTypes.some(type => type === kind) }
const widgetKinds = computed(() => ['report' as const, ...dashboardTools].map(value => ({
  value,
  label: t(dashboardWidgetLabelKey(value)),
})))
function changeKind(value: unknown) {
  const kind = widgetKinds.value.find(item => item.value === value)?.value
  if (!kind || !props.widget || kind === props.widget.kind) return
  const effect = dashboardScreenEffectTool(kind)
  const modeDefaults = effect || effectKind.value ? { movable: !effect, resizable: !effect, showCard: !effect, showTitle: !effect } : {}
  const linked = dashboardSpatialTool(kind) || dashboardPlanningTool(kind) || dashboardComparativeTool(kind) || dashboardTemporalTool(kind) || dashboardRelationTool(kind) || dashboardAuditTool(kind) && kind !== 'dateCoverage'
  emit('widget', { ...modeDefaults, kind, chartOptions: undefined, field: '', reportCode: dashboardWidgetUsesAsset(kind) ? props.widget.reportCode : undefined,
    operationsOptions: dashboardOperationDataTool(kind) ? dashboardOperationsSettings(kind) : undefined,
    businessOptions: dashboardBusinessTool(kind) ? dashboardBusinessSettings() : undefined,
    relationOptions: dashboardRelationTool(kind) ? dashboardRelationSettings() : undefined,
    auditOptions: dashboardAuditTool(kind) ? dashboardAuditSettings() : undefined,
    temporalOptions: dashboardTemporalTool(kind) ? dashboardTemporalSettings() : undefined,
    comparativeOptions: dashboardComparativeTool(kind) ? dashboardComparativeSettings() : undefined,
    planningOptions: dashboardPlanningTool(kind) ? dashboardPlanningSettings() : undefined,
    spatialOptions: dashboardSpatialTool(kind) ? dashboardSpatialSettings() : undefined,
    action: linked ? 'filter' : relationData.value || auditData.value || temporalData.value || comparativeData.value || planningData.value || spatialData.value ? 'saved' : props.widget.action,
    screenEffect: effect ? dashboardScreenEffectSettings(kind) : undefined })
}
function setRefreshMinutes(value: number | undefined) {
  if (value === undefined) return
  if (props.widget) emit('widget', { refreshSeconds: value * 60 })
  else emit('board', { refreshSeconds: value * 60 })
}
const reportOptions = computed(() => props.reports.filter(item => props.widget?.kind === 'report' || !!dashboardSnapshot(item)).map(item => ({ value: item.code ?? '', label: item.name || item.code || '' })))
const fieldOptions = computed(() => {
  const config = snapshot.value
  if (!config) return []
  const options = config.queryChart.options
  const widget = props.widget
  const fields = widget?.operation === 'distinct' ? options.fields : options.fields.filter(field => chartFieldKind(config.columns, field) === 'number')
  return fields.map(value => ({ value, label: value }))
})
function setDisplayFields(value: unknown) {
  if (Array.isArray(value)) emit('widget', { displayFields: value.filter((field): field is string => typeof field === 'string').slice(0, 12) })
}
function setDeadline(change: Partial<DashboardDeadline>) {
  emit('widget', { deadline: { date: '', hour: 0, minute: 0, ...props.widget?.deadline, ...change } })
}
const resolutions = computed(() => [{ value: 'auto', label: t('dashboard.studio.resolutionAuto') }, ...dashboardWallPresets])
const aggregations = computed(() => ['count', 'distinct', 'sum', 'avg', 'min', 'max'].map(value => ({
  value, label: t('queryChart.choice.aggregation.' + value)
})))
function limit(key: keyof DashboardRect) {
  return key === 'w' || key === 'x' ? wall.value ? props.board.width : 12 : wall.value ? props.board.height : 1000
}
function setRect(key: keyof DashboardRect, value: number | undefined) { if (value !== undefined) emit('widget', { [key]: value }) }
function updateCanvasNumber(key: keyof typeof dashboardCanvasNumbers, value: number | undefined) {
  if (value !== undefined) emit('board', { [key]: dashboardCanvasNumber(key, value) })
}
function updateTitleSize(value: number | undefined) { if (value !== undefined) emit('widget', { titleSize: value }) }
function updateInset(key: 'padding' | 'corners', index: number, value: number | undefined) {
  if (!props.widget || value === undefined) return
  const insets: DashboardInsets = [...props.widget[key]]
  insets[index] = value
  emit('widget', { [key]: insets })
}
function changeResolution(value: string | number | undefined) {
  const update = dashboardWallResolution(value)
  if (update) emit('board', update)
}
function setTarget(value: number | undefined) { if (value !== undefined) emit('widget', { target: value }) }
function changeShape(value: string | number | undefined) {
  if (value === 'rectangle' || value === 'circle') emit('widget', { shape: value })
}
function changeFit(value: string | number | undefined) { if (value === 'contain' || value === 'cover') emit('board', { fit: value }) }
function changeTheme(value: string | number | undefined) { if (value === 'ocean' || value === 'jade') emit('board', { theme: value }) }
function updateRefresh(value: string | number | undefined, widget: boolean) {
  if (value !== 'saved' && value !== '5m' && value !== 'custom' && value !== 'off') return
  if (widget) emit('widget', { refresh: value as DashboardRefresh })
  else emit('board', { refresh: value as DashboardRefresh })
}
function changeAction(value: string | number | undefined) {
  if (value === 'saved' || value === 'details' || value === 'filter' || value === 'none') emit('widget', { action: value as DashboardAction })
}
function changeOperation(value: string | number | undefined) {
  if (value === 'count' || value === 'distinct' || value === 'sum' || value === 'avg' || value === 'min' || value === 'max') {
    emit('widget', { operation: value, field: '' })
  }
}
async function validate() {
  tab.value = 'data'
  await nextTick()
  pageInfo.value?.expand()
  await settingsForm.value?.validate()
}
defineExpose({ validate, showPageSettings: () => { tab.value = 'data' } })
</script>

<style scoped>
.inspector-section { display: grid; grid-template-columns: minmax(0, 1fr); min-width: 0; gap: 12px; padding: 12px; }
.inspector-tabs :deep([role="tablist"]) { margin: 0 12px; }
.inspector-tabs :deep([role="tab"]) { flex: 1; min-width: 0; padding-inline: 6px; }
.toggle-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; font-size: 12px; }
</style>
