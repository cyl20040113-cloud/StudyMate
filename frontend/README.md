# StudyMate 前端

Vue 3 + TypeScript + Vite + Vant 前端。

完整介绍、环境配置、数据库初始化、启动及验证步骤见 [根目录 README](../README.md)。

~~~powershell
npm ci
npm run dev
~~~

开发环境的 /api 代理到 http://localhost:8080。

~~~powershell
npx --no-install vue-tsc -b
npm run build
~~~

生产构建输出为 dist，部署时另行配置 /api 反向代理。
