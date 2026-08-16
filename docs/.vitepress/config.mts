import { defineConfig } from 'vitepress'

const REPO = 'https://github.com/OpenVdra/RacksPlugin'
const MODRINTH = 'https://modrinth.com/plugin/racksplugin'
const SITE = 'https://openvdra.github.io/RacksPlugin'

// Pages relative to /docs/ or /vi/docs/, matching the on-disk layout. Both locales
// share this list; only the sidebar labels differ.
const pages = [
  'getting-started', 'using-racks', 'wood-variants',
  'commands', 'permissions',
  'configuration', 'language', 'migration', 'protections'
]

const enSidebar = [
  {
    text: 'Getting Started',
    items: [
      { text: 'Installation', link: '/docs/getting-started' },
      { text: 'Using Racks', link: '/docs/using-racks' },
      { text: 'Wood Variants', link: '/docs/wood-variants' }
    ]
  },
  {
    text: 'Server Setup',
    items: [
      { text: 'Commands', link: '/docs/commands' },
      { text: 'Permissions', link: '/docs/permissions' },
      { text: 'Configuration', link: '/docs/configuration' },
      { text: 'Language', link: '/docs/language' },
      { text: 'Protection Plugins', link: '/docs/protections' },
      { text: 'Coming from the Data Pack', link: '/docs/migration' }
    ]
  }
]

const viSidebar = [
  {
    text: 'Bắt đầu',
    items: [
      { text: 'Cài đặt', link: '/vi/docs/getting-started' },
      { text: 'Cách dùng giá treo', link: '/vi/docs/using-racks' },
      { text: 'Các loại gỗ', link: '/vi/docs/wood-variants' }
    ]
  },
  {
    text: 'Thiết lập máy chủ',
    items: [
      { text: 'Lệnh', link: '/vi/docs/commands' },
      { text: 'Quyền', link: '/vi/docs/permissions' },
      { text: 'Cấu hình', link: '/vi/docs/configuration' },
      { text: 'Ngôn ngữ', link: '/vi/docs/language' },
      { text: 'Plugin bảo vệ đất', link: '/vi/docs/protections' },
      { text: 'Chuyển từ data pack', link: '/vi/docs/migration' }
    ]
  }
]

const enSidebarMap = {
  ...Object.fromEntries(pages.map(page => [`/docs/${page}`, enSidebar])),
  '/docs/changelog': [{ text: 'Changelog', items: [{ text: 'Release history', link: '/docs/changelog' }] }],
  '/docs/': enSidebar
}

const viSidebarMap = {
  ...Object.fromEntries(pages.map(page => [`/vi/docs/${page}`, viSidebar])),
  '/vi/docs/changelog': [{ text: 'Nhật ký thay đổi', items: [{ text: 'Lịch sử phát hành', link: '/vi/docs/changelog' }] }],
  '/vi/docs/': viSidebar
}

export default defineConfig({
  title: 'Racks',
  description: 'Adds wooden display racks for tools and weapons. A Paper and Folia port of KawaMood\'s Racks data pack.',
  // GitHub project pages are served from /RacksPlugin/. Point a custom domain at the site
  // (add public/CNAME) and change this back to '/'.
  base: '/RacksPlugin/',
  cleanUrls: true,
  head: [
    // `head` entries are emitted verbatim, so `base` is NOT prepended the way it is for
    // themeConfig.logo and markdown links. The href has to carry the base itself or the
    // favicon resolves to the domain root on a project site and 404s.
    ['link', { rel: 'icon', type: 'image/png', href: '/RacksPlugin/favicon.png' }],
    ['link', { rel: 'apple-touch-icon', href: '/RacksPlugin/logo.png' }],
    // Social share preview when a docs link is pasted into Discord, Twitter and friends.
    ['meta', { property: 'og:image', content: `${SITE}/banner.png` }],
    ['meta', { name: 'twitter:card', content: 'summary_large_image' }],
    ['meta', { name: 'twitter:image', content: `${SITE}/banner.png` }]
  ],
  themeConfig: {
    // Shared across every locale; per-locale nav, sidebar and editLink live under each
    // entry in `locales` below and are deep-merged over these.
    logo: '/favicon.png',

    externalLinkIcon: true,

    socialLinks: [
      { icon: 'github', link: REPO },
      { icon: 'modrinth', link: MODRINTH }
    ],

    search: {
      provider: 'local'
    }
  },

  // i18n. English is served from the root, Vietnamese mirrors it under /vi/. The content
  // lives in `vi/` mirroring the root structure; the Vue components are registered
  // globally so both languages reuse them as-is.
  locales: {
    root: {
      label: 'English',
      lang: 'en',
      themeConfig: {
        nav: [
          { text: 'Docs', link: '/docs/getting-started', activeMatch: '^/docs/(?!changelog)' },
          { text: 'Changelog', link: '/docs/changelog', activeMatch: '^/docs/changelog' },
          { text: 'Download', link: '/docs/getting-started#download' },
          { component: 'LanguageDropdown' }
        ],

        sidebar: enSidebarMap,

        editLink: {
          pattern: `${REPO}/edit/main/docs/:path`,
          text: 'Edit this page on GitHub'
        }
      }
    },

    vi: {
      label: 'Tiếng Việt',
      lang: 'vi',
      description: 'Trưng bày công cụ và vũ khí trên giá treo tường hoặc giá đặt sàn. Bản chuyển Paper và Folia của data pack Racks by KawaMood.',
      themeConfig: {
        nav: [
          { text: 'Tài liệu', link: '/vi/docs/getting-started', activeMatch: '^/vi/docs/(?!changelog)' },
          { text: 'Thay đổi', link: '/vi/docs/changelog', activeMatch: '^/vi/docs/changelog' },
          { text: 'Tải về', link: '/vi/docs/getting-started#tai-ve' },
          { component: 'LanguageDropdown' }
        ],

        sidebar: viSidebarMap,

        editLink: {
          pattern: `${REPO}/edit/main/docs/:path`,
          text: 'Chỉnh sửa trang này trên GitHub'
        },

        // VitePress does not translate its own UI strings from `lang` alone.
        outlineTitle: 'Trên trang này',
        docFooter: {
          prev: 'Trang trước',
          next: 'Trang sau'
        },
        lastUpdatedText: 'Cập nhật lần cuối',
        returnToTopLabel: 'Về đầu trang',
        sidebarMenuLabel: 'Menu',
        darkModeSwitchLabel: 'Giao diện',
        lightModeSwitchTitle: 'Chuyển sang giao diện sáng',
        darkModeSwitchTitle: 'Chuyển sang giao diện tối'
      }
    }
  }
})
