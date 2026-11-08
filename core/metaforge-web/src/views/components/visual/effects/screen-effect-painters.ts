import { createAmbientScreenPainters } from './ambient-screen-painters'
import { createGeometricScreenPainters } from './geometric-screen-painters'

export interface ScreenParticle { x: number; y: number; depth: number; phase: number; spin: number }
export interface ScreenEnvironment { readonly width: number; readonly height: number; readonly size: number; readonly wind: number; readonly color: string; readonly customColor: string }
export type Position = (particle: ScreenParticle, time: number, vertical: number, sway?: number) => { x: number; y: number }
export type Painter = (context: CanvasRenderingContext2D, particle: ScreenParticle, time: number, index: number) => void
const wrap = (value: number) => (value % 1 + 1) % 1
export function createAdditionalScreenPainters(env: ScreenEnvironment, position: Position) {
  function mist(ctx: CanvasRenderingContext2D, x: number, y: number, radius: number, alpha: number) {
    const gradient = ctx.createRadialGradient(x, y, 0, x, y, radius)
    gradient.addColorStop(0, env.color + '60'); gradient.addColorStop(1, env.color + '00')
    ctx.globalAlpha = alpha; ctx.fillStyle = gradient
    ctx.beginPath(); ctx.arc(x, y, radius, 0, Math.PI * 2); ctx.fill()
  }
  const painters = {
    ...createAmbientScreenPainters(env, position),
    ...createGeometricScreenPainters(env, position),
    matrixGrid(ctx, p, time) {
      const horizon=env.height*.35,vanish=env.width*(.5+env.wind/600),depth=wrap(p.y+time*.1),y=horizon+depth*depth*(env.height-horizon)
      ctx.globalAlpha=.15+p.depth*.35;ctx.lineWidth=Math.max(.5,env.size*p.depth)
      ctx.beginPath();ctx.moveTo(vanish,horizon);ctx.lineTo(p.x*env.width*1.6-env.width*.3,env.height);ctx.moveTo(0,y);ctx.lineTo(env.width,y);ctx.stroke()
    },
    searchBeams(ctx,p,time) {
      const x=p.x*env.width,angle=Math.sin(time*.4+p.phase)*.8,reach=env.height*1.4,spread=env.size/350
      const gradient=ctx.createLinearGradient(x,env.height,x+Math.sin(angle)*reach,env.height-Math.cos(angle)*reach)
      gradient.addColorStop(0,env.color);gradient.addColorStop(1,'rgba(0,0,0,0)');ctx.fillStyle=gradient;ctx.globalAlpha=.12+p.depth*.08
      ctx.beginPath();ctx.moveTo(x,env.height);ctx.lineTo(x+Math.sin(angle-spread)*reach,env.height-Math.cos(angle-spread)*reach);ctx.lineTo(x+Math.sin(angle+spread)*reach,env.height-Math.cos(angle+spread)*reach);ctx.closePath();ctx.fill()
    },
    floatingHexagons(ctx,p,time) {
      const at=position(p,time,-.025,.02),radius=env.size*p.depth,rotation=p.phase+time*p.spin*.2
      ctx.globalAlpha=.15+p.depth*.4;ctx.lineWidth=Math.max(.6,env.size/16);ctx.beginPath()
      for(let i=0;i<6;i++){const angle=rotation+i*Math.PI/3,x=at.x+Math.cos(angle)*radius,y=at.y+Math.sin(angle)*radius;if(i===0)ctx.moveTo(x,y);else ctx.lineTo(x,y)}ctx.closePath();ctx.stroke()
    },
    digitalScan(ctx,p,time,index) {
      const scan=wrap(.25+time*.08)*env.height,band=env.size*10
      if(index===0){const gradient=ctx.createLinearGradient(0,scan-band,0,scan+band);gradient.addColorStop(0,'rgba(0,0,0,0)');gradient.addColorStop(.5,env.color);gradient.addColorStop(1,'rgba(0,0,0,0)');ctx.fillStyle=gradient;ctx.globalAlpha=.12;ctx.fillRect(0,scan-band,env.width,band*2)}
      const distance=Math.abs(p.y*env.height-scan);if(distance>band)return
      ctx.globalAlpha=(1-distance/band)*p.depth;ctx.fillStyle=env.color;ctx.font=`${Math.max(8,env.size)}px monospace`;ctx.fillText((index*31%256).toString(16).padStart(2,'0').toUpperCase(),p.x*env.width,p.y*env.height)
    },
    waterCaustics(ctx, p, time) {
      const { x, y } = position(p, time, .015, .015)
      const radius = env.size * (1 + p.depth * 1.6)
      ctx.globalAlpha = .13 + .2 * p.depth
      ctx.lineWidth = Math.max(.7, env.size / 65)
      ctx.shadowColor = env.color
      ctx.shadowBlur = Math.min(8, env.size / 12)
      ctx.beginPath()
      for (let index = 0; index <= 32; index++) {
        const angle = index * Math.PI / 16
        const swell = 1 + .16 * Math.sin(3 * angle + time * .65 + p.phase) + .13 * Math.cos(5 * angle - time * .9)
        const px = x + Math.cos(angle) * radius * swell
        const py = y + Math.sin(angle) * radius * swell * .58
        if (index === 0) ctx.moveTo(px, py)
        else ctx.lineTo(px, py)
      }
      ctx.closePath()
      ctx.stroke()
    },
    lightTrails(ctx, p, time) {
      const reach = env.size * (2 + p.depth * 3)
      const x = wrap(p.x + time * (.09 + env.wind / 1800) * (1 + p.depth)) * (env.width + reach) - reach / 2
      const y = p.y * env.height
      const curve = (offset: number) => y + Math.sin(offset / Math.max(1, env.width) * 5 + p.phase) * Math.min(env.height * .09, env.size)
      const gradient = ctx.createLinearGradient(x - reach, 0, x, 0)
      gradient.addColorStop(0, env.color + '00')
      gradient.addColorStop(.8, env.color + '77')
      gradient.addColorStop(1, env.color)
      ctx.globalAlpha = .45 + .5 * p.depth
      ctx.strokeStyle = gradient
      ctx.lineWidth = Math.max(.6, env.size / 19 * p.depth)
      ctx.beginPath()
      ctx.moveTo(x - reach, curve(x - reach))
      for (let step = 1; step <= 10; step++) {
        const px = x - reach + reach * step / 10
        ctx.lineTo(px, curve(px))
      }
      ctx.stroke()
    },
    pulseGrid(ctx, p, time) {
      const step = Math.max(8, env.size)
      const x = Math.floor(p.x * env.width / step) * step
      const y = Math.floor(p.y * env.height / step) * step
      for (let row = -1; row <= 1; row++) for (let column = -1; column <= 1; column++) {
        const distance = Math.hypot(row, column)
        const phase = time * (.7 + p.depth * .4) - distance + p.phase + env.wind * column / 130
        ctx.globalAlpha = .08 + (.5 + .5 * Math.sin(phase)) ** 3 * .62 / (1 + distance)
        ctx.lineWidth = 1
        ctx.strokeRect(x + column * step + 2, y + row * step + 2, step - 4, step - 4)
        if (distance === 0) {
          ctx.globalAlpha *= .4
          ctx.fillRect(x + 2, y + 2, step - 4, step - 4)
        }
      }
    },
    contourLines(ctx, p, time) {
      const base = p.y * env.height
      const amplitude = env.size * (.55 + p.depth)
      const shift = time * (.12 + env.wind / 1100)
      ctx.globalAlpha = .14 + p.depth * .34
      ctx.lineWidth = Math.max(.6, env.size / 42)
      ctx.beginPath()
      for (let step = 0; step <= 48; step++) {
        const x = step / 48
        const fold = Math.sin(x * 7 + shift) * .65 + Math.sin(x * 13 - shift + base / 120) * .35
        const y = base + fold * amplitude
        if (step === 0) ctx.moveTo(0, y)
        else ctx.lineTo(x * env.width, y)
      }
      ctx.stroke()
    },
    fog(ctx, p, time) { mist(ctx, wrap(p.x + time * env.wind / 5000) * env.width, p.y * env.height, env.size * (1 + p.depth), .5) },
    smoke(ctx, p, time) {
      const at = position(p, time, -.04, .035)
      mist(ctx, at.x, at.y, env.size * (1.5 - at.y / env.height), .5 + at.y / env.height * .3)
    },
    clouds(ctx, p, time) {
      const x = wrap(p.x + time * (.008 + env.wind / 10000)) * env.width, y = p.y * env.height
      for (const offset of [-.6, 0, .6]) mist(ctx, x + offset * env.size, y + Math.abs(offset) * env.size * .2, env.size * .8, .6)
    },
    sandstorm(ctx, p, time) {
      const x = wrap(p.x + time * (.12 + env.wind / 1000) * p.depth) * env.width
      const y = wrap(p.y + Math.sin(time + p.phase) * .025) * env.height
      ctx.globalAlpha = p.depth; ctx.fillRect(x, y, env.size * p.depth * 2, Math.max(.6, env.size * .35))
    },
    embers(ctx, p, time) {
      const at = position(p, time, -.07, .035), r = env.size * p.depth
      ctx.globalAlpha = .3 + Math.sin(time + p.phase) ** 2 * .7
      ctx.beginPath(); ctx.ellipse(at.x, at.y, r * .5, r * 1.4, p.spin, 0, Math.PI * 2); ctx.fill()
    },
    leaves(ctx, p, time, index) {
      const at = position(p, time, .055, .06), size = env.size * p.depth
      ctx.fillStyle = env.customColor || ['#71A87A', '#B6AD65', '#B38859'][index % 3]
      ctx.translate(at.x, at.y); ctx.rotate(p.phase + time * p.spin)
      ctx.beginPath(); ctx.moveTo(-size, 0); ctx.quadraticCurveTo(0, -size, size, 0); ctx.quadraticCurveTo(0, size, -size, 0); ctx.fill()
      ctx.globalAlpha = .45; ctx.beginPath(); ctx.moveTo(-size, 0); ctx.lineTo(size, 0); ctx.stroke()
    },
    hearts(ctx, p, time) {
      const at = position(p, time, -.045, .03), size = env.size * p.depth
      ctx.translate(at.x, at.y); ctx.rotate(Math.sin(time + p.phase) * .2); ctx.scale(size / 10, size / 10)
      ctx.beginPath(); ctx.moveTo(0, 8); ctx.bezierCurveTo(-18, -2, -9, -15, 0, -6); ctx.bezierCurveTo(9, -15, 18, -2, 0, 8); ctx.fill()
    },
    sparkles(ctx, p, time) {
      const x = p.x * env.width, y = p.y * env.height, size = env.size * p.depth
      ctx.globalAlpha = .2 + Math.sin(time * .6 + p.phase) ** 2 * .8
      ctx.beginPath(); ctx.moveTo(x, y - size); ctx.quadraticCurveTo(x, y, x + size, y); ctx.quadraticCurveTo(x, y, x, y + size)
      ctx.quadraticCurveTo(x, y, x - size, y); ctx.quadraticCurveTo(x, y, x, y - size); ctx.fill()
    },
    lightning(ctx, p, time) {
      const phase = wrap(time / 9 + p.phase / (Math.PI * 2))
      if (phase < .08 || phase > .15) return
      ctx.globalAlpha = Math.sin((phase - .08) / .07 * Math.PI) * .55
      ctx.lineWidth = env.size; ctx.shadowColor = env.color; ctx.shadowBlur = 8
      ctx.beginPath()
      for (let index = 0; index <= 7; index++) {
        const x = (p.x + Math.sin(index * 3 + p.phase) * .04) * env.width, y = index / 7 * env.height * (.4 + p.depth * .4)
        if (index === 0) ctx.moveTo(x, y)
        else ctx.lineTo(x, y)
      }
      ctx.stroke()
    },
    fireworks(ctx, p, time, index) {
      const phase = wrap(time / 6 + p.phase / (Math.PI * 2))
      const x = p.x * env.width, y = (.15 + p.y * .45) * env.height
      ctx.strokeStyle = env.customColor || ['#E2AF6D', '#D797B7', '#77B5DD', '#89C9A6'][index % 4]
      if (phase < .2) {
        const current = env.height - phase / .2 * (env.height - y)
        ctx.globalAlpha = .6; ctx.beginPath(); ctx.moveTo(x, current); ctx.lineTo(x, current + 18); ctx.stroke(); return
      }
      if (phase > .75) return
      const age = (phase - .2) / .55, radius = age * env.size * 30
      ctx.globalAlpha = 1 - age; ctx.lineWidth = Math.max(1, env.size * .3)
      for (let ray = 0; ray < 18; ray++) {
        const angle = ray * Math.PI / 9
        ctx.beginPath(); ctx.moveTo(x + Math.cos(angle) * radius * .75, y + Math.sin(angle) * radius * .75 + age * age * 25)
        ctx.lineTo(x + Math.cos(angle) * radius, y + Math.sin(angle) * radius + age * age * 25); ctx.stroke()
      }
    },
    risingDigits(ctx, p, time, index) {
      const at = position(p, time, .065, 0)
      const size = Math.max(8, env.size * p.depth)
      ctx.font = `${size}px monospace`
      for (let trail = 0; trail < 5; trail++) {
        ctx.globalAlpha = p.depth * (1 - trail / 5)
        ctx.fillText(String((index + trail) % 10), at.x, at.y - trail * size * 1.1)
      }
    },
    warpStars(ctx, p, time) {
      const depth = .08 + wrap(p.depth - time * .18) * .92
      const x = env.width / 2 + (p.x - .5) * env.width / depth, y = env.height / 2 + (p.y - .5) * env.height / depth
      ctx.globalAlpha = 1 - depth * .7; ctx.lineWidth = Math.max(.6, env.size * (1 - depth))
      ctx.beginPath(); ctx.moveTo(x, y); ctx.lineTo(env.width / 2 + (x - env.width / 2) * .9, env.height / 2 + (y - env.height / 2) * .9); ctx.stroke()
    },
    oceanWaves(ctx, p, time, index) {
      const base = env.height * (.55 + index / 30), amplitude = env.size * p.depth
      const gradient = ctx.createLinearGradient(0, base - amplitude, 0, env.height)
      gradient.addColorStop(0, env.color + '80'); gradient.addColorStop(1, env.color + '10')
      ctx.fillStyle = gradient; ctx.globalAlpha = .45
      ctx.beginPath(); ctx.moveTo(0, env.height)
      for (let step = 0; step <= 50; step++) ctx.lineTo(env.width * step / 50, base + Math.sin(step / 6 + time * .5 + p.phase + env.wind / 50) * amplitude)
      ctx.lineTo(env.width, env.height); ctx.closePath(); ctx.fill()
    },
    sunRays(ctx, p, time) {
      const angle = p.phase * .5 + time * .015, spread = Math.min(.18, env.size / 600)
      const radius = Math.hypot(env.width, env.height)
      ctx.translate(env.width / 2, -30)
      const gradient = ctx.createRadialGradient(0, 0, 0, 0, 0, radius)
      gradient.addColorStop(0, env.color + 'A0'); gradient.addColorStop(1, env.color + '00')
      ctx.fillStyle = gradient; ctx.globalAlpha = .4
      ctx.beginPath(); ctx.moveTo(0, 0); ctx.arc(0, 0, radius, angle - spread, angle + spread); ctx.closePath(); ctx.fill()
    },
  } satisfies Record<string, Painter>
  return painters
}
