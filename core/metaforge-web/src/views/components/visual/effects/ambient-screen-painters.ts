import type { Painter, Position, ScreenEnvironment } from './screen-effect-painters'

const turn = Math.PI * 2
const wrap = (value: number) => value - Math.floor(value)

export function createAmbientScreenPainters(env: ScreenEnvironment, position: Position) {
  return {
    rainRipples(ctx, particle, time) {
      const phase = wrap(particle.phase / turn + time * .48)
      const radius = Math.max(1, env.size * (.12 + phase) * particle.depth)
      const x = wrap(particle.x + env.wind * time / 7000) * env.width
      const y = particle.y * env.height
      ctx.lineWidth = Math.max(.5, particle.depth * 1.8)
      ctx.globalAlpha = (1 - phase) * .52
      ctx.beginPath()
      ctx.ellipse(x, y, radius, radius * .35, 0, 0, turn)
      ctx.moveTo(x + radius * .72, y)
      ctx.ellipse(x, y, radius * .72, radius * .24, 0, 0, turn)
      ctx.stroke()
    },
    pollenDrift(ctx, particle, time) {
      const { x, y } = position(particle, time, -.022, .034)
      const radius = Math.max(1, env.size * particle.depth * (.65 + .35 * Math.sin(particle.phase + time * .5) ** 2))
      ctx.globalAlpha = .45 + particle.depth * .32
      ctx.shadowColor = env.color
      ctx.shadowBlur = radius * 1.8
      ctx.beginPath()
      ctx.arc(x, y, radius, 0, turn)
      ctx.fill()
    },
    orbitalTrails(ctx, particle, time) {
      const { x, y } = position(particle, time, .006, .015)
      const radius = env.size * (1 + particle.depth)
      const angle = time * (.45 + particle.depth * .35) + particle.phase
      ctx.globalAlpha = .23
      ctx.lineWidth = 1
      ctx.beginPath()
      ctx.ellipse(x, y, radius, radius * .35, particle.spin, 0, turn)
      ctx.stroke()
      ctx.translate(x, y)
      ctx.rotate(particle.spin)
      ctx.globalAlpha = .7
      ctx.beginPath()
      ctx.arc(Math.cos(angle) * radius, Math.sin(angle) * radius * .35, 2 + particle.depth, 0, turn)
      ctx.fill()
    },
    floatingLanterns(ctx, particle, time) {
      const { x, y } = position(particle, time, -.035, .022)
      const size = Math.max(1, env.size * particle.depth)
      const gradient = ctx.createRadialGradient(x, y, 0, x, y, size * 2.8)
      gradient.addColorStop(0, env.color + '75')
      gradient.addColorStop(1, env.color + '00')
      ctx.fillStyle = gradient
      ctx.fillRect(x - size * 2.8, y - size * 2.8, size * 5.6, size * 5.6)
      ctx.fillStyle = env.color
      ctx.globalAlpha = .68
      ctx.beginPath()
      ctx.ellipse(x, y, size * .72, size, 0, 0, turn)
      ctx.fill()
      ctx.globalAlpha = .9
      ctx.fillRect(x - size * .36, y + size * .68, size * .72, Math.max(1, size * .18))
    },
    sonarSweeps(ctx, particle, time) {
      const { x, y } = position(particle, time, .004, .01)
      const radius = env.size * (1 + particle.depth * 1.8)
      const angle = particle.phase + time * .9
      ctx.lineWidth = 1
      ctx.globalAlpha = .2
      ctx.beginPath()
      ctx.arc(x, y, radius, 0, turn)
      ctx.stroke()
      ctx.globalAlpha = .12
      ctx.beginPath()
      ctx.moveTo(x, y)
      ctx.arc(x, y, radius, angle - .9, angle)
      ctx.closePath()
      ctx.fill()
      ctx.globalAlpha = .6
      ctx.beginPath()
      ctx.moveTo(x, y)
      ctx.lineTo(x + Math.cos(angle) * radius, y + Math.sin(angle) * radius)
      ctx.stroke()
    },
    shimmerCurtain(ctx, particle, time) {
      const x = wrap(particle.x + env.wind * time / 9000) * env.width
      const span = env.size * (1 + particle.depth)
      const sway = Math.sin(time * .48 + particle.phase) * span
      const gradient = ctx.createLinearGradient(x - span, 0, x + span, 0)
      gradient.addColorStop(0, env.color + '00')
      gradient.addColorStop(.5, env.color + '94')
      gradient.addColorStop(1, env.color + '00')
      ctx.fillStyle = gradient
      ctx.globalAlpha = .16 + particle.depth * .24
      ctx.beginPath()
      ctx.moveTo(x - span, 0)
      ctx.quadraticCurveTo(x + sway - span, env.height * .45, x - span, env.height)
      ctx.lineTo(x + span, env.height)
      ctx.quadraticCurveTo(x + sway + span, env.height * .45, x + span, 0)
      ctx.closePath()
      ctx.fill()
    },
    paperKites(ctx, particle, time) {
      const { x, y } = position(particle, time, -.028, .052)
      const size = env.size * particle.depth
      ctx.translate(x, y)
      ctx.rotate(Math.sin(time * .9 + particle.phase) * .48)
      ctx.globalAlpha = .62
      ctx.beginPath()
      ctx.moveTo(0, -size)
      ctx.lineTo(size * .72, -.08 * size)
      ctx.lineTo(0, size * 1.25)
      ctx.lineTo(-size * .72, -.08 * size)
      ctx.closePath()
      ctx.fill()
      ctx.globalAlpha = .48
      ctx.beginPath()
      ctx.moveTo(0, size * 1.25)
      ctx.quadraticCurveTo(Math.cos(time + particle.phase) * size, size * 2, -size * .25, size * 3.1)
      ctx.stroke()
    },
    inkBloom(ctx, particle, time) {
      const { x, y } = position(particle, time, .009, .029)
      const radius = env.size * (1.1 + particle.depth + .35 * Math.sin(time * .35 + particle.phase))
      const gradient = ctx.createRadialGradient(x, y, 0, x, y, radius * 1.2)
      gradient.addColorStop(0, env.color + 'B3')
      gradient.addColorStop(1, env.color + '00')
      ctx.fillStyle = gradient
      ctx.globalAlpha = .26
      ctx.beginPath()
      for (let index = 0; index <= 40; index++) {
        const angle = turn * index / 40
        const edge = radius * (1 + .12 * Math.sin(angle * 5 + particle.phase + time * .52))
        const px = x + Math.cos(angle) * edge
        const py = y + Math.sin(angle) * edge * .75
        if (index === 0) ctx.moveTo(px, py)
        else ctx.lineTo(px, py)
      }
      ctx.closePath()
      ctx.fill()
    },
  } satisfies Record<string, Painter>
}
