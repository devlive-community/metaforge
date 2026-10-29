<script setup lang="ts">
withDefaults(defineProps<{
    size?: number
    /** False keeps the assembled mark visible after loading finishes. */
    loading?: boolean
}>(), {
    size: 24,
    loading: true,
})
</script>

<template>
    <span
        class="mf-loading-icon"
        :class="{ 'mf-loading-icon--complete': !loading }"
        :style="{ '--mf-loading-size': `${size}px` }"
        aria-hidden="true"
    >
        <span class="mf-loading-icon__ghost" />
        <span
            v-for="part in ['left', 'right', 'ribbon', 'spark']"
            :key="part"
            class="mf-loading-icon__piece"
            :class="`mf-loading-icon__${part}`"
        />
    </span>
</template>

<style scoped>
.mf-loading-icon {
    --mf-loading-mark: url('../assets/metaforge-mark.webp');
    position: relative;
    display: inline-block;
    width: var(--mf-loading-size);
    height: var(--mf-loading-size);
    flex-shrink: 0;
    vertical-align: middle;
    perspective: calc(var(--mf-loading-size) * 3);
    isolation: isolate;
}

.mf-loading-icon__piece,
.mf-loading-icon__ghost {
    position: absolute;
    inset: 0;
    background: currentColor;
    -webkit-mask-image: var(--mf-loading-mark);
    mask-image: var(--mf-loading-mark);
    -webkit-mask-size: 100% 100%;
    mask-size: 100% 100%;
    -webkit-mask-repeat: no-repeat;
    mask-repeat: no-repeat;
    pointer-events: none;
}

.mf-loading-icon__ghost {
    opacity: .065;
}

.mf-loading-icon__piece {
    backface-visibility: hidden;
    animation-duration: var(--mf-loading-cycle, 2.6s);
    animation-timing-function: var(--mf-easing);
    animation-iteration-count: infinite;
    animation-fill-mode: both;
}

.mf-loading-icon__left {
    clip-path: polygon(0 0, 52% 0, 52% 35%, 32% 46%, 32% 100%, 0 100%);
    transform-origin: 18% 56%;
    animation-name: mf-loading-fold-left;
    z-index: 2;
}

.mf-loading-icon__ribbon {
    clip-path: polygon(32.5% 45.8%, 54% 32%, 78% 12%, 100% 12%, 100% 27.5%, 49.9% 59.1%, 49.9% 100%, 32.5% 100%);
    transform-origin: 62% 52%;
    animation-name: mf-loading-fold-ribbon;
    z-index: 3;
}

.mf-loading-icon__right {
    clip-path: polygon(71.5% 45.2336%, 100% 27.5%, 100% 100%, 71.5% 100%);
    transform-origin: 82% 66%;
    animation-name: mf-loading-fold-right;
    z-index: 1;
}

.mf-loading-icon__spark {
    clip-path: inset(55% 29% 24% 51%);
    transform-origin: 61.5% 65.5%;
    animation-name: mf-loading-fold-spark;
    z-index: 4;
}

.mf-loading-icon--complete .mf-loading-icon__piece {
    animation: none;
    opacity: 1;
    transform: none;
}

.mf-loading-icon--complete .mf-loading-icon__ghost {
    opacity: 0;
}

@keyframes mf-loading-fold-left {
    0%, 3% { transform: translate(-18%, 9%) rotateY(-100deg) rotateZ(-8deg); opacity: 0; }
    17%, 62% { transform: translate(0, 0) rotateY(0) rotateZ(0); opacity: 1; }
    82%, 100% { transform: translate(-10%, 16%) rotateY(100deg) rotateZ(-8deg); opacity: 0; }
}

@keyframes mf-loading-fold-ribbon {
    0%, 12% { transform: translate(16%, -14%) rotateY(105deg) rotateZ(10deg); opacity: 0; }
    32%, 68% { transform: translate(0, 0) rotateY(0) rotateZ(0); opacity: 1; }
    88%, 100% { transform: translate(-14%, -12%) rotateY(-100deg) rotateZ(-10deg); opacity: 0; }
}

@keyframes mf-loading-fold-right {
    0%, 23% { transform: translate(20%, 8%) rotateY(100deg) rotateZ(8deg); opacity: 0; }
    43%, 74% { transform: translate(0, 0) rotateY(0) rotateZ(0); opacity: 1; }
    97%, 100% { transform: translate(18%, 10%) rotateY(-100deg) rotateZ(8deg); opacity: 0; }
}

@keyframes mf-loading-fold-spark {
    0%, 34% { transform: scale(0) rotate(-90deg); opacity: 0; }
    47%, 75% { transform: scale(1) rotate(0); opacity: 1; }
    93%, 100% { transform: scale(0) rotate(90deg); opacity: 0; }
}

@media (prefers-reduced-motion: reduce) {
    .mf-loading-icon__piece {
        animation: none;
        opacity: 1;
        transform: none;
    }
}
</style>
