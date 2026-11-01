<template>
  <!-- A stacked database with a chart card and a code card, drawn in the primary color. -->
  <svg viewBox="0 0 320 260" class="h-auto w-full max-w-80" role="img" :aria-label="$t('auth.brand.illustration')">
    <defs>
      <linearGradient id="auth-disk" x1="0" y1="0" x2="1" y2="0">
        <stop offset="0" stop-color="var(--color-primary)" stop-opacity="0.55"/>
        <stop offset="0.5" stop-color="var(--color-primary)" stop-opacity="0.25"/>
        <stop offset="1" stop-color="var(--color-primary)" stop-opacity="0.6"/>
      </linearGradient>
      <linearGradient id="auth-top" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="var(--color-primary)" stop-opacity="0.18"/>
        <stop offset="1" stop-color="var(--color-primary)" stop-opacity="0.4"/>
      </linearGradient>
      <linearGradient id="auth-base" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="var(--color-surface)"/>
        <stop offset="1" stop-color="var(--color-primary)" stop-opacity="0.12"/>
      </linearGradient>
    </defs>
    <!-- platform -->
    <path d="M160 168 L292 206 L160 246 L28 206 Z" fill="url(#auth-base)" stroke="var(--color-primary)" stroke-opacity="0.25"/>
    <path d="M28 206 L28 214 L160 254 L292 214 L292 206 L160 246 Z" fill="var(--color-primary)" fill-opacity="0.2"/>
    <ellipse cx="160" cy="206" rx="104" ry="30" fill="none" stroke="var(--color-primary)" stroke-opacity="0.35" stroke-width="3" stroke-dasharray="60 18"/>
    <!-- database stack -->
    <g v-for="(y, index) in [150, 112, 74]" :key="y">
      <path :d="`M100 ${ y } L100 ${ y + 30 } A60 18 0 0 0 220 ${ y + 30 } L220 ${ y } Z`" fill="url(#auth-disk)"/>
      <ellipse cx="160" :cy="y" rx="60" ry="18" :fill="index === 2 ? 'url(#auth-top)' : 'var(--color-primary)'" :fill-opacity="index === 2 ? 1 : 0.3"/>
    </g>
    <ellipse cx="160" cy="74" rx="60" ry="18" fill="none" stroke="var(--color-surface)" stroke-opacity="0.8"/>
    <!-- chart card -->
    <g transform="translate(222 40)">
      <rect width="78" height="62" rx="10" fill="var(--color-surface)" stroke="var(--color-primary)" stroke-opacity="0.25"/>
      <rect x="12" y="38" width="9" height="14" rx="2" fill="var(--color-primary)" fill-opacity="0.45"/>
      <rect x="27" y="28" width="9" height="24" rx="2" fill="var(--color-primary)" fill-opacity="0.6"/>
      <rect x="42" y="20" width="9" height="32" rx="2" fill="var(--color-primary)" fill-opacity="0.75"/>
      <rect x="57" y="12" width="9" height="40" rx="2" fill="var(--color-primary)"/>
    </g>
    <!-- code card -->
    <g transform="translate(150 150)">
      <rect width="66" height="50" rx="10" fill="var(--color-surface)" stroke="var(--color-primary)" stroke-opacity="0.25"/>
      <path d="M22 17 L13 25 L22 33 M44 17 L53 25 L44 33 M37 14 L29 36" fill="none" stroke="var(--color-primary)" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/>
    </g>
  </svg>
</template>
