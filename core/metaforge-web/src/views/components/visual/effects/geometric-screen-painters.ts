import type { Painter, Position, ScreenEnvironment } from './screen-effect-painters'

const turn = Math.PI * 2
const wrap = (value: number) => value - Math.floor(value)

export function createGeometricScreenPainters(env: ScreenEnvironment, position: Position) {
  return {
    fallingFeathers(ctx, particle, time) {
      const { x, y } = position(particle, time, .041, .058)
      const size = env.size * particle.depth
      ctx.translate(x, y)
      ctx.rotate(particle.phase + Math.sin(time + particle.phase) * .55)
      ctx.globalAlpha = .44 + .24 * particle.depth
      ctx.beginPath()
      ctx.moveTo(0, size)
      ctx.bezierCurveTo(-size * .8, size * .1, -size * .34, -size * .62, 0, -size)
      ctx.bezierCurveTo(size * .65, -size * .43, size * .55, size * .2, 0, size)
      ctx.fill()
      ctx.globalAlpha = .8
      ctx.beginPath()
      ctx.moveTo(0, size * 1.35)
      ctx.quadraticCurveTo(-size * .16, size * .35, 0, -size * .88)
      ctx.stroke()
    },
    prismShards(ctx, particle, time) {
      const { x, y } = position(particle, time, -.018, .034)
      const size = env.size * particle.depth
      const tilt = particle.phase + time * particle.spin * .46
      ctx.translate(x, y)
      ctx.rotate(tilt)
      ctx.globalAlpha = .25 + .25 * particle.depth
      ctx.beginPath()
      ctx.moveTo(-size, size * .53)
      ctx.lineTo(size * .88, size * .31)
      ctx.lineTo(size * .19, -size)
      ctx.closePath()
      ctx.fill()
      ctx.globalAlpha = .55
      ctx.stroke()
    },
    concentricWaves(ctx, particle, time) {
      const { x, y } = position(particle, time, 0, 0)
      ctx.lineWidth = 1 + particle.depth
      for (let ring = 0; ring < 3; ring++) {
        const phase = wrap(particle.phase / turn + time * .28 + ring / 3)
        ctx.globalAlpha = (1 - phase) * .36
        ctx.beginPath()
        ctx.arc(x, y, env.size * (.1 + phase) * (1 + particle.depth), 0, turn)
        ctx.stroke()
      }
    },
    signalBars(ctx, particle, time) {
      const x = wrap(particle.x + env.wind * time / 8000) * env.width
      const bottom = env.height * (.86 + .12 * particle.y)
      const height = env.size * (.2 + .8 * Math.sin(time * .9 + particle.phase) ** 2)
      const width = Math.max(1, Math.min(7, env.size / 10))
      ctx.globalAlpha = .24 + particle.depth * .32
      ctx.fillRect(x, bottom - height, width, height)
      ctx.globalAlpha = .75
      ctx.fillRect(x, bottom - height, width, 2)
    },
    isometricMesh(ctx, particle, time) {
      const halfWidth = Math.max(6, env.size)
      const halfHeight = halfWidth * .54
      const x = Math.round(particle.x * env.width / halfWidth) * halfWidth
      const y = Math.round(particle.y * env.height / halfHeight) * halfHeight
      const lifted = Math.sin(time * .6 + particle.phase) * halfHeight * .4 + env.wind / 15
      ctx.globalAlpha = .16 + .24 * particle.depth
      ctx.beginPath()
      ctx.moveTo(x - halfWidth, y + lifted)
      ctx.lineTo(x, y - halfHeight + lifted)
      ctx.lineTo(x + halfWidth, y + lifted)
      ctx.lineTo(x, y + halfHeight + lifted)
      ctx.closePath()
      ctx.stroke()
      ctx.beginPath()
      ctx.moveTo(x, y + halfHeight + lifted)
      ctx.lineTo(x, y + halfHeight * 1.8 + lifted)
      ctx.stroke()
    },
    wovenRibbons(ctx, particle, time, index) {
      const baseline = particle.y * env.height
      const amplitude = env.size * particle.depth
      ctx.globalAlpha = .18 + particle.depth * .22
      ctx.lineWidth = Math.max(1, env.size / 14)
      ctx.beginPath()
      for (let step = 0; step <= 36; step++) {
        const x = env.width * step / 36
        const angle = step / 4 + particle.phase + time * (index % 2 ? .48 : -.48)
        const y = baseline + Math.sin(angle + env.wind / 40) * amplitude
        if (step === 0) ctx.moveTo(x, y)
        else ctx.lineTo(x, y)
      }
      ctx.stroke()
    },
    constellationMarks(ctx, particle, time) {
      const { x, y } = position(particle, time, -.01, .017)
      const radius = env.size * particle.depth
      const nodes = [0, 1, 2, 3].map(index => {
        const angle = particle.phase + index * 1.34 + time * particle.spin * .16
        return [x + Math.cos(angle) * radius, y + Math.sin(angle) * radius * .74]
      })
      ctx.globalAlpha = .26
      ctx.beginPath()
      nodes.forEach(([x, y], index) => index === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y))
      ctx.stroke()
      ctx.globalAlpha = .7
      for (const [x, y] of nodes) {
        ctx.beginPath()
        ctx.arc(x, y, 1 + particle.depth * 1.4, 0, turn)
        ctx.fill()
      }
    },
    spiralDust(ctx, particle, time) {
      const { x, y } = position(particle, time, .009, .022)
      for (let dot = 0; dot < 8; dot++) {
        const angle = time * .54 + particle.phase + dot * .62
        const radius = env.size * (dot + 1) / 8
        ctx.globalAlpha = (dot + 1) * particle.depth / 12
        ctx.beginPath()
        ctx.arc(x + Math.cos(angle) * radius, y + Math.sin(angle) * radius, 1 + dot * .2, 0, turn)
        ctx.fill()
      }
    },
    horizonGlow(ctx, particle, time) {
      const x = wrap(particle.x + time * env.wind / 10000) * env.width
      const y = env.height * (.69 + particle.y * .24)
      const radius = env.size * (1 + particle.depth)
      const gradient = ctx.createRadialGradient(x, y, 0, x, y, radius)
      gradient.addColorStop(0, env.color + '85')
      gradient.addColorStop(1, env.color + '00')
      ctx.fillStyle = gradient
      ctx.globalAlpha = .23 + Math.sin(time * .25 + particle.phase) ** 2 * .2
      ctx.fillRect(x - radius, y - radius, radius * 2, radius * 2)
    },
    mosaicFade(ctx, particle, time) {
      const size = Math.max(4, env.size)
      const x = Math.floor(wrap(particle.x + time * env.wind / 14000) * env.width / size) * size
      const y = Math.floor(particle.y * env.height / size) * size
      ctx.globalAlpha = Math.sin(time * .38 + particle.phase) ** 2 * particle.depth * .52
      ctx.fillRect(x + 1, y + 1, size - 2, size - 2)
    },
  } satisfies Record<string, Painter>
}
