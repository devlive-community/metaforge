<template>
  <div class="dc-footer">
    <div class="dc-footer__inner">
      <div class="dc-footer__brand">
        <MfAvatar src="/static/images/logo.png" alt="MetaForge Logo"/>
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
          <GithubOutlined :style="{ fontSize: '18px' }"/>
        </a>
      </div>
      <p class="dc-footer__copyright">Copyright &copy; 2022 MetaForge. All rights reserved.</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { GithubOutlined } from '@ant-design/icons-vue'
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

.dc-footer__copyright {
    font-size: 12px;
    color: #8c8c8c;
    margin: 0;
}
</style>
