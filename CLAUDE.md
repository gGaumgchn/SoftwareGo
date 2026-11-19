# CLAUDE.md — 实训室选拔系统

> 本文件是本仓库的**架构与开发硬约束**。修改代码前先读本文件；实现业务功能前，先读对应的需求文档与原型页面。

## 1. 项目定位与权威文档

软件学院实训室选拔系统，面向 **学生、导师、负责人** 三类角色，覆盖：登录、主页面、公告查看、个人待办、实训室信息、志愿填报、分轮补录、拟录取确认、发放拟录取、阶段管理。

选拔主流程：

```
集中宣讲 → 实训室开放日 → 第一轮录取 → 第二轮录取 → 第三轮录取 → 统筹调剂 → 正式入组
```

每轮录取包含：**志愿填报 → 线下面试 → 拟录取确认**。

### 1.1 文档权威顺序

冲突时按以下顺序取准：

1. `docs/requirements/`：业务规则与验收标准的唯一来源。**未经明确要求不得修改。**
2. `原型图/`：页面结构、交互与视觉参考。**只读**；`frontend/` 是唯一实现副本。
3. `docs/design/`：接口、数据库、架构设计文档，随代码同步维护。
4. `CLAUDE.md`：技术栈、工程规范、安全与数据权限约束。技术决策以本文件为准。

需求有歧义、代码与需求冲突、或需要新增业务字段/流程时，**先停下来问用户，不要自行发明业务规则**。

### 1.2 范围边界（不要越界实现）

- 不提供注册、密码登录、找回密码。
- “人脸比对”是**模拟操作**：只做前端本地格式校验与状态切换，不调用摄像头、不上传/存储人脸数据。
- 学工号只校验 `^\d{10}$`，不查询账号是否存在、不校验身份是否匹配。
- 公告**只读**；不提供公告新增、编辑、删除入口。公告只能由阶段管理保存时自动生成。
- 统筹调剂线下进行，线上**只展示结果**；不实现学生确认、导师接收等线上调剂流程。
- “发放拟录取”只在系统内生成拟录取记录；不实现短信、邮件、站内推送。
- 没有第四轮填报；不实现宣讲、开放日、面试的线上排期/签到。
- 原型中的“顶栏角色切换”“预览已录取状态”“预览暂无待办”等是**演示开关**，不得进入正式前端；切换身份必须退出重新登录。
- `background.jpg` 是流程参考图，不作为页面背景资源。

## 2. 技术栈

| 层 | 选型 |
| --- | --- |
| 前端 | Vue 3 + Vite、Vue Router 4、Pinia、Axios；原生 CSS（沿用原型深色科幻主题）；ESLint + Prettier；Node ≥ 18 |
| 后端 | Java 17、Spring Boot 3.3.x、Maven 3.9+、**原生 MyBatis（XML）**、Jakarta Validation、Lombok |
| 数据库 | MySQL 8.0+、InnoDB、utf8mb4 |
| 鉴权 | **HttpSession + Cookie**（不引入 Spring Security，使用 `HandlerInterceptor` 做会话校验） |
| 接口 | REST、`/api/v1`、JSON、统一响应 `{code, message, data}`，成功码 **200** |
| 文件 | PDF 简历存本地磁盘，存放在 Web 根目录之外，通过鉴权接口下载 |
| 时间 | 一律以**服务端时间**为准，时区 `Asia/Shanghai` |

## 3. 目录结构

```
SoftwareGo/
├── CLAUDE.md
├── README.md
├── background.jpg                 # 流程参考图，不用于页面
├── backend/                       # Spring Boot 后端
│   ├── pom.xml
│   ├── src/main/java/com/softwarego/selection/
│   │   ├── SelectionApplication.java
│   │   ├── common/                # Result、PageResult、ErrorCode、BusinessException、GlobalExceptionHandler
│   │   ├── config/                # WebMvcConfig、AuthInterceptor、JacksonConfig、UploadProperties
│   │   ├── auth/                  # AuthController、AuthService、LoginRequest、SessionUser、CurrentUser
│   │   ├── user/                  # User、UserMapper、UserService
│   │   ├── stage/                 # 阶段配置与当前阶段
│   │   ├── announcement/          # 公告只读查询 + 阶段保存时的公告生成
│   │   ├── lab/                   # 实训室信息（导师维护、学生查看）
│   │   ├── volunteer/             # 志愿填报、分轮补录、申请目标
│   │   ├── offer/                 # 拟录取、确认、超时放弃、最终录取
│   │   ├── admission/             # 导师发放拟录取、分数、名额
│   │   ├── file/                  # PDF 上传与鉴权下载
│   │   └── todo/                  # 个人待办（实时推导，不落表）
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-local.yml  # 本地数据库凭据，Git 忽略
│   │   ├── mapper/                # MyBatis XML，按模块建子目录
│   │   └── db/
│   │       ├── schema.sql         # 建表/索引
│   │       └── data.sql           # 仅 dev 的演示数据
│   └── uploads/resumes/           # Git 忽略
├── frontend/                      # Vue 3 前端
│   ├── package.json
│   ├── vite.config.js
│   ├── src/
│   │   ├── main.js
│   │   ├── App.vue
│   │   ├── api/                   # request.js + 各模块 API 函数
│   │   ├── router/index.js        # 路由 + meta.roles 守卫
│   │   ├── stores/                # Pinia，如 user.js
│   │   ├── layouts/               # DefaultLayout、LoginLayout
│   │   ├── views/
│   │   │   ├── LoginView.vue
│   │   │   ├── HomeView.vue
│   │   │   ├── AnnouncementView.vue
│   │   │   ├── TodoView.vue
│   │   │   ├── student/           # LabsView、VolunteerView、OfferView
│   │   │   ├── teacher/           # LabInfoView、AdmissionView
│   │   │   └── manager/           # StageView
│   │   ├── components/            # AppSidebar、AppHeader、BaseModal、ToastHost、StatusBadge、StepBar、Countdown、FileUpload、EmptyState
│   │   ├── styles/                # variables.css、base.css、components.css
│   │   └── utils/                 # time.js、validate.js、format.js
│   └── public/                    # favicon、字体等静态资源
├── docs/
│   ├── requirements/              # 需求文档、用户故事、学生旅途（只读）
│   ├── design/                    # api.md、db-schema.md、architecture.md
│   └── 原型图/                    # 原型 HTML（只读）
```

## 4. 启动、构建与环境

### 4.1 本地开发

```bash
# 后端（默认 8080）
cd backend
mvn spring-boot:run

# 前端（默认 5173）
cd frontend
npm install
npm run dev
```

前端通过 Vite 代理访问后端，保持同源，Cookie 才能正常携带：

```js
// vite.config.js
server: {
  port: 5173,
  proxy: {
    '/api': { target: 'http://localhost:8080', changeOrigin: true }
  }
}
```

### 4.2 生产构建

```bash
cd frontend && npm run build     # 产物 frontend/dist
cd backend  && mvn clean package
```

生产环境优先**同源部署**：Nginx 托管 `frontend/dist`，将 `/api` 反向代理到后端；这样 Session Cookie 的 `SameSite=Lax` 可正常工作。不要默认前后端跨域部署。

### 4.3 配置

- 数据库、上传目录、会话超时通过环境变量或 `application-local.yml` 配置。
- `application-local.yml`、`.env.local` 不得提交。
- 不在代码中硬编码数据库密码、绝对路径、种子账号。
- dev 环境才加载 `db/data.sql` 演示数据；生产环境禁止自动灌入演示数据。

## 5. 后端架构约定

### 5.1 分层

每个模块内部按 `Controller / Service / Mapper / domain / dto` 组织（小项目不强制 Service 接口 + Impl，直接类即可）。

- **Controller**：只做 HTTP 参数接收、`@Valid` 校验、调用 Service、返回 `Result`。不写业务规则。
- **Service**：业务规则、事务、数据归属校验、状态机。所有写操作的事务边界在这里。
- **Mapper**：只写 SQL。复杂/动态 SQL 放 XML；简单查询可用注解。
- **domain**：数据库实体，字段与表列对应。
- **dto**：请求/响应对象。**Controller 不直接返回实体**，避免泄露内部字段。

### 5.2 编码规则

- 构造器注入（`@RequiredArgsConstructor`），禁止 `@Autowired` 字段注入。
- 使用 `@Transactional` 包裹多表写操作；只在 `RuntimeException` 时回滚，业务异常统一继承 `BusinessException`。
- 统一异常处理：`@RestControllerAdvice` 捕获参数校验、业务异常、未登录、越权、未知异常；Controller 内不写 try/catch 兜底。
- 时间统一通过可注入的 `Clock` 获取，禁止在业务代码中散落 `LocalDateTime.now()`，便于测试截止逻辑。
- 日志使用 SLF4J；记录登录、提交志愿、发放、确认、阶段保存等关键操作，包含 `userId/role`，不记录 Session ID、简历全文等敏感内容。
- 枚举值存 `VARCHAR(32)`，Java 侧用 enum；不使用 MySQL `ENUM`。
- 金额、名额等数值字段用明确的整型；分数允许小数时用 `DECIMAL(5,2)`，不要用 `DOUBLE`。

### 5.3 MyBatis

- 参数一律 `#{}`；**禁止 `${}`**（白名单排序字段除外，且必须枚举校验）。
- 查询显式列出字段，禁止生产 SQL 使用 `SELECT *`。
- Mapper XML 的 `namespace` 与接口全限定名一致；关联查询用 `resultMap`。
- 分页统一 `LIMIT #{offset}, #{size}` + 独立 count 查询；不引入 PageHelper，除非用户同意。
- 动态条件用 `<if>` / `<foreach>`，不拼接 SQL 字符串。

### 5.4 会话鉴权

- `POST /api/v1/auth/login`：校验 `role ∈ {STUDENT, TEACHER, MANAGER}`、`staffNo` 匹配 `^\d{10}$`；校验通过后**销毁旧会话并新建会话**（防会话固定），写入：

```java
SessionUser { Long userId; String staffNo; String name; Role role; }
```

- `AuthInterceptor` 拦截 `/api/v1/**`，放行 `POST /auth/login` 与健康检查；其余请求必须存在会话。
- 路径级角色约束：
  - `/api/v1/student/**` → `STUDENT`
  - `/api/v1/teacher/**` → `TEACHER`
  - `/api/v1/manager/**` → `MANAGER`
- 路径级角色只是第一道闸；**数据归属必须在 Service 层再校验**。
- 当前用户通过 `CurrentUser` 参数解析器或 `SessionUserHolder` 获取，**绝不从请求参数/请求体读取 `userId`、`role`、`studentId`、`teacherId` 作为身份依据**。
- `POST /auth/logout` 使会话失效；前端清理用户状态并回登录页。
- 会话配置：`HttpOnly=true`、`SameSite=Lax`、`Path=/`、生产环境 `Secure=true`、超时 30 分钟（可配置）。
- CSRF/CORS：本地用 Vite 代理、生产同源部署，配合 `SameSite=Lax` 与 JSON 请求作为基线。若必须跨域，CORS 只允许**精确来源**并开启 `allowCredentials`，禁止 `*`；若后续引入 Spring Security，必须同时启用 CSRF token。

## 6. 前端架构约定

### 6.1 技术规则

- 全部使用 Vue 3 **Composition API + `<script setup>`**，不使用 Options API。
- 路由使用 `meta.roles` 声明可访问角色；全局守卫在首次进入或刷新时调用 `GET /auth/me` 恢复会话，未登录跳 `/login?redirect=...`，角色不符跳 `/home` 或提示“无权访问”。
- **不把 `localStorage` 中的角色/学工号当作鉴权依据**。原型用 `localStorage.sel_role` 仅为演示；正式前端以服务端 Session 为准，Pinia 只做展示缓存。
- Axios 实例统一在 `src/api/request.js`：
  - `baseURL: '/api/v1'`
  - `withCredentials: true`
  - 响应 `code !== 200` 时统一弹 Toast；HTTP 401 清理用户状态并跳登录；HTTP 403 提示“无权访问该数据”；网络异常提示“网络异常，请重试”。
- 组件内不直接调用 Axios；所有请求封装在 `src/api/*.js`，函数命名 `getXxx / createXxx / updateXxx / confirmXxx`。
- Pinia 使用 setup store；`user` store 保存当前用户与角色，提供 `fetchMe()`、`logout()`。
- 页面必须处理 **加载中 / 空状态 / 加载失败** 三态。加载失败不能显示为“暂无数据”。
- 表单交互统一遵循：初始禁用 → 满足条件启用 → 提交中禁用防重复 → 失败保留已填内容。
- 倒计时以服务端时间为基准：接口返回 `serverTime`，前端计算本地偏移；不要只依赖浏览器时钟。
- 中文 UI 文案，错误提示与需求文档保持一致（见 §12.4）。
- 提交代码前移除 `console.log`；不要在生产页面写死 mock 数据，演示数据只放在 dev 环境或 `data.sql`。

### 6.2 视觉与组件

- 主题变量从原型 `:root` 提取到 `frontend/src/styles/variables.css`，至少保留：

```css
--bg:#070b16; --bg2:#0b1222; --panel:#0f1729; --panel2:#131f35;
--line:rgba(56,189,248,.13); --line2:rgba(56,189,248,.3);
--cyan:#22d3ee; --blue:#38bdf8;
--text:#e8eef9; --text2:#9baabd; --text3:#5d6c83;
--green:#34d399; --amber:#fbbf24; --red:#f87171;
--font:'Noto Sans SC', ...; --tech:'Rajdhani', ...;
```

- 布局沿用原型：左侧栏 238px + 顶栏 + 卡片式内容区；按 1920×1080 设计，窄屏可纵向滚动，不重构视觉风格。
- 字体使用 `Noto Sans SC`（正文）与 `Rajdhani`（数字/倒计时）；优先自托管到 `frontend/public/fonts`，不直连 Google Fonts，保留系统字体回退。
- 图标使用原型中的内联 SVG；不引入图标库。
- 不引入 Element Plus、Ant Design Vue、Naive UI 等 UI 库，除非用户明确同意。按钮、弹窗、Toast、上传区、状态徽标、步骤条、倒计时均按原型自建组件。
- 颜色、圆角、阴影、间距一律使用 CSS 变量或既有类名，不在模板中散落新的硬编码色值。
- 公共布局、侧边栏菜单、顶部用户信息必须复用 `layouts/DefaultLayout.vue` 与 `components/AppSidebar.vue`，不要每个页面复制一份。

### 6.3 原型到 Vue 的映射

| 原型文件 | 路由 | Vue 视图 | 角色 |
| --- | --- | --- | --- |
| `登录.html` | `/login` | `LoginView.vue` | 公开 |
| `主页面.html` | `/home` | `HomeView.vue` | 全部 |
| `公告.html` | `/announcements` | `AnnouncementView.vue` | 全部 |
| `个人待办.html` | `/todos` | `TodoView.vue` | 全部 |
| `查看实训室信息.html` | `/student/labs` | `student/LabsView.vue` | 学生 |
| `志愿填报.html` | `/student/volunteer` | `student/VolunteerView.vue` | 学生 |
| `拟录取确认.html` | `/student/offers` | `student/OfferView.vue` | 学生 |
| `提交实训室信息.html` | `/teacher/lab` | `teacher/LabInfoView.vue` | 导师 |
| `发放拟录取.html` | `/teacher/admissions` | `teacher/AdmissionView.vue` | 导师 |
| `阶段管理.html` | `/manager/stages` | `manager/StageView.vue` | 负责人 |

- 原型是视觉/交互参考，不是运行时依赖；不得 iframe 引入或运行时链接原型文件。
- 每个页面先对齐原型的结构、文案、状态与禁用逻辑，再接后端；不要先做一套通用后台模板再套页面。

### 6.4 角色菜单

- 学生：主页面、公告、查看实训室信息、志愿填报、拟录取确认、个人待办。
- 导师：主页面、公告、实训室信息、申请与录取管理、个人待办。
- 负责人：主页面、公告、阶段管理、个人待办。
- 菜单按会话角色渲染；当前项高亮；不要实现原型顶栏的角色切换。

## 7. 鉴权与数据权限

### 7.1 身份定义

- 身份键为 **`(staffNo, role)`**：同一学工号以不同角色登录会形成两个独立身份和数据空间。这是“身份以所选为准、不根据学工号推断”的直接结果，不要尝试自动推断或合并角色。
- 首次登录自动创建 `sys_user` 记录（`staff_no + role` 唯一），`name` 可为空；**不得因账号不存在而拒绝登录**。
- 姓名来源为 `sys_user.name`（可由导入/种子数据补充）；为空时页面显示学工号。不要在登录页新增姓名输入。
- 会话中保存 `userId, staffNo, name, role`；前端展示信息来自 `GET /auth/me`。

### 7.2 数据归属矩阵

| 数据 | 学生 | 导师 | 负责人 |
| --- | --- | --- | --- |
| 阶段配置 / 当前阶段 | 只读 | 只读 | 读写 |
| 公告 | 只读 | 只读 | 只读（仅能在阶段保存时生成） |
| 实训室公开信息 | 只读全部 | 只读全部；仅可改本人 | 只读 |
| 实训室导师简历 | 只读 | 本人可替换 | 只读 |
| 本人志愿/补录记录 | 读写本人 | 不可访问他人志愿 | 不可访问 |
| 本人学生简历 | 读写本人 | 仅可读投递到本人的申请简历 | 不可访问 |
| 面试分数 / 录取操作 | 不可访问 | 仅本人名下申请 | 不可访问 |
| 本人拟录取 / 最终录取 | 只读本人 | 仅本人发放的记录 | 不可访问 |
| 个人待办 | 本人 | 本人 | 本人 |

- “只读全部”的公开目录类数据（阶段、公告、实训室信息）不属于个人数据，但仍需登录后访问。
- 任何个人记录查询的 SQL 必须带当前用户条件，例如 `WHERE student_id = #{currentUserId}` 或 `WHERE teacher_id = #{currentUserId}`。
- 通过 URL/请求体传入的资源 ID，必须先查归属再操作。越权返回 HTTP 403 + `code=40301`；不要返回“操作成功”空结果。
- 文件下载同样校验：导师简历所有已登录用户可读；学生简历仅本人和收到该申请的导师可读。

### 7.3 未登录 / 越权处理

- 无会话：HTTP 401 + `code=40101`，前端清空用户状态并跳登录。
- 角色不符或数据不属于当前用户：HTTP 403 + `code=40301`，前端提示“无权访问该数据”，不跳登录。
- 资源不存在：HTTP 404 + `code=40401`。
- 不要向普通用户暴露内部异常、SQL、堆栈。

## 8. 接口规范

### 8.1 通用约定

- 前缀：`/api/v1`。
- 角色专属接口使用角色前缀：`/student/**`、`/teacher/**`、`/manager/**`；公共接口：`/auth/**`、`/announcements/**`、`/labs/**`、`/files/**`、`/todos/**`。
- 资源用复数名词，动作用 HTTP 方法或子资源表达，例如 `POST /student/offers/{id}/confirm`。
- 请求/响应使用 DTO；日期时间格式 `yyyy-MM-dd'T'HH:mm:ss`（无时区后缀，统一 `Asia/Shanghai`）。
- 分页参数：`page` 从 1 开始，默认 1；`pageSize` 默认 10，最大 100。分页数据放在 `data` 内：

```json
{ "list": [], "total": 0, "page": 1, "pageSize": 10 }
```

- 参数校验使用 Jakarta Validation；错误信息中文，返回第一条可读错误。
- 写操作必须幂等或防重复：按钮禁用只是 UX，服务端必须用唯一索引/事务/状态判断兜底。

### 8.2 统一响应

成功：

```json
{ "code": 200, "message": "success", "data": { } }
```

失败：

```json
{ "code": 40301, "message": "无权访问该数据", "data": null }
```

错误码分段：

| code | 含义 | HTTP |
| --- | --- | --- |
| 200 | 成功 | 200 |
| 40001 | 参数/格式校验失败 | 200 |
| 40101 | 未登录或会话过期 | 401 |
| 40301 | 无权限 / 越权访问 | 403 |
| 40401 | 资源不存在 | 404 |
| 40901 | 状态冲突（重复确认、名额已满、已截止、重复发放等） | 200 |
| 50001 | 系统异常 | 500 |
| 50002 | 文件上传/存储失败 | 200 |

说明：业务校验/状态冲突类错误使用 HTTP 200 + 非 200 `code`，便于前端统一弹提示；认证/权限/资源不存在/未捕获异常使用对应 HTTP 状态码，响应体仍保持统一结构。

### 8.3 关键接口（契约以 `docs/design/api.md` 为准）

| 方法 | 路径 | 角色 | 说明 |
| --- | --- | --- | --- |
| POST | `/auth/login` | 公开 | 角色 + 学工号，创建会话 |
| POST | `/auth/logout` | 登录 | 注销会话 |
| GET | `/auth/me` | 登录 | 当前会话用户 |
| GET | `/stages/current` | 登录 | 当前阶段、起止、serverTime |
| GET | `/manager/stages` | 负责人 | 全部阶段配置 |
| PUT | `/manager/stages` | 负责人 | 保存阶段；可选发布公告 |
| POST | `/manager/stages/announcement` | 负责人 | 仅重试发布公告 |
| GET | `/announcements` | 登录 | 公告分页列表 |
| GET | `/announcements/{id}` | 登录 | 公告详情 |
| GET | `/labs` | 登录 | 实训室列表/筛选 |
| GET | `/labs/{id}` | 登录 | 实训室详情 |
| GET | `/teacher/lab` | 导师 | 本人实训室信息 |
| PUT | `/teacher/lab` | 导师 | 新增/更新本人信息 |
| POST | `/files/resumes` | 学生/导师 | 上传 PDF，返回 fileId |
| GET | `/files/{id}` | 按归属 | 鉴权下载简历 |
| GET | `/student/volunteers/current` | 学生 | 当前轮本人志愿 |
| POST | `/student/volunteers` | 学生 | 提交/重填本轮志愿 |
| GET | `/student/offers` | 学生 | 本人拟录取/最终录取 |
| POST | `/student/offers/{id}/confirm` | 学生 | 确认拟录取 |
| GET | `/student/admission` | 学生 | 最终录取结果 |
| GET | `/teacher/applications` | 导师 | 本人名下申请（按轮次） |
| PUT | `/teacher/applications/{targetId}/score` | 导师 | 录入/修改面试分数 |
| PUT | `/teacher/applications/{targetId}/order` | 导师 | 同分排序 |
| GET | `/teacher/admissions/summary` | 导师 | 名额概览 |
| POST | `/teacher/admissions/grant` | 导师 | 按分数与名额发放/递补 |
| GET | `/todos` | 登录 | 当前用户待办（服务端推导） |

新增/修改接口时，先在 `docs/design/api.md` 写清请求、响应、错误码、角色与归属规则，再写代码。

## 9. 数据库规范

- MySQL 8.0+、InnoDB、`utf8mb4`、排序规则 `utf8mb4_0900_ai_ci`。
- 表名、列名使用 snake_case、单数形式（如 `sys_user`、`volunteer_application`）。
- 主键统一 `id BIGINT UNSIGNED AUTO_INCREMENT`；外键关联列加索引。
- 每表包含 `created_at`、`updated_at`：`DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP` / `ON UPDATE CURRENT_TIMESTAMP`。
- 布尔用 `TINYINT(1)`；状态/角色用 `VARCHAR(32)` + Java enum，不用 MySQL `ENUM`。
- 默认使用**逻辑外键 + 索引**，不建物理外键；关联完整性在 Service 层保证。
- 不使用软删除；用状态字段表达作废/放弃。
- 唯一索引是并发安全的最后一道防线，至少包括：
  - `sys_user(staff_no, role)`
  - `lab_info(teacher_id)`
  - `volunteer_application(student_id, round_no)`
  - `application_target(application_id, teacher_id)`
  - `offer(teacher_id, round_no, student_id)`
  - `final_admission(student_id)`
- 核心表（详细字段见 `docs/design/db-schema.md`）：
  - `sys_user`：学工号、姓名、角色。
  - `stage`：固定阶段编码、名称、起止时间、排序；共 7 行（宣讲、开放日、第一/二/三轮、调剂、正式入组）。
  - `announcement`：标题、正文、发布时间、创建人；只增不改不删。
  - `lab_info`：导师本人实训室信息与计划名额。
  - `resume_file`：上传文件元数据与所有者。
  - `volunteer_application`：学生每轮志愿及两项调剂意愿。
  - `application_target`：志愿解析后的导师目标、面试分数、同分顺序。
  - `offer`：拟录取记录、状态、确认截止时间。
  - `final_admission`：最终录取（唯一学生），区分志愿录取/调剂录取。
- 名额、确认、发放相关写操作必须在事务中完成；必要时对 `lab_info` 或名额行 `SELECT ... FOR UPDATE`，并用唯一索引兜底，防止并发超发/重复确认。
- 建表/变更 SQL 放 `backend/src/main/resources/db/schema.sql`，并同步 `docs/design/db-schema.md`；不要只改本地数据库。
- dev 演示数据放 `db/data.sql`，生产环境不得自动执行。

## 10. 核心业务不变量（必须服务端强制）

1. **阶段**：名称与顺序固定，不可增删改；三轮录取分别配置；起止时间必填、开始早于结束、按流程顺序且不重叠；保存后立即生效；阶段间允许留空档；全部结束后显示“选拔已结束”，无配置显示“阶段暂未配置”。阶段调整不恢复已放弃的拟录取、不重新开放已截止的填报、不回滚已提交结果。
2. **公告**：只能由阶段保存时生成，每次保存最多一条；发布后不可修改/删除；不提供公告 CRUD 接口。阶段保存成功但公告失败时，阶段仍生效，且必须支持“只重试公告、不重复保存阶段、不重复生成公告”。
3. **登录**：角色必须选择；学工号仅格式校验；模拟比对不调用后端人脸服务；登录成功建立新会话；不提供注册/密码。
4. **志愿填报**：每轮 1–3 名导师、平行志愿、不可重复、至少 1 名；两项调剂意愿必选；PDF 简历必传；只允许在对应轮次时间内提交；已录取学生不可提交；同一轮申请产生面试分数或拟录取（含待确认拟录取）后不可重填；补录轮次所填导师必须仍有剩余名额；每轮记录独立保存，提交失败保留填写内容。
5. **导师姓名解析**：学生输入的是导师姓名，不是 ID。后端按 `lab_info.teacher_name` 去首尾空格、合并连续空格后精确匹配；未匹配或匹配到多条时拒绝提交并提示“导师姓名不存在或存在重名，请联系负责人”。前端仍只提供三个普通输入框，不提供搜索/候选。该规则用于把申请路由到导师本人；若产品要求允许自由文本，需先修改需求与数据模型。
6. **拟录取确认**：只能确认本人且未截止的拟录取；确认后不可更改，其余自动“已放弃”；只收到一个也必须主动确认；并发/重复请求只能产生一个结果；到截止时间未确认自动放弃；已确认的不受截止处理影响；调剂录取不提供确认。
7. **发放拟录取**：导师只处理本人名下、本轮申请；未评分不参与；按分数降序，同分需导师手动定序；分数 ≥ 分数线才可发放；名额 = 计划名额 −（已确认 + 待确认）；名额满即停，绝不超发；已放弃/超时/确认他处释放名额后可在截止前递补；同一导师同一轮同一学生不得重复发放；已有最终录取的学生不接收；截止后停止发放和递补。
8. **实训室信息**：仅导师本人可写；必填项完整；计划人数为正整数；修改后不得小于当前已占用名额（已确认 + 待确认）；更新原记录，不新增重复记录。
9. **待办**：由“当前阶段 + 用户状态”实时推导，不单独建表、不缓存过期结果；加载失败不能当成“暂无待办”。
10. **时间**：所有开放/截止判断以服务端时间为准；确认截止时间默认取本轮结束时间，发放时写入 `offer.confirm_deadline`；超时放弃由定时任务 + 读取/确认时惰性校正双重兜底。
11. **状态单向**：拟录取状态只允许 `PENDING → CONFIRMED` 或 `PENDING → ABANDONED`，不得从终态回退；阶段调整不触发回滚。
12. **调剂结果**：三轮结束后的调剂由线下确定，线上只读展示；当前版本不实现线上调剂操作页面，`final_admission` 的调剂记录通过线下结果导入（脚本/初始化），具体入口待需求确认。

## 11. 文件上传

- 仅接受 PDF：校验扩展名 `.pdf`、Content-Type `application/pdf`、文件头 `%PDF-`；大小上限默认 10 MB（可配置）。
- 存储路径：`backend/uploads/resumes/{yyyy}/{MM}/{uuid}.pdf`；数据库保存相对路径、原始文件名、大小、Content-Type、所有者、用途（学生简历/导师简历）。
- 文件名使用 UUID，禁止使用用户原始文件名作为磁盘名；防止路径穿越。
- **不要**把 `uploads/` 配置为静态资源目录；所有下载走 `GET /files/{id}`，在 Controller/Service 校验会话与归属。
- 上传接口返回 `fileId`；提交志愿/实训室信息时只传 `fileId`，服务端校验该文件属于当前用户且用途匹配。
- 替换简历时更新引用；旧文件若仍被历史记录引用则保留，孤立文件清理可后置，不在请求链路里做危险删除。
- 上传失败返回 `code=50002`，不产生半条数据库记录。
- `backend/uploads/` 加入 `.gitignore`。

## 12. 代码风格

### 12.1 Java

- 4 空格缩进、UTF-8；类 `PascalCase`、方法/变量 `camelCase`、常量 `UPPER_SNAKE_CASE`。
- 实体/DTO 使用 Lombok；不可变响应对象优先用 `record`。
- 业务异常统一 `BusinessException(int code, String message)`；错误码集中在 `ErrorCode` 枚举/常量。
- 请求 DTO 上用 `@NotBlank`、`@Pattern`、`@Min` 等；Service 仍要复核状态类规则（时间、名额、归属）。
- 早返回、少嵌套；单个方法聚焦一件事；避免魔法数字，改用枚举/常量。
- 注释用中文解释**业务原因**（如“待确认也占用名额”），不写“给变量赋值”这类废话。

### 12.2 SQL

- 关键字大写；子句分行；表别名明确。
- 不使用 `SELECT *`；不使用 `${}`；排序字段必须白名单校验。
- 所有列表查询写明排序，避免结果不稳定。

### 12.3 Vue / JavaScript

- 2 空格、单引号、分号、尾逗号；ESLint + Prettier 统一。
- 组件文件 `PascalCase.vue`；composable 命名 `useXxx`；store 命名 `useXxxStore`。
- `<script setup>` 中先声明 props/emits，再声明状态，最后声明方法。
- 模板中不写复杂表达式；格式化/判断放 `computed` 或 `utils`。
- 不直接操作 DOM；必须时用 `ref` 并限制在组件内。
- 不提交 `console.log`、调试用 `debugger`。
- 正式页面不硬编码演示数据；dev mock 必须通过环境变量显式开启。

### 12.4 关键提示文案（保持与需求一致）

| 场景 | 文案 |
| --- | --- |
| 学工号格式错误 | 学工号异常 |
| 非 PDF | 请上传 PDF 格式的简历 |
| 上传失败 | 简历上传失败，请重试 |
| 导师重复 | 导师姓名不能重复 |
| 导师无法匹配/重名 | 导师姓名不存在或存在重名，请联系负责人 |
| 通用提交失败 | 提交失败，请重试 |
| 确认已截止 | 确认时间已截止 |
| 已确认后再改 | 已确认录取，不可更改 |
| 确认失败 | 确认失败，请刷新查看结果后重试 |
| 名额满 | 名额已满 |
| 分数非法 | 请填写有效分数 |
| 同分未定序 | 请先确定同分学生顺序 |
| 无达标学生 | 暂无可发放的达标学生 |
| 发放已截止 | 本轮拟录取发放已截止 |
| 名额低于占用 | 计划招收人数不能小于当前已占用名额 |
| 补录未开始/已截止 | 补录填报尚未开始 / 补录填报已截止 |
| 补录无名额 | 本轮暂无可填报名额 |
| 所填导师无名额 | 所填导师已无剩余名额，请重新选择 |
| 已录取仍补录 | 你已确认录取，无需参加补录 |
| 已录取仍填报 | 已被录取！ |
| 本轮申请已处理仍重填 | 本轮志愿已进入处理阶段，不能再修改 |
| 阶段未配置/无公告 | 阶段暂未配置 / 暂无公告 |
| 加载失败 | 加载失败，请重试 |
| 阶段时间不完整 | 请填写完整的阶段时间 |
| 阶段起止错误 | 阶段结束时间必须晚于开始时间 |
| 阶段顺序/重叠 | 阶段时间必须按流程顺序设置且不能重叠 |
| 公告内容为空 | 公告内容不能为空 |
| 阶段保存成功 | 保存成功 |
| 阶段+公告成功 | 保存成功，公告已发布 |
| 公告发布失败 | 阶段已保存，公告发布失败，请重试 |
| 非负责人访问阶段管理 | 仅负责人可配置阶段 |
| 越权 | 无权访问该数据 |

## 13. 测试与验收

### 13.1 后端

- JUnit 5 + Spring Boot Test + MockMvc。
- 集成测试优先用 Testcontainers MySQL；环境不支持时使用独立的 `software_go_selection_test` 库，**禁止连生产库跑测试**。
- 必须覆盖：
  - 登录：格式合法/非法、未选角色、会话建立与注销。
  - 权限：学生/导师/负责人交叉访问、越权读写、文件越权下载。
  - 志愿：1–3 名、重复、空白、缺调剂意愿、非 PDF、非开放时间、已录取、补录无名额。
  - 确认：唯一确认、重复请求、截止后拒绝、超时自动放弃、已确认不受影响。
  - 发放：按分数、分数线、同分未定序、名额占满、释放后递补、重复发放、截止后拒绝。
  - 阶段：时间完整、顺序、重叠、保存生效、公告生成/失败重试、重复点击不重复生成。
  - 名额：并发发放/确认不超发、不重复。
- 关键并发点写集成测试；不要只靠手工点页面。

### 13.2 前端

- `npm run lint`、`npm run build` 必须通过。
- 工具函数、Pinia store、表单校验可用 Vitest 覆盖。
- 每个页面手工验收：加载中、空状态、失败状态、表单禁用/启用、提交防重复、Toast 文案、角色菜单、路由守卫。
- 视觉对照原型：配色、侧边栏宽度、卡片层级、字体、按钮状态一致。

### 13.3 提交前

```bash
cd backend && mvn test
cd frontend && npm run lint && npm run build
```

## 14. 文档与协作

- `docs/design/api.md`：接口清单、请求/响应示例、错误码、角色与归属规则。
- `docs/design/db-schema.md`：表结构、字段、索引、枚举、关系、并发约束。
- `docs/design/architecture.md`：模块划分、鉴权流程、阶段/录取状态机、时间与并发处理。
- 接口或表结构变更必须**同一次提交**更新对应文档；不要代码先行、文档后补。
- 不修改 `docs/requirements/` 与 `docs/原型图/`，除非用户明确要求；需求冲突先提问。
- Git：
  - 分支：`feat/<模块>`、`fix/<问题>`。
  - 提交信息用 Conventional Commits，主题可用中文，如 `feat: 实现志愿填报提交校验`。
  - 不提交 `target/`、`node_modules/`、`dist/`、`uploads/`、`*.log`、`application-local.yml`、`.env.local`、IDE 个人配置。
  - 不提交数据库密码、密钥、真实学生个人信息。
- 新增依赖前说明用途；前端不擅自引入 UI 库，后端不擅自引入 Spring Security/PageHelper/Redis 等。

## 15. 完成定义（DoD）

一个功能算完成，必须同时满足：

- [ ] 与对应需求文档、用户故事、原型一致；没有擅自新增/删减业务规则。
- [ ] 后端完成会话校验、角色校验、数据归属校验；越权返回 403。
- [ ] 核心业务不变量在 Service/数据库中强制，不依赖前端。
- [ ] 接口符合 `/api/v1`、统一响应、错误码、分页与时间格式。
- [ ] 前端有加载/空/失败三态，表单禁用逻辑正确，提交防重复，文案与需求一致。
- [ ] 视觉与原型主题一致，复用公共布局与组件。
- [ ] 新增/修改的接口与表结构已写入 `docs/design/`。
- [ ] 后端测试覆盖关键规则；`mvn test`、`npm run lint`、`npm run build` 通过。
- [ ] 没有硬编码用户身份、演示数据、密码、绝对路径；没有遗留调试输出。

## 16. 常见坑与禁止事项

- ❌ 用 `localStorage` 里的角色/学工号做鉴权；✅ 以 Session 为准。
- ❌ 只在菜单/路由隐藏入口，后端不校验；✅ 后端路径角色 + 数据归属双重校验。
- ❌ 用浏览器时间判断截止；✅ 服务端时间 + 前端偏移。
- ❌ 把“加载失败”显示成“暂无数据/暂无待办”；✅ 明确失败态与重试。
- ❌ 新增公告管理 CRUD；✅ 公告只读，仅阶段保存时生成。
- ❌ 实现线上调剂确认/接收；✅ 只读展示线下结果。
- ❌ 实现注册、密码、真实人脸识别；✅ 模拟比对。
- ❌ 把 uploads 目录映射为静态资源；✅ 鉴权下载接口。
- ❌ SQL 使用 `${}` 拼接；✅ `#{}` 参数化。
- ❌ Controller 直接返回实体；✅ 返回 DTO/VO。
- ❌ 名额/确认只靠前端置灰防重复；✅ 事务 + 唯一索引 + 状态判断。
- ❌ 直接修改需求文档或原型来“对齐代码”；✅ 先确认需求，再改实现。
- ❌ 照搬原型的演示开关、假数据、角色切换；✅ 按真实会话与接口实现。
