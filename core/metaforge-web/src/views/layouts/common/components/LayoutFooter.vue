<template>
  <div class="dc-footer">
    <div class="dc-footer__inner">
      <div class="dc-footer__brand">
        <img src="/static/images/logo.png" alt="" width="48" height="48" class="dc-brand-logo"/>
        <div class="dc-footer__company">MetaForge</div>
        <p class="dc-footer__desc">
          MetaForge is integrated software for data transformation, integration, and visualization.
          Support a variety of data sources, file types, big data related database, relational database, NoSQL database, etc.
        </p>
      </div>

      <div class="dc-footer__links">
        <div v-for="group in links" :key="group.title" class="dc-footer__group">
          <div class="dc-footer__title">{{ group.title }}</div>
          <a v-for="link in group.links"
             :key="link.label"
             class="dc-footer__link"
             :href="link.href"
             target="_blank"
             rel="noopener noreferrer">
            {{ link.label }}
          </a>
        </div>
      </div>
    </div>

    <div class="dc-footer__bottom">
      <div class="dc-footer__socials">
        <a v-for="social in socials"
           :key="social.label"
           :href="social.href"
           target="_blank"
           rel="noopener noreferrer">
          <svg class="dc-footer__social-icon" viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden="true">
            <path d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12"/>
          </svg>
        </a>
      </div>
      <p class="dc-footer__copyright">Copyright &copy; 2022 MetaForge. All rights reserved.</p>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'LayoutFooter' })

const links = [
  {
    title: 'Resources',
    links: [
      { label: 'Blog', href: 'https://metaforge.devlive.org/' },
      { label: 'Gitee', href: 'https://gitee.com/devlive-community/metaforge' },
      { label: 'Github', href: 'https://github.com/devlive-community/metaforge' },
      { label: 'Documentation', href: 'https://metaforge.devlive.org/' }
    ]
  },
  {
    title: 'Community',
    links: [
      { label: 'Website', href: 'https://metaforge.devlive.org/' },
      { label: 'Issues', href: 'https://github.com/devlive-community/metaforge/issues' },
      { label: 'Discussions', href: 'https://github.com/devlive-community/metaforge/discussions' },
      { label: 'Community', href: 'https://community.devlive.org' }
    ]
  },
  {
    title: 'Projects',
    links: [
      { label: 'Database Tools', href: 'https://dbm.devlive.org' },
      { label: 'Open AI Java SDK', href: 'https://openai-java-sdk.devlive.org' },
      { label: 'InfoSphere', href: 'https://infosphere.devlive.org' }
    ]
  }
]
const socials = [
  { icon: 'Github', href: 'https://github.com/devlive-community/metaforge', label: 'GitHub' }
]
</script>

<style scoped>
.dc-footer {
    border-top: 1px solid var(--dc-border, rgba(0, 0, 0, 0.06));
    padding: 32px 24px 16px;
    margin-top: 24px;
}

.dc-footer__inner {
    display: flex;
    flex-wrap: wrap;
    gap: 32px;
    max-width: 1200px;
    margin: 0 auto;
}

.dc-footer__brand {
    flex: 1 1 280px;
    min-width: 240px;
}

.dc-footer__company {
    font-weight: 600;
    margin-top: 8px;
}

.dc-footer__desc {
    font-size: 12px;
    color: #8c8c8c;
    margin-top: 8px;
    line-height: 1.6;
}

.dc-footer__links {
    display: flex;
    flex-wrap: wrap;
    gap: 48px;
    flex: 2 1 480px;
}

.dc-footer__group {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.dc-footer__title {
    font-weight: 600;
    margin-bottom: 4px;
}

.dc-footer__link {
    font-size: 13px;
    color: #8c8c8c;
    text-decoration: none;
}

.dc-footer__link:hover {
    color: var(--dc-primary);
}

.dc-footer__bottom {
    max-width: 1200px;
    margin: 24px auto 0;
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;
    border-top: 1px solid var(--dc-border, rgba(0, 0, 0, 0.06));
    padding-top: 12px;
}

.dc-footer__socials {
    display: flex;
    align-items: center;
    gap: 12px;
}

.dc-footer__social-icon {
    display: block;
    color: #8c8c8c;
}

.dc-footer__socials a:hover .dc-footer__social-icon {
    color: var(--dc-primary);
}

.dc-footer__copyright {
    font-size: 12px;
    color: #8c8c8c;
    margin: 0;
}
</style>
