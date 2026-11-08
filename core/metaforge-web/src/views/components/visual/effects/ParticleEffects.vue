<template>
  <div ref="root" class="dashboard-screen-effect pointer-events-none absolute inset-0 overflow-hidden" :data-screen-effect="kind"
       :class="{ 'dashboard-widget-motion': !!widget.animation && widget.animation.effect !== 'none', 'motion-on-hover': widget.animation?.trigger === 'hover' }"
       :style="dashboardMotionStyle(widget)" aria-hidden="true">
    <canvas ref="canvas" class="block size-full" :style="{ opacity: settings.opacity / 100 }"/>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onScopeDispose, ref, watch } from 'vue'
import { dashboardCanvasColor, dashboardMotionStyle, dashboardScreenEffectPresets, dashboardScreenEffectSettings,
  type DashboardScreenEffectKind, type DashboardWidget } from '@/model/dashboard-layout'
import { createAdditionalScreenPainters, type ScreenParticle as Particle } from './screen-effect-painters'

const props = defineProps<{ widget: Pick<DashboardWidget, 'animation' | 'color' | 'id' | 'screenEffect'>; kind: DashboardScreenEffectKind }>()
const root = ref<HTMLElement>()
const canvas = ref<HTMLCanvasElement>()
const settings = computed(() => dashboardScreenEffectSettings(props.kind, props.widget.screenEffect))
const color = computed(() => dashboardCanvasColor(props.widget.color) || dashboardScreenEffectPresets[props.kind].color)
const particles = computed(() => {
  let seed = [...props.widget.id].reduce((hash, character) => Math.imul(hash, 31) + character.charCodeAt(0), 7) >>> 0
  const random = () => { seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0; return seed / 4294967296 }
  return Array.from({ length: settings.value.quantity }, (): Particle => ({
    x: random(), y: random(), depth: .35 + random() * .65, phase: random() * Math.PI * 2, spin: random() * 2 - 1,
  }))
})
let width = 0
let height = 0
let context: CanvasRenderingContext2D | null = null
let frame: number | undefined
let previous = 0
let elapsed = 0
let visible = true
let mounted = false
let resizeObserver: ResizeObserver | undefined
let intersectionObserver: IntersectionObserver | undefined
const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)')
const wrap = (value: number) => (value % 1 + 1) % 1
const radius = (particle: Particle) => settings.value.size * particle.depth
function position(particle: Particle, time: number, vertical: number, sway = .02) {
  return {
    x: wrap(particle.x + time * settings.value.wind / 4000 + Math.sin(time + particle.phase) * sway) * width,
    y: wrap(particle.y + time * vertical * particle.depth) * height,
  }
}
function circle(ctx: CanvasRenderingContext2D, x: number, y: number, size: number) {
  ctx.beginPath(); ctx.arc(x, y, size, 0, Math.PI * 2); ctx.fill()
}
const painters: Record<DashboardScreenEffectKind, (ctx: CanvasRenderingContext2D, particle: Particle, time: number, index: number) => void> = {
  ...createAdditionalScreenPainters({ get width() { return width }, get height() { return height }, get size() { return settings.value.size },
    get wind() { return settings.value.wind }, get color() { return color.value }, get customColor() { return dashboardCanvasColor(props.widget.color) } }, position),
  snow(ctx, particle, time) {
    const { x, y } = position(particle, time, .06)
    ctx.globalAlpha = .35 + particle.depth * .65
    circle(ctx, x, y, radius(particle))
  },
  rain(ctx, particle, time) {
    const { x, y } = position(particle, time, .65, 0)
    const length = radius(particle) * 8
    ctx.globalAlpha = particle.depth
    ctx.lineWidth = Math.max(.6, radius(particle) * .4)
    ctx.beginPath(); ctx.moveTo(x, y); ctx.lineTo(x - settings.value.wind / 100 * length * .5, y - length); ctx.stroke()
  },
  stars(ctx, particle, time) {
    ctx.globalAlpha = .25 + (.5 + Math.sin(time * .7 + particle.phase) * .5) * .75
    circle(ctx, particle.x * width, particle.y * height, Math.max(.6, radius(particle)))
  },
  meteors(ctx, particle, time) {
    const phase = wrap(time / 7 + particle.phase / (Math.PI * 2))
    if (phase > .24) return
    const progress = phase / .24
    const x = (particle.x + .4 - progress * .65) * width
    const y = (particle.y * .5 + progress * .55) * height
    const tail = Math.max(20, settings.value.size * 18)
    const gradient = ctx.createLinearGradient(x, y, x + tail, y - tail * .55)
    gradient.addColorStop(0, color.value); gradient.addColorStop(1, color.value + '00')
    ctx.strokeStyle = gradient; ctx.lineWidth = Math.max(1, radius(particle))
    ctx.beginPath(); ctx.moveTo(x, y); ctx.lineTo(x + tail, y - tail * .55); ctx.stroke()
  },
  fireflies(ctx, particle, time) {
    const x = wrap(particle.x + Math.sin(time * .35 + particle.phase) * .06 + time * settings.value.wind / 8000) * width
    const y = wrap(particle.y + Math.cos(time * .25 + particle.phase) * .05) * height
    const r = radius(particle)
    const gradient = ctx.createRadialGradient(x, y, 0, x, y, r * 4)
    gradient.addColorStop(0, color.value); gradient.addColorStop(1, color.value + '00')
    ctx.globalAlpha = .3 + (.5 + Math.sin(time + particle.phase) * .5) * .7
    ctx.fillStyle = gradient; circle(ctx, x, y, r * 4)
  },
  bubbles(ctx, particle, time) {
    const { x, y } = position(particle, time, -.045)
    const r = radius(particle)
    ctx.lineWidth = 1
    ctx.globalAlpha = .35 + particle.depth * .4
    ctx.beginPath(); ctx.arc(x, y, r, 0, Math.PI * 2); ctx.stroke()
    ctx.globalAlpha *= .7
    ctx.beginPath(); ctx.arc(x, y, r * .65, Math.PI, Math.PI * 1.45); ctx.stroke()
  },
  confetti(ctx, particle, time, index) {
    const { x, y } = position(particle, time, .1, .04)
    const size = radius(particle)
    ctx.fillStyle = dashboardCanvasColor(props.widget.color) || ['#E39CBE', '#E4BE63', '#6EBDB8', '#7CAADD', '#B39CD9'][index % 5]
    ctx.translate(x, y); ctx.rotate(particle.phase + time * particle.spin * 2)
    ctx.fillRect(-size / 2, -size * .7, size, size * 1.4)
  },
  petals(ctx, particle, time) {
    const { x, y } = position(particle, time, .055, .055)
    const size = radius(particle)
    ctx.translate(x, y); ctx.rotate(particle.phase + time * particle.spin)
    ctx.globalAlpha = .5 + particle.depth * .5
    ctx.beginPath(); ctx.ellipse(0, 0, size, size * .55, 0, 0, Math.PI * 2); ctx.fill()
  },
  particleNetwork(ctx, particle, time) {
    const { x, y } = position(particle, time, .004, .025)
    circle(ctx, x, y, Math.max(1, radius(particle)))
  },
  aurora(ctx, particle, time, index) {
    const top = height * (.1 + particle.y * .55)
    const thickness = settings.value.size * (.6 + particle.depth)
    const gradient = ctx.createLinearGradient(0, top - thickness, 0, top + thickness)
    gradient.addColorStop(0, color.value + '00'); gradient.addColorStop(.5, color.value + '80'); gradient.addColorStop(1, color.value + '00')
    ctx.fillStyle = gradient
    ctx.beginPath()
    for (let step = 0; step <= 30; step++) {
      const x = width * step / 30
      const y = top + Math.sin(step * .22 + time * .25 + particle.phase + settings.value.wind / 100) * thickness * .5
      if (step === 0) ctx.moveTo(x, y)
      else ctx.lineTo(x, y)
    }
    for (let step = 30; step >= 0; step--) {
      const x = width * step / 30
      const y = top + Math.sin(step * .22 + time * .25 + particle.phase + settings.value.wind / 100) * thickness * .5 + thickness
      ctx.lineTo(x, y)
    }
    ctx.globalAlpha = .3 + index % 3 * .15
    ctx.closePath(); ctx.fill()
  },
}
function paint() {
  const ctx = context
  if (!ctx || !width || !height) return
  ctx.clearRect(0, 0, width, height)
  if (!settings.value.enabled) return
  const time = elapsed * settings.value.speed / 100
  const points = particles.value
  if (props.kind === 'particleNetwork') {
    ctx.strokeStyle = color.value
    for (let index = 0; index < points.length; index++) {
      const first = position(points[index], time, .004, .025)
      for (let next = index + 1; next < points.length; next++) {
        const second = position(points[next], time, .004, .025)
        const distance = Math.hypot(first.x - second.x, first.y - second.y)
        if (distance > 130) continue
        ctx.globalAlpha = (1 - distance / 130) * .35
        ctx.beginPath(); ctx.moveTo(first.x, first.y); ctx.lineTo(second.x, second.y); ctx.stroke()
      }
    }
  }
  points.forEach((particle, index) => {
    ctx.save(); ctx.globalAlpha = 1; ctx.fillStyle = color.value; ctx.strokeStyle = color.value
    painters[props.kind](ctx, particle, time, index)
    ctx.restore()
  })
  ctx.globalAlpha = 1
}
function tick(timestamp: number) {
  if (previous) elapsed += Math.min(.05, (timestamp - previous) / 1000)
  previous = timestamp
  paint()
  frame = requestAnimationFrame(tick)
}
function syncPlayback() {
  if (frame !== undefined) cancelAnimationFrame(frame)
  frame = undefined; previous = 0
  paint()
  if (mounted && visible && !document.hidden && !reducedMotion.matches && settings.value.enabled && settings.value.opacity > 0) {
    frame = requestAnimationFrame(tick)
  }
}
function resize() {
  const element = root.value
  const surface = canvas.value
  if (!element || !surface) return
  width = element.clientWidth; height = element.clientHeight
  const ratio = Math.min(2, window.devicePixelRatio || 1)
  surface.width = Math.max(1, Math.round(width * ratio)); surface.height = Math.max(1, Math.round(height * ratio))
  context = surface.getContext('2d')
  context?.setTransform(ratio, 0, 0, ratio, 0, 0)
  paint()
}
watch([settings, color, particles], syncPlayback)
onMounted(() => {
  mounted = true
  resize()
  resizeObserver = new ResizeObserver(resize)
  if (root.value) resizeObserver.observe(root.value)
  if (typeof IntersectionObserver !== 'undefined') {
    intersectionObserver = new IntersectionObserver(entries => { visible = entries[0]?.isIntersecting ?? true; syncPlayback() })
    if (root.value) intersectionObserver.observe(root.value)
  }
  window.addEventListener('resize', resize)
  document.addEventListener('visibilitychange', syncPlayback)
  reducedMotion.addEventListener('change', syncPlayback)
  syncPlayback()
})
onScopeDispose(() => {
  mounted = false
  if (frame !== undefined) cancelAnimationFrame(frame)
  resizeObserver?.disconnect(); intersectionObserver?.disconnect()
  window.removeEventListener('resize', resize)
  document.removeEventListener('visibilitychange', syncPlayback)
  reducedMotion.removeEventListener('change', syncPlayback)
})
</script>
