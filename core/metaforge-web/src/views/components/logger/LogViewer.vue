<template>
  <div class="dc-logger w-full font-mono text-xs">
    <div v-if="toolbar" class="flex items-center gap-2 p-2">
      <MfSelect v-model="filterLevel"
                class="w-32"
                size="sm"
                :options="levelOptions"
                :placeholder="$t('logger.text.allLevel')"/>

      <MfInput v-model="searchText"
               class="flex-1"
               size="sm"
               clearable
               :placeholder="$t('logger.placeholder.search')"/>
    </div>

    <div class="dc-logger__body overflow-y-auto"
         :style="{ height: normalizeSize(height), maxHeight: normalizeSize(height) }">
      <div class="min-w-full inline-block">
        <div v-for="(entry, index) in filteredEntries"
             :key="index"
             class="dc-logger__row whitespace-pre py-1 px-4 space-x-2 hover:bg-gray-100 w-full block">
          <template v-if="$slots.content">
            <slot name="content" :item="entry"/>
          </template>
          <template v-else>
            <span v-if="entry.timestamp" class="text-gray-500">{{ entry.timestamp }}</span>
            <span v-if="entry.thread" class="text-gray-700">[{{ entry.thread }}]</span>
            <span v-if="entry.logger" class="text-gray-500">{{ entry.logger }}</span>
            <span v-if="entry.file" class="text-gray-500">[{{ entry.file }}]</span>
            <span :style="{ color: levelColors[entry.level] || undefined }"
                  class="font-semibold">{{ entry.level }}</span>
            <span>{{ entry.message }}</span>
          </template>
        </div>

        <div v-if="filteredEntries.length === 0" class="p-4 text-center text-gray-400">
          {{ $t('common.noData') }}
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed, PropType } from 'vue'
import { useI18n } from 'vue-i18n'

interface LogEntry
{
  timestamp: string
  level: string
  thread: string
  logger: string
  file: string
  message: string
}

export interface LoggerPatterns
{
  timestamp?: RegExp[]
  level?: RegExp[]
  thread?: RegExp[]
  logger?: RegExp[]
  file?: RegExp[]
}

/**
 * Finds the first match of any pattern in the remaining text,
 * returning the matched value and the text with the match removed.
 */
const findMatch = (patterns: RegExp[] | undefined, text: string): { value: string; remaining: string } => {
  if (!patterns) {
    return { value: '', remaining: text }
  }

  for (const pattern of patterns) {
    const matched = pattern.exec(text)
    if (matched) {
      return {
        value: matched[1] || matched[0],
        remaining: (text.slice(0, matched.index) + text.slice(matched.index + matched[0].length)).trim()
      }
    }
  }
  return { value: '', remaining: text }
}

export default defineComponent({
  name: 'LogViewer',
  props: {
    items: {
      type: Array as PropType<string[]>,
      default: () => []
    },
    highlightConfig: {
      type: Object as PropType<Record<string, string>>,
      default: () => ({
        WARN: 'var(--dc-warning)',
        ERROR: 'var(--dc-danger)',
        DEBUG: 'var(--dc-text-secondary)',
        TRACE: 'var(--dc-text-placeholder)'
      })
    },
    height: {
      type: [Number, String],
      default: 200
    },
    toolbar: {
      type: Boolean,
      default: false
    },
    caseSensitive: {
      type: Boolean,
      default: false
    },
    customPatterns: {
      type: Object as PropType<LoggerPatterns>,
      default: undefined
    }
  },
  setup(props)
  {
    const { t } = useI18n()

    const filterLevel = ref('')
    const searchText = ref('')

    const levelOptions = computed(() => [
      { label: t('logger.text.allLevel'), value: '' },
      ...Object.keys(props.highlightConfig).map((level) => ({ label: level, value: level }))
    ])

    const parseLine = (line: string): LogEntry => {
      const entry: LogEntry = { timestamp: '', level: '', thread: '', logger: '', file: '', message: '' }
      let remaining = line.trim()

      const groups: Array<keyof LoggerPatterns> = ['timestamp', 'level', 'thread', 'logger', 'file']
      for (const group of groups) {
        const result = findMatch(props.customPatterns?.[group], remaining)
        if (result.value) {
          (entry as any)[group] = result.value
          remaining = result.remaining
        }
      }

      entry.message = remaining
      entry.level = (entry.level || 'INFO').trim()
      return entry
    }

    const entries = computed<LogEntry[]>(() => props.items.map((line) => parseLine(String(line))))

    const filteredEntries = computed(() => {
      return entries.value.filter((entry) => {
        const levelMatched = !filterLevel.value || entry.level === filterLevel.value
        if (!levelMatched) {
          return false
        }

        if (!searchText.value) {
          return true
        }

        const source = Object.values(entry).map((value) => String(value))
        const target = props.caseSensitive ? searchText.value : searchText.value.toLowerCase()
        return source.some((value) => (props.caseSensitive ? value : value.toLowerCase()).includes(target))
      })
    })

    const normalizeSize = (value: number | string): string => {
      return typeof value === 'number' ? `${ value }px` : value
    }

    return {
      filterLevel,
      searchText,
      levelOptions,
      entries,
      filteredEntries,
      levelColors: props.highlightConfig,
      normalizeSize
    }
  }
})
</script>

<style scoped>
.dc-logger__row {
  line-height: 1.4;
}
</style>
