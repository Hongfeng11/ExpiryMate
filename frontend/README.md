# ExpiryMate 前端

前端应用单独位于本目录，采用 Vue 3 单文件组件、JavaScript ES Modules、Vite 5 和自定义 CSS。页面采用 PC 优先布局，同时支持手机窄屏；目前实现总览、物品列表、搜索/筛选/排序、新增/编辑/删除、标记已处理和提醒预览。

## 启动

```powershell
npm install
npm run dev
```

打开 Vite 输出的本地地址（默认 `http://localhost:5173`）。生产构建：

```powershell
npm run build
npm run preview
```

## 数据模式

默认使用浏览器 `localStorage` 的演示数据，适合在后端尚未就绪时体验界面。需要接入独立后端时，复制 `.env.example` 为 `.env.local`，配置：

```dotenv
VITE_API_BASE_URL=http://localhost:8080
```

API 模式当前约定使用 SRS 中的 `/api/v1/items` 查询、新建、更新和删除接口，并以浏览器同源或允许 credentials 的 CORS 配置承载会话。后端尚未建立；登录、权限、真实定时通知和 Web Push 需要后端配合完成。

## 主要目录

```text
src/
├─ App.vue              # PC 首页与物品录入交互
├─ main.js              # Vue 应用入口
├─ styles.css           # 视觉系统与响应式规则
└─ services/items.js    # API / 本地演示数据适配层
```

## 说明

演示模式的数据保存在当前浏览器。清除站点数据会清空演示内容。当前筛选为前端演示；API 模式的分页和后端错误结构需与实际服务契约联调。
