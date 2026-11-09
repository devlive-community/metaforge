import type { Painter, Position, ScreenEnvironment } from './screen-effect-painters'

const turn = Math.PI * 2
const wrap = (value: number) => value - Math.floor(value)

export function createFlowScreenPainters(env: ScreenEnvironment, position: Position) {
  return {
    streamlineFlow(ctx, particle, time) {
      const { x, y } = position(particle, time, .008, .028)
      const length = env.size * (1 + particle.depth)
      const drift = Math.sin(time * .62 + particle.phase) * length * .27
      ctx.globalAlpha = .38
      ctx.lineWidth = 1 + particle.depth
      ctx.beginPath()
      ctx.moveTo(x - length, y + drift)
      ctx.bezierCurveTo(x - length * .66, y + drift, x - length * .29, y - drift, x, y)
      ctx.stroke()
      ctx.globalAlpha = .65
      ctx.beginPath()
      ctx.arc(x, y, Math.max(1, particle.depth * 2.2), 0, turn)
      ctx.fill()
    },
    bokehField(ctx, particle, time) {
      const { x, y } = position(particle, time, -.016, .035)
      const radius = env.size * (.6 + particle.depth)
      const gradient = ctx.createRadialGradient(x, y, radius * .15, x, y, radius)
      gradient.addColorStop(0, env.color + '55')
      gradient.addColorStop(.72, env.color + '33')
      gradient.addColorStop(1, env.color + '00')
      ctx.fillStyle = gradient
      ctx.globalAlpha = .62 + Math.sin(time * .42 + particle.phase) ** 2 * .3
      ctx.fillRect(x - radius, y - radius, radius * 2, radius * 2)
    },
    polygonBloom(ctx, particle, time) {
      const { x, y } = position(particle, time, 0, .012)
      const phase = wrap(particle.phase / turn + time * .21)
      const radius = env.size * (.2 + phase * 1.5) * particle.depth
      const corners = 3 + Math.floor(particle.x * 4)
      ctx.globalAlpha = (1 - phase) * .52
      ctx.lineWidth = 1.2
      ctx.beginPath()
      for (let index = 0; index < corners; index++) {
        const angle = particle.phase + time * particle.spin * .16 + index * turn / corners
        const px = x + Math.cos(angle) * radius
        const py = y + Math.sin(angle) * radius
        if (index === 0) ctx.moveTo(px, py)
        else ctx.lineTo(px, py)
      }
      ctx.closePath()
      ctx.stroke()
    },
    glowRails(ctx, particle, time) {
      const x = env.width * particle.x
      const y = wrap(particle.y + time * (.12 + particle.depth * .09)) * env.height
      const sway = env.wind / 100 * env.width * .17
      const length = env.size * (1 + particle.depth)
      ctx.globalAlpha = .11
      ctx.beginPath()
      ctx.moveTo(x, 0)
      ctx.lineTo(x + sway, env.height)
      ctx.stroke()
      const head = x + sway * y / env.height
      const gradient = ctx.createLinearGradient(head, y - length, head, y)
      gradient.addColorStop(0, env.color + '00')
      gradient.addColorStop(1, env.color + 'D9')
      ctx.strokeStyle = gradient
      ctx.globalAlpha = .75
      ctx.lineWidth = Math.max(1, particle.depth * 3)
      ctx.beginPath()
      ctx.moveTo(head - sway * length / env.height, y - length)
      ctx.lineTo(head, y)
      ctx.stroke()
    },
    twilightBands(ctx, particle, time) {
      const band = env.size * (1 + particle.depth)
      const y = env.height * (.24 + .58 * particle.y) + Math.sin(time * .22 + particle.phase) * band * .42 + env.wind * .32
      const gradient = ctx.createLinearGradient(0, y - band, 0, y + band)
      gradient.addColorStop(0, env.color + '00')
      gradient.addColorStop(.5, env.color + '76')
      gradient.addColorStop(1, env.color + '00')
      ctx.fillStyle = gradient
      ctx.globalAlpha = .26
      ctx.fillRect(0, y - band, env.width, band * 2)
    },
    starFlares(ctx, particle, time) {
      const { x, y } = position(particle, time, -.008, .02)
      const radius = env.size * particle.depth * (.7 + .3 * Math.sin(time * .65 + particle.phase) ** 2)
      ctx.globalAlpha = .64
      ctx.beginPath()
      ctx.moveTo(x - radius, y)
      ctx.quadraticCurveTo(x, y - radius * .17, x, y - radius * .66)
      ctx.quadraticCurveTo(x, y - radius * .17, x + radius, y)
      ctx.quadraticCurveTo(x, y + radius * .17, x, y + radius * .66)
      ctx.quadraticCurveTo(x, y + radius * .17, x - radius, y)
      ctx.fill()
    },
    paperStreamers(ctx, particle, time) {
      const { x, y } = position(particle, time, .07, .035)
      const size = env.size * particle.depth
      ctx.translate(x, y)
      ctx.rotate(particle.phase + Math.sin(time + particle.phase) * .4)
      ctx.globalAlpha = .55
      ctx.lineWidth = Math.max(1, size * .16)
      ctx.beginPath()
      ctx.moveTo(0, 0)
      ctx.bezierCurveTo(size, -size, size * 1.7, size * .34, size * 2.3, -size * .5)
      ctx.stroke()
    },
    rasterSweep(ctx, particle, time) {
      const size = env.size * (1 + particle.depth)
      const y = wrap(particle.y + time * .05) * (env.height + size * 4) - size * 2
      const slope = env.width * (.14 + env.wind / 800)
      const gradient = ctx.createLinearGradient(0, y - size, 0, y + size)
      gradient.addColorStop(0, env.color + '00')
      gradient.addColorStop(.5, env.color + 'AE')
      gradient.addColorStop(1, env.color + '00')
      ctx.fillStyle = gradient
      ctx.globalAlpha = .35
      ctx.beginPath()
      ctx.moveTo(0, y - size - slope * .5)
      ctx.lineTo(env.width, y - size + slope * .5)
      ctx.lineTo(env.width, y + size + slope * .5)
      ctx.lineTo(0, y + size - slope * .5)
      ctx.closePath()
      ctx.fill()
    },
    wireframeTunnel(ctx, particle, time) {
      const depth = wrap(particle.phase / turn + time * .18)
      const size = (.12 + depth * .86) ** 2
      const width = env.width * size * .76
      const height = env.height * size * .62
      const x = env.width * .5 + env.wind * depth
      const y = env.height * .5
      const bevel = Math.min(width, height, env.size) * .25
      ctx.globalAlpha = Math.sin(Math.PI * depth) * .43
      ctx.beginPath()
      ctx.moveTo(x - width + bevel, y - height)
      ctx.lineTo(x + width - bevel, y - height)
      ctx.lineTo(x + width, y - height + bevel)
      ctx.lineTo(x + width, y + height - bevel)
      ctx.lineTo(x + width - bevel, y + height)
      ctx.lineTo(x - width + bevel, y + height)
      ctx.lineTo(x - width, y + height - bevel)
      ctx.lineTo(x - width, y - height + bevel)
      ctx.closePath()
      ctx.stroke()
    },
    magneticArcs(ctx, particle, time) {
      const { x, y } = position(particle, time, -.004, .012)
      const radius = env.size * (1 + particle.depth)
      const angle = time * .34 + particle.phase
      ctx.globalAlpha = .28 + particle.depth * .28
      ctx.lineWidth = Math.max(1, env.size / 24)
      ctx.beginPath()
      ctx.ellipse(x, y, radius, radius * .53, angle, -.78, 1.34)
      ctx.stroke()
      ctx.beginPath()
      ctx.ellipse(x, y, radius * .68, radius * .37, angle, 2.2, 4.3)
      ctx.stroke()
    },
    grainDrift(ctx, particle, time, index) {
      const { x, y } = position(particle, time, .012, .007)
      const grain = Math.max(.5, env.size * particle.depth)
      ctx.globalAlpha = .14 + particle.depth * .35
      for (let dot = 0; dot < 5; dot++) {
        const angle = particle.phase + dot * 2.4
        const spread = grain * (2 + dot) + Math.sin(index + dot + time * .48) * grain
        ctx.fillRect(x + Math.cos(angle) * spread, y + Math.sin(angle) * spread, grain, grain)
      }
    },
    waveParticles(ctx, particle, time) {
      const x = wrap(particle.x + time * (.035 + env.wind / 6000)) * env.width
      const frequency = 2 + Math.floor(particle.depth * 3)
      const amplitude = env.size * particle.depth
      const y = env.height * particle.y + Math.sin(particle.x * turn * frequency + time * .76 + particle.phase) * amplitude
      ctx.globalAlpha = .46 + particle.depth * .32
      ctx.beginPath()
      ctx.arc(x, y, Math.max(1, amplitude / 8), 0, turn)
      ctx.fill()
    },
  } satisfies Record<string, Painter>
}
