# 实训室选拔系统 · API 接口设计

> 文档版本：v1.1  
> 编写日期：2026-11-16  
> 更新日期：2026-11-18  
> 依据：`CLAUDE.md`、`docs/requirements/`、`docs/原型图(1)/`、`docs/数据库设计/数据库设计.md`  
> 基础路径：`/api/v1`  
> 技术约束：HttpSession + Cookie、REST、JSON、统一响应 `{code, message, data}`、成功码 `200`

---

## 1. 接口总览

### 1.1 接口清单（共 30 个）

| # | 方法 | 路径 | 角色 | 用途 |
| --- | --- | --- | --- | --- |
| 1 | POST | `/auth/login` | 公开 | 角色 + 学工号登录，建立会话 |
| 2 | POST | `/auth/logout` | 登录 | 注销会话 |
| 3 | GET | `/auth/me` | 登录 | 当前会话用户 |
| 4 | GET | `/health` | 公开 | 健康检查 |
| 5 | GET | `/stages/current` | 登录 | 当前阶段、全部阶段、服务端时间 |
| 6 | GET | `/announcements` | 登录 | 公告分页列表 |
| 7 | GET | `/announcements/{id}` | 登录 | 公告详情 |
| 8 | GET | `/labs` | 登录 | 实训室列表 / 筛选 |
| 9 | GET | `/labs/{id}` | 登录 | 实训室详情 |
| 10 | GET | `/todos` | 登录 | 当前用户待办（实时推导） |
| 11 | POST | `/files/resumes` | 学生 / 导师 | 上传 PDF 简历，返回 fileId |
| 12 | GET | `/files/{id}` | 按归属 | 鉴权下载简历 |
| 13 | GET | `/student/volunteers` | 学生 | 本人历轮志愿记录 |
| 14 | GET | `/student/volunteers/current` | 学生 | 当前轮志愿状态与本人申请 |
| 15 | GET | `/student/available-teachers` | 学生 | 当前轮仍有名额的导师 |
| 16 | POST | `/student/volunteers` | 学生 | 提交 / 重填当前轮志愿 |
| 17 | GET | `/student/offers` | 学生 | 本人拟录取列表 |
| 18 | POST | `/student/offers/{id}/confirm` | 学生 | 确认拟录取 |
| 19 | GET | `/student/admission` | 学生 | 本人最终录取结果 |
| 20 | GET | `/teacher/lab` | 导师 | 本人实训室信息 |
| 21 | PUT | `/teacher/lab` | 导师 | 新增 / 更新本人实训室信息 |
| 22 | GET | `/teacher/applications` | 导师 | 本人名下本轮申请与面试分数 |
| 23 | PUT | `/teacher/applications/{id}/score` | 导师 | 录入 / 修改面试分数 |
| 24 | PUT | `/teacher/applications/{id}/order` | 导师 | 同分学生排序 |
| 25 | GET | `/teacher/admissions` | 导师 | 名额概览 |
| 26 | POST | `/teacher/admissions/grant` | 导师 | 按分数与名额发放 / 递补拟录取 |
| 27 | GET | `/manager/stages` | 负责人 | 全部阶段配置 |
| 28 | PUT | `/manager/stages` | 负责人 | 保存阶段，可选发布公告 |
| 29 | POST | `/manager/stages/announcement` | 负责人 | 仅重试发布公告 |
| 30 | GET | `/manager/stage-changes` | 负责人 | 阶段保存 / 公告状态记录 |

> 命名说明：为保持接口名简单，`GET /teacher/admissions` 对应 `CLAUDE.md` 示例中的 `/teacher/admissions/summary`，省略了 `summary`；如团队要求严格对齐，可改回。其余路径均与 `CLAUDE.md` 的关键接口清单一致或为其补充。

### 1.2 角色与路径前缀

| 前缀 | 角色 | 说明 |
| --- | --- | --- |
| `/auth/**` | 公开 / 登录 | 登录、注销、当前用户 |
| `/health` | 公开 | 健康检查 |
| `/stages/**`、`/announcements/**`、`/labs/**`、`/todos/**` | 登录 | 公共只读数据 |
| `/files/**` | 登录 | 文件上传 / 下载，按归属校验 |
| `/student/**` | 学生 | 学生专属 |
| `/teacher/**` | 导师 | 导师专属 |
| `/manager/**` | 负责人 | 负责人专属 |

路径级角色只是第一道闸；数据归属必须在 Service 层再次校验，绝不能从请求参数读取 `studentId` / `teacherId` 作为身份依据。

### 1.3 页面与接口映射

| 页面 | 使用接口 |
| --- | --- |
| 登录 | `POST /auth/login`、`GET /auth/me` |
| 主页面 | `GET /auth/me`、`GET /stages/current`、`GET /announcements?pageSize=3`、`GET /todos`、学生额外 `GET /student/admission` |
| 公告 | `GET /announcements`、`GET /announcements/{id}` |
| 个人待办 | `GET /todos` |
| 查看实训室信息 | `GET /labs`、`GET /labs/{id}`、`GET /files/{id}` |
| 志愿填报 / 分轮补录 | `GET /student/volunteers/current`、`GET /student/available-teachers`、`POST /files/resumes`、`POST /student/volunteers`、`GET /student/volunteers` |
| 拟录取确认 | `GET /student/offers`、`POST /student/offers/{id}/confirm`、`GET /student/admission` |
| 提交实训室信息 | `GET /teacher/lab`、`POST /files/resumes`、`PUT /teacher/lab` |
| 发放拟录取 | `GET /teacher/admissions`、`GET /teacher/applications`、`PUT /teacher/applications/{id}/score`、`PUT /teacher/applications/{id}/order`、`POST /teacher/admissions/grant`、`GET /files/{id}` |
| 阶段管理 | `GET /manager/stages`、`PUT /manager/stages`、`POST /manager/stages/announcement`、`GET /manager/stage-changes` |

---

## 2. 通用约定

### 2.1 请求与响应

成功：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

失败：

```json
{
  "code": 40301,
  "message": "无权访问该数据",
  "data": null
}
```

- 请求 / 响应字段使用 camelCase。
- 日期时间格式：`yyyy-MM-dd'T'HH:mm:ss`，无时区后缀，统一 `Asia/Shanghai`。
- 业务校验 / 状态冲突使用 HTTP 200 + 非 200 `code`；认证 / 权限 / 资源不存在 / 未捕获异常使用对应 HTTP 状态码，响应体仍保持统一结构。
- 前端 Axios：`code !== 200` 统一弹 Toast；网络异常提示“网络异常，请重试”。
- 文件下载成功时返回二进制流，不使用统一 JSON；失败时仍返回统一 JSON。

### 2.2 错误码

| code | 含义 | HTTP |
| --- | --- | --- |
| 200 | 成功 | 200 |
| 40001 | 参数 / 格式校验失败 | 200 |
| 40101 | 未登录或会话过期 | 401 |
| 40301 | 无权限 / 越权访问 | 403 |
| 40401 | 资源不存在 | 404 |
| 40901 | 状态冲突（未开放、已截止、名额已满、重复确认、重复发放、同分未定序等） | 200 |
| 50001 | 系统异常 | 500 |
| 50002 | 文件上传 / 存储失败 | 200 |

### 2.3 分页

| 参数 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| `page` | int | 1 | 从 1 开始 |
| `pageSize` | int | 10 | 最大 100 |

分页响应 `data`：

```json
{
  "list": [],
  "total": 0,
  "page": 1,
  "pageSize": 10
}
```

### 2.4 时间与倒计时

- 所有开放 / 截止判断以服务端时间为准。
- 涉及截止时间的接口在 `data` 中返回 `serverTime`，前端计算本地偏移，不依赖浏览器时钟。
- 阶段时间精确到分钟；接口输出秒位固定为 `00`。
- `offer.confirmDeadline` 是发放时的快照，后续阶段调整不自动改写。

### 2.5 鉴权与会话

- `POST /auth/login` 成功后服务端销毁旧会话并新建会话，写入 `SessionUser { userId, staffNo, name, role }`。
- Cookie：`HttpOnly=true`、`SameSite=Lax`、`Path=/`，生产环境 `Secure=true`，超时 30 分钟（可配置）。
- `AuthInterceptor` 拦截 `/api/v1/**`，放行 `POST /auth/login` 与 `GET /health`。
- 前端 Axios 使用 `baseURL: '/api/v1'`、`withCredentials: true`。
- 不使用 `Authorization` 头、不把角色 / 学工号存 `localStorage` 作为鉴权依据。
- 无会话：HTTP 401 + `code=40101`；角色不符或数据不属于当前用户：HTTP 403 + `code=40301`。
- 前端处理：401 清理用户状态并跳 `/login?redirect=...`；403 提示“无权访问该数据”，不跳登录。
- 本地通过 Vite 代理、生产同源部署；若必须跨域，CORS 只允许精确来源并开启 `allowCredentials`，禁止 `*`；JSON 请求 + `SameSite=Lax` 作为 CSRF 基线。

### 2.6 文件上传与下载

- 仅接受 PDF：扩展名 `.pdf`、`Content-Type: application/pdf`、文件头 `%PDF-`；大小上限默认 10 MB。
- 磁盘路径：`backend/uploads/resumes/{yyyy}/{MM}/{uuid}.pdf`，数据库存相对路径。
- 上传接口返回 `fileId`；提交志愿 / 实训室信息时只传 `fileId`。
- 下载统一走 `GET /files/{id}`，不把 `uploads/` 映射为静态资源。
- 导师简历：所有登录用户可读；学生简历：仅本人和收到该申请的导师可读。

### 2.7 幂等与防重复

| 操作 | 服务端兜底 |
| --- | --- |
| 登录首次建用户 | `sys_user(staff_no, role)` 唯一索引 |
| 同轮重复提交志愿 | 事务 + `volunteer_application(student_id, round_no)` 唯一 |
| 重复确认 | 事务 + `final_admission(student_id)` 唯一 |
| 重复发放 | 事务 + `offer(teacher_id, round_no, student_id)`、`offer(application_target_id)` 唯一 |
| 重复保存阶段 | `changeNo` 幂等键 + `stage_change.change_no` 唯一 |
| 重复发布公告 | 行锁 + `stage_change.announcement_id` 唯一 |

前端按钮禁用只是 UX，不能替代上述兜底。

### 2.8 枚举值

| 枚举 | 取值 |
| --- | --- |
| 角色 `role` | `STUDENT` / `TEACHER` / `MANAGER` |
| 阶段编码 | `BRIEFING` / `OPEN_DAY` / `ROUND_1` / `ROUND_2` / `ROUND_3` / `ADJUSTMENT` / `ENROLLMENT` |
| 轮次 | `1` / `2` / `3` |
| 拟录取状态 | `PENDING` / `CONFIRMED` / `ABANDONED` |
| 放弃原因 | `TIMEOUT` / `CONFIRMED_OTHER` |
| 录取类型 | `VOLUNTEER` / `ADJUST_INTRA_LAB` / `ADJUST_COLLEGE` |
| 公告发布状态 | `NONE` / `PENDING` / `PUBLISHED` / `FAILED` |
| 简历用途 | `STUDENT_RESUME` / `TEACHER_RESUME` |

---

## 3. 认证与会话

### 3.1 POST `/auth/login`

**角色**：公开  
**用途**：选择身份、输入学工号，完成登录。人脸比对由前端模拟，不调用后端人脸服务。

**请求体**

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `role` | string | 是 | `STUDENT` / `TEACHER` / `MANAGER` |
| `staffNo` | string | 是 | 10 位数字，正则 `^\d{10}$` |

```json
{
  "role": "STUDENT",
  "staffNo": "2026001001"
}
```

**响应 `data`**

```json
{
  "userId": 1,
  "staffNo": "2026001001",
  "name": null,
  "role": "STUDENT"
}
```

**业务规则**

1. 角色必须选择；学工号只校验格式，不查询账号是否存在、不校验身份是否匹配。
2. 首次登录自动创建 `sys_user(staff_no, role)`；同一学工号不同角色是两条独立身份。
3. 登录成功后销毁旧会话并新建会话，防会话固定。
4. `name` 可为空；前端为空时显示学工号。
5. 登录响应不返回密码、Session ID、人脸数据。
6. 客户端不应传 `studentId` / `teacherId`；身份以 `(staffNo, role)` 为准。

**错误**

| code | HTTP | 场景 | 文案 |
| --- | --- | --- | --- |
| 40001 | 200 | 未选择角色 | 请选择身份 |
| 40001 | 200 | 学工号为空 / 非 10 位数字 | 学工号异常 |
| 50001 | 500 | 系统异常 | 加载失败，请重试 |

---

### 3.2 POST `/auth/logout`

**角色**：登录用户  
**用途**：注销当前会话。

**请求体**：无。

**响应 `data`**：`null`

**业务规则**

- 服务端使会话失效。
- 前端清理用户状态并跳转 `/login`。
- 未登录调用返回 401 + `40101`。

---

### 3.3 GET `/auth/me`

**角色**：登录用户  
**用途**：获取当前会话用户，用于刷新页面后恢复登录态。

**响应 `data`**

```json
{
  "userId": 1,
  "staffNo": "2026001001",
  "name": "张伟",
  "role": "STUDENT"
}
```

**错误**

| code | HTTP | 场景 |
| --- | --- | --- |
| 40101 | 401 | 无会话或会话过期 |

---

## 4. 公共接口

### 4.1 GET `/health`

**角色**：公开  
**用途**：健康检查；`AuthInterceptor` 放行。

**响应 `data`**

```json
{
  "status": "UP",
  "serverTime": "2026-11-16T10:00:00"
}
```

---

### 4.2 GET `/stages/current`

**角色**：登录用户  
**用途**：主页面进度条、当前阶段、倒计时；学生 / 导师只读。

**响应 `data`**

```json
{
  "serverTime": "2026-11-16T10:00:00",
  "status": "ONGOING",
  "configured": true,
  "currentStage": {
    "stageCode": "ROUND_1",
    "stageName": "第一轮录取",
    "sortNo": 3,
    "startAt": "2026-11-23T09:00:00",
    "endAt": "2026-11-27T17:00:00"
  },
  "nextStage": null,
  "stages": [
    {
      "stageCode": "BRIEFING",
      "stageName": "集中宣讲",
      "sortNo": 1,
      "startAt": "2026-11-18T09:00:00",
      "endAt": "2026-11-18T17:00:00",
      "state": "FINISHED"
    },
    {
      "stageCode": "ROUND_1",
      "stageName": "第一轮录取",
      "sortNo": 3,
      "startAt": "2026-11-23T09:00:00",
      "endAt": "2026-11-27T17:00:00",
      "state": "ONGOING"
    }
  ]
}
```

**字段说明**

| 字段 | 说明 |
| --- | --- |
| `status` | `UNCONFIGURED` / `ONGOING` / `BETWEEN` / `ENDED` |
| `configured` | 7 个阶段是否全部配置时间 |
| `currentStage` | 当前时间所在阶段；`BETWEEN` / `ENDED` / `UNCONFIGURED` 时为 null |
| `nextStage` | `BETWEEN` 时的下一阶段；其他情况为 null |
| `stages[].state` | `UNCONFIGURED` / `NOT_STARTED` / `ONGOING` / `FINISHED` |

**展示规则**

- `ONGOING`：显示当前阶段、起止时间、截止倒计时。
- `BETWEEN`：显示下一阶段及开始时间。
- `ENDED`：显示“选拔已结束”。
- `UNCONFIGURED`：显示“阶段暂未配置”，不虚构阶段和倒计时。

**错误**：40101。

---

### 4.3 GET `/announcements`

**角色**：登录用户  
**用途**：公告只读分页列表；主页面取前 3 条。

**查询参数**

| 参数 | 类型 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| `page` | int | 否 | 1 | 页码 |
| `pageSize` | int | 否 | 10 | 每页条数，最大 100 |

**响应 `data`**

```json
{
  "list": [
    {
      "id": 10,
      "title": "关于调整选拔阶段时间的通知",
      "summary": "各阶段时间调整如下……",
      "publishedAt": "2026-11-16T09:00:00"
    }
  ],
  "total": 1,
  "page": 1,
  "pageSize": 10
}
```

**业务规则**

- 按 `publishedAt DESC, id DESC` 排序。
- 摘要由正文派生，不单独存储。
- 无公告时 `list: []`，前端显示“暂无公告”。
- 不提供新增、编辑、删除接口。

**错误**：40001（分页参数非法）、40101。

---

### 4.4 GET `/announcements/{id}`

**角色**：登录用户  
**用途**：公告详情。

**响应 `data`**

```json
{
  "id": 10,
  "title": "关于调整选拔阶段时间的通知",
  "content": "各阶段时间调整如下：\n\n第一轮录取：……",
  "publisherId": 1,
  "publisherName": "王老师",
  "publishedAt": "2026-11-16T09:00:00"
}
```

**错误**

| code | HTTP | 场景 |
| --- | --- | --- |
| 40401 | 404 | 公告不存在 |
| 40101 | 401 | 未登录 |

---

### 4.5 GET `/todos`

**角色**：登录用户  
**用途**：当前用户可办理待办，由“当前阶段 + 用户状态”实时推导，不落表。

**响应 `data`**

```json
{
  "serverTime": "2026-11-16T10:00:00",
  "list": [
    {
      "type": "VOLUNTEER_SUBMIT",
      "title": "填报志愿",
      "description": "第一轮志愿填报进行中",
      "stageCode": "ROUND_1",
      "stageName": "第一轮录取",
      "deadline": "2026-11-27T17:00:00",
      "status": "TODO",
      "actionPath": "/student/volunteer",
      "actionLabel": "去填报"
    }
  ]
}
```

**`type` 取值**

| type | 角色 | 说明 |
| --- | --- | --- |
| `VOLUNTEER_SUBMIT` | 学生 | 当前轮未填报，需填报志愿 |
| `VOLUNTEER_MAKEUP` | 学生 | 第二 / 第三轮补录填报 |
| `OFFER_CONFIRM` | 学生 | 有未截止的待确认拟录取 |
| `LAB_INFO` | 导师 | 尚未填写实训室信息 |
| `SCORE_ENTRY` | 导师 | 本轮有未评分申请 |
| `GRANT_OFFER` | 导师 | 本轮可发放 / 递补拟录取 |
| `STAGE_CONFIG` | 负责人 | 阶段未配置或需要调整 |

**规则**

- 已完成、已截止、已放弃的事项不返回。
- 已录取学生不再返回志愿填报、补录、拟录取确认待办。
- 排序：`deadline ASC`（无截止时间最后）、`type ASC`，保证展示稳定。
- 无待办返回 `list: []`，前端显示“暂无待办”。
- 加载失败返回错误，前端显示“加载失败，请重试”，不能显示为“暂无待办”。

**错误**：40101。

---

## 5. 实训室与文件

### 5.1 GET `/labs`

**角色**：登录用户  
**用途**：实训室公开信息列表 / 筛选。每个导师一行；同一实训室的总额通过 `labTotalQuota` 汇总。

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `keyword` | string | 否 | 匹配实训室名称 / 导师姓名 / 研究方向 |
| `researchDirection` | string | 否 | 研究方向精确筛选 |
| `page` | int | 否 | 默认 1 |
| `pageSize` | int | 否 | 默认 10，最大 100 |

**响应 `data`**

```json
{
  "list": [
    {
      "id": 10,
      "labName": "云计算与大数据实验室",
      "location": "主校区 · 软件学院楼 · 2-204",
      "teacherId": 2,
      "teacherName": "李娜",
      "researchDirection": "云计算与大数据",
      "plannedQuota": 4,
      "labTotalQuota": 6,
      "contact": "138-0000-2222",
      "resumeFileId": 20
    }
  ],
  "total": 1,
  "page": 1,
  "pageSize": 10,
  "researchDirections": ["云计算与大数据", "人工智能"]
}
```

**说明**

- `labTotalQuota`：同一 `labName` + `location` 下所有导师 `plannedQuota` 之和。
- `researchDirections`：全部实训室的研究方向去重，供前端下拉筛选；不受当前分页限制。
- 未填写实训室信息的导师不出现。
- 排序：`labName ASC, teacherName ASC, id ASC`，保证分页稳定。
- 关键字使用 `LIKE '%关键字%'`，数据量小，接受全表扫描。

**错误**：40001、40101。

---

### 5.2 GET `/labs/{id}`

**角色**：登录用户  
**用途**：实训室详情；`id` 为 `lab_info.id`。

**响应 `data`**

```json
{
  "id": 10,
  "labName": "云计算与大数据实验室",
  "location": "主校区 · 软件学院楼 · 2-204",
  "teacherId": 2,
  "teacherName": "李娜",
  "researchDirection": "云计算与大数据",
  "plannedQuota": 4,
  "labTotalQuota": 6,
  "contact": "138-0000-2222",
  "resumeFileId": 20,
  "resumeFileName": "李娜-简历.pdf",
  "updatedAt": "2026-11-15T14:30:00"
}
```

**错误**：40101、40401。

---

### 5.3 POST `/files/resumes`

**角色**：学生 / 导师  
**用途**：上传 PDF 简历，返回 `fileId`。用途由会话角色推断：学生 → `STUDENT_RESUME`，导师 → `TEACHER_RESUME`。

**请求**：`multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `file` | file | 是 | PDF，大小 ≤ 10 MB |

**响应 `data`**

```json
{
  "fileId": 20,
  "originalName": "张三-简历.pdf",
  "fileSize": 123456,
  "contentType": "application/pdf",
  "uploadedAt": "2026-11-16T10:00:00"
}
```

**校验**

1. 扩展名 `.pdf`。
2. `Content-Type: application/pdf`。
3. 文件头 `%PDF-`。
4. 大小 ≤ 10 MB（可配置）。
5. 磁盘文件名使用 UUID，禁止原始文件名作为磁盘名。
6. 上传失败不产生数据库记录。

**错误**

| code | HTTP | 场景 | 文案 |
| --- | --- | --- | --- |
| 40001 | 200 | 非 PDF | 请上传 PDF 格式的简历 |
| 50002 | 200 | 存储失败 | 简历上传失败，请重试 |
| 40101 | 401 | 未登录 | — |
| 40301 | 403 | 角色不允许 | 无权访问该数据 |

---

### 5.4 GET `/files/{id}`

**角色**：登录用户，按归属校验  
**用途**：鉴权下载 PDF 简历。

**权限**

| 文件用途 | 可读用户 |
| --- | --- |
| `TEACHER_RESUME` | 所有登录用户 |
| `STUDENT_RESUME` | 学生本人；收到该学生申请的导师（`application_target.teacher_id = 当前用户`） |

**成功响应**

- `Content-Type: application/pdf`
- `Content-Disposition: attachment; filename*=UTF-8''<percent-encoded originalName>`
- `Content-Length`
- `Cache-Control: private, no-store`
- `X-Content-Type-Options: nosniff`
- Body 为 PDF 二进制流。

**失败响应**：统一 JSON。

**错误**

| code | HTTP | 场景 | 文案 |
| --- | --- | --- | --- |
| 40101 | 401 | 未登录 | — |
| 40301 | 403 | 无归属 / 越权 | 无权访问该数据 |
| 40401 | 404 | 文件不存在 | — |

---

## 6. 学生接口

### 6.1 GET `/student/volunteers`

**角色**：学生  
**用途**：查看本人历轮志愿记录；只读，不包含面试分数和拟录取状态。

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `roundNo` | int | 否 | 1 / 2 / 3；不传返回全部 |

**响应 `data`**

```json
{
  "serverTime": "2026-11-16T10:00:00",
  "list": [
    {
      "applicationId": 1,
      "roundNo": 1,
      "submittedAt": "2026-11-24T10:00:00",
      "intraLabAdjust": true,
      "collegeAdjust": false,
      "resumeFileId": 20,
      "resumeFileName": "张三-简历.pdf",
      "targets": [
        {
          "preferenceNo": 1,
          "teacherId": 2,
          "teacherName": "李娜",
          "labName": "云计算与大数据实验室"
        }
      ]
    }
  ]
}
```

**规则**

- 只返回 `student_id = 当前用户 ID` 的记录。
- 按 `roundNo DESC` 排序。
- 历史记录只读，不提供修改接口。

**错误**：40001（roundNo 非法）、40101。

---

### 6.2 GET `/student/volunteers/current`

**角色**：学生  
**用途**：志愿填报 / 补录页初始化：当前轮状态、本人本轮申请、是否可提交及原因。

**响应 `data`**

```json
{
  "serverTime": "2026-11-16T10:00:00",
  "roundNo": 2,
  "roundState": "OPEN",
  "roundStart": "2026-12-01T09:00:00",
  "roundEnd": "2026-12-05T17:00:00",
  "admitted": false,
  "canSubmit": true,
  "blockedReason": null,
  "availableTeacherCount": 5,
  "application": {
    "applicationId": 2,
    "roundNo": 2,
    "submittedAt": "2026-12-02T10:00:00",
    "intraLabAdjust": true,
    "collegeAdjust": true,
    "resumeFileId": 20,
    "resumeFileName": "张三-简历.pdf",
    "targets": [
      {
        "preferenceNo": 1,
        "teacherId": 2,
        "teacherName": "李娜",
        "labName": "云计算与大数据实验室"
      }
    ]
  }
}
```

**字段说明**

| 字段 | 说明 |
| --- | --- |
| `roundNo` | 当前或下一轮次；没有可填报轮次时为 null |
| `roundState` | `OPEN` / `NOT_STARTED` / `ENDED` / `NO_ROUND` / `UNCONFIGURED` |
| `admitted` | 是否已有最终录取 |
| `canSubmit` | 当前是否允许提交 |
| `blockedReason` | `ADMITTED` / `NOT_OPEN` / `ENDED` / `NO_ROUND` / `NO_QUOTA` / `PROCESSED` |
| `availableTeacherCount` | 当前轮仍有剩余名额的导师数量 |
| `application` | 本轮已有申请；未提交为 null |

**规则**

- 服务端根据阶段表判断当前轮次，不接受前端传入轮次作为依据。
- 当前时间在下一轮开始前：返回下一轮及 `roundState=NOT_STARTED`，前端提示“补录填报尚未开始”。
- 当前轮已结束：返回该轮及 `roundState=ENDED`，前端提示“补录填报已截止”。
- 已有最终录取：`admitted=true`，`canSubmit=false`，`blockedReason=ADMITTED`。
- 补录轮次没有剩余名额：`blockedReason=NO_QUOTA`。
- 本轮申请已进入评分 / 发放阶段：`blockedReason=PROCESSED`，不允许重填。判断依据为本轮目标已产生面试分数或拟录取，包括待确认、已确认、已放弃的拟录取。

**错误**：40101。

---

### 6.3 GET `/student/available-teachers`

**角色**：学生  
**用途**：第二、第三轮补录页顶部只读列表：当前轮仍有剩余名额的导师。

**响应 `data`**

```json
{
  "serverTime": "2026-11-16T10:00:00",
  "roundNo": 2,
  "list": [
    {
      "teacherId": 2,
      "teacherName": "李娜",
      "labName": "云计算与大数据实验室",
      "location": "主校区 · 软件学院楼 · 2-204",
      "researchDirection": "云计算与大数据",
      "plannedQuota": 4,
      "remainingQuota": 2
    }
  ]
}
```

**规则**

- 只返回 `remainingQuota > 0` 的导师。
- `remainingQuota = plannedQuota - 最终录取数 - 未过截止的待确认拟录取数`。
- 排序：`teacherName ASC, teacherId ASC`，保证展示稳定。
- 当前不在录取轮次时返回 `roundNo: null`、`list: []`。
- 列表只读，不提供搜索、点击选择或候选列表；前端仍使用三个普通输入框。

**错误**：40101。

---

### 6.4 POST `/student/volunteers`

**角色**：学生  
**用途**：提交或重填当前轮志愿。每轮一条申请，重填覆盖本轮目标，不影响其他轮次。

**请求体**

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `teacherNames` | string[] | 是 | 1–3 个导师姓名；空字符串会被忽略；不可重复 |
| `intraLabAdjust` | boolean | 是 | 是否同意实训室内调剂 |
| `collegeAdjust` | boolean | 是 | 是否同意学院内调剂 |
| `resumeFileId` | long | 是 | 本人上传的 `STUDENT_RESUME` 文件 ID |

```json
{
  "teacherNames": ["李娜", "王强", "赵敏"],
  "intraLabAdjust": true,
  "collegeAdjust": false,
  "resumeFileId": 20
}
```

**响应 `data`**

```json
{
  "applicationId": 2,
  "roundNo": 2,
  "submittedAt": "2026-12-02T10:00:00",
  "intraLabAdjust": true,
  "collegeAdjust": false,
  "targets": [
    {
      "preferenceNo": 1,
      "teacherId": 2,
      "teacherName": "李娜",
      "labName": "云计算与大数据实验室"
    },
    {
      "preferenceNo": 2,
      "teacherId": 5,
      "teacherName": "王强",
      "labName": "人工智能实验室"
    },
    {
      "preferenceNo": 3,
      "teacherId": 8,
      "teacherName": "赵敏",
      "labName": "软件工程实验室"
    }
  ],
  "message": "提交成功"
}
```

**业务规则**

1. 当前阶段必须是录取轮次，且当前时间在该轮区间内；否则按情况返回“补录填报尚未开始 / 补录填报已截止 / 当前不在录取轮次”。
2. 学生已有最终录取：拒绝，提示“你已确认录取，无需参加补录”；如果是通过“志愿填报”菜单进入，提示“已被录取！”。
3. `teacherNames` 去掉首尾空格、合并连续空格后：
   - 有效姓名数量必须为 1–3；
   - 不可重复，重复提示“导师姓名不能重复”；
   - 每个姓名必须精确匹配唯一一条 `lab_info.teacher_name`；未匹配或匹配多条，提示“导师姓名不存在或存在重名，请联系负责人”。
4. 第二、第三轮补录：每个解析出的导师必须仍有剩余名额；否则提示“所填导师已无剩余名额，请重新选择”。
5. `resumeFileId` 必须属于当前学生且用途为 `STUDENT_RESUME`；否则拒绝。
6. 两项调剂意愿均必填；均选择“不同意”也可提交。
7. 同轮重填：锁定该生该轮申请行，替换 `application_target`，更新 `submittedAt`；不新增第二条申请。
8. 如果本轮目标已产生面试分数或拟录取（包括待确认、已确认、已放弃的拟录取），拒绝重填，提示“本轮志愿已进入处理阶段，不能再修改”。
9. 事务提交；失败保留前端填写内容。

**错误**

| code | HTTP | 场景 | 文案 |
| --- | --- | --- | --- |
| 40001 | 200 | 姓名数量不在 1–3、姓名为空、调剂意愿缺失、简历非法 | 提交失败，请重试 / 导师姓名不能重复 |
| 40901 | 200 | 未开始 / 已截止 | 第一轮：志愿填报尚未开始 / 志愿填报已截止；第二、三轮：补录填报尚未开始 / 补录填报已截止 |
| 40901 | 200 | 已录取 | 你已确认录取，无需参加补录 / 已被录取！ |
| 40901 | 200 | 导师未匹配或重名 | 导师姓名不存在或存在重名，请联系负责人 |
| 40901 | 200 | 导师无名额 | 所填导师已无剩余名额，请重新选择 |
| 40901 | 200 | 本轮无任何名额 | 本轮暂无可填报名额 |
| 40101 | 401 | 未登录 | — |
| 40301 | 403 | 非学生 / 简历不属于本人 | 无权访问该数据 |

---

### 6.5 GET `/student/offers`

**角色**：学生  
**用途**：查看本人收到的拟录取；用于拟录取确认页。

**响应 `data`**

```json
{
  "serverTime": "2026-11-27T16:00:00",
  "list": [
    {
      "offerId": 100,
      "teacherId": 2,
      "teacherName": "李娜",
      "labName": "云计算与大数据实验室",
      "roundNo": 1,
      "status": "PENDING",
      "confirmDeadline": "2026-11-27T17:00:00",
      "grantedAt": "2026-11-26T10:00:00",
      "confirmedAt": null,
      "abandonedAt": null,
      "abandonReason": null,
      "canConfirm": true
    }
  ]
}
```

**规则**

- 只返回 `student_id = 当前用户 ID` 的拟录取。
- 按 `roundNo DESC, grantedAt DESC` 排序。
- 返回前对本人已过 `confirmDeadline` 的 `PENDING` 记录做惰性校正（改为 `ABANDONED`），确保状态准确；定时任务仍作为主兜底。
- `canConfirm`：`status=PENDING`、当前时间早于 `confirmDeadline`、且本人无最终录取。
- 无拟录取返回 `list: []`，前端显示“暂无拟录取”。
- 调剂录取不是 offer，通过 `GET /student/admission` 展示，不提供确认按钮。

**错误**：40101。

---

### 6.6 POST `/student/offers/{id}/confirm`

**角色**：学生  
**用途**：确认一条拟录取。确认后不可更改，其余本人待确认拟录取自动放弃。

**路径参数**

| 参数 | 说明 |
| --- | --- |
| `id` | `offer.id` |

**请求体**：无。

**响应 `data`**

```json
{
  "offerId": 100,
  "status": "CONFIRMED",
  "confirmedAt": "2026-11-27T16:10:00",
  "finalAdmission": {
    "id": 1,
    "teacherId": 2,
    "teacherName": "李娜",
    "labName": "云计算与大数据实验室",
    "roundNo": 1,
    "admissionType": "VOLUNTEER",
    "admittedAt": "2026-11-27T16:10:00"
  },
  "abandonedOfferIds": [101, 102]
}
```

**业务规则**

1. 拟录取必须属于当前学生；否则 403。
2. 状态必须为 `PENDING`；已确认提示“已确认录取，不可更改”，已放弃按状态冲突处理。
3. 当前时间必须早于 `confirmDeadline`；已截止提示“确认时间已截止”，服务端同时惰性标记超时放弃。
4. 本人不能已有最终录取；已有则提示“已确认录取，不可更改”。
5. 事务内：
   - 选中记录改 `CONFIRMED`，写 `confirmedAt`；
   - 其余本人 `PENDING` 记录改 `ABANDONED`，写 `abandonedAt`、`abandonReason=CONFIRMED_OTHER`；
   - 插入 `final_admission`（志愿录取）。
6. 并发 / 重复请求由行锁 + `final_admission(student_id)` 唯一索引兜底，只能产生一个确认结果。
7. 确认失败统一提示“确认失败，请刷新查看结果后重试”，不在页面直接显示成功。

**错误**

| code | HTTP | 场景 | 文案 |
| --- | --- | --- | --- |
| 40901 | 200 | 已过截止 | 确认时间已截止 |
| 40901 | 200 | 已确认 / 已有最终录取 | 已确认录取，不可更改 |
| 40901 | 200 | 已放弃 | 确认失败，请刷新查看结果后重试 |
| 40301 | 403 | 非本人拟录取 | 无权访问该数据 |
| 40401 | 404 | 拟录取不存在 | — |
| 50001 | 500 | 系统异常 | 确认失败，请刷新查看结果后重试 |

---

### 6.7 GET `/student/admission`

**角色**：学生  
**用途**：查看本人最终录取结果，包括志愿录取和统筹调剂录取；用于主页面“已录取”标记和最终结果展示。

**响应 `data`（已录取）**

```json
{
  "admitted": true,
  "admission": {
    "id": 1,
    "teacherId": 2,
    "teacherName": "李娜",
    "labName": "云计算与大数据实验室",
    "location": "主校区 · 软件学院楼 · 2-204",
    "roundNo": 1,
    "admissionType": "VOLUNTEER",
    "admittedAt": "2026-11-27T16:10:00"
  }
}
```

**响应 `data`（未录取）**

```json
{
  "admitted": false,
  "admission": null
}
```

**字段说明**

| `admissionType` | 前端文案 |
| --- | --- |
| `VOLUNTEER` | 志愿录取 |
| `ADJUST_INTRA_LAB` | 实训室内调剂 |
| `ADJUST_COLLEGE` | 学院内调剂 |

**规则**

- 调剂录取 `roundNo`、`sourceOfferId` 为 null，不提供确认按钮。
- 已录取学生仍可查看阶段、公告；不再生成志愿 / 补录 / 确认待办。
- 学生点击“志愿填报”时，若已录取，前端只提示“已被录取！”，不进入填报页。

**错误**：40101。

---

## 7. 导师接口

### 7.1 GET `/teacher/lab`

**角色**：导师  
**用途**：获取本人实训室信息；无记录时返回未提交状态，前端展示空白表单。

**响应 `data`（已提交）**

```json
{
  "submitted": true,
  "lab": {
    "id": 10,
    "teacherId": 2,
    "teacherName": "李娜",
    "labName": "云计算与大数据实验室",
    "location": "主校区 · 软件学院楼 · 2-204",
    "plannedQuota": 4,
    "researchDirection": "云计算与大数据",
    "contact": "138-0000-2222",
    "resumeFileId": 20,
    "resumeFileName": "李娜-简历.pdf",
    "updatedAt": "2026-11-15T14:30:00"
  },
  "occupiedQuota": 2,
  "remainingQuota": 2
}
```

**响应 `data`（未提交）**

```json
{
  "submitted": false,
  "lab": null,
  "occupiedQuota": 0,
  "remainingQuota": null
}
```

**规则**

- `occupiedQuota` = 本人最终录取数（含调剂）+ 未过截止的待确认拟录取数，跨轮次累计。
- `remainingQuota = plannedQuota - occupiedQuota`；未提交时为 null。
- 只返回当前会话导师本人的信息。

**错误**：40101。

---

### 7.2 PUT `/teacher/lab`

**角色**：导师  
**用途**：新增或更新本人实训室信息；存在则更新原记录，不新增重复记录。

**请求体**

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `labName` | string | 是 | 实训室名称，去空格后非空 |
| `location` | string | 是 | 校区、楼栋及房间号 |
| `plannedQuota` | int | 是 | 正整数 |
| `teacherName` | string | 是 | 导师姓名，去首尾空格、合并连续空格 |
| `researchDirection` | string | 是 | 研究方向 |
| `contact` | string | 是 | 联系方式 |
| `resumeFileId` | long | 是 | 本人 `TEACHER_RESUME` 文件 ID |

```json
{
  "labName": "云计算与大数据实验室",
  "location": "主校区 · 软件学院楼 · 2-204",
  "plannedQuota": 4,
  "teacherName": "李娜",
  "researchDirection": "云计算与大数据",
  "contact": "138-0000-2222",
  "resumeFileId": 20
}
```

**响应 `data`**

```json
{
  "submitted": true,
  "lab": {
    "id": 10,
    "teacherId": 2,
    "teacherName": "李娜",
    "labName": "云计算与大数据实验室",
    "location": "主校区 · 软件学院楼 · 2-204",
    "plannedQuota": 4,
    "researchDirection": "云计算与大数据",
    "contact": "138-0000-2222",
    "resumeFileId": 20,
    "resumeFileName": "李娜-简历.pdf",
    "updatedAt": "2026-11-16T10:00:00"
  },
  "occupiedQuota": 2,
  "remainingQuota": 2,
  "message": "提交成功"
}
```

**业务规则**

1. 仅导师本人可写；请求体不接受 `teacherId`，以会话为准。
2. 所有必填项去空格后非空；`plannedQuota` 为正整数。
3. `resumeFileId` 必须属于当前导师且用途为 `TEACHER_RESUME`。
4. 修改 `plannedQuota` 时不得小于当前占用名额；否则提示“计划招收人数不能小于当前已占用名额”。
5. 保存前锁定 `lab_info` 行，避免并发降低名额导致超发。
6. 按 `teacher_id` 唯一更新；并发首次提交由唯一索引兜底。
7. 提交失败保留前端填写内容，原已保存信息不变。

**错误**

| code | HTTP | 场景 | 文案 |
| --- | --- | --- | --- |
| 40001 | 200 | 必填项缺失 / 人数非正整数 / 简历非法 | 提交失败，请重试 |
| 40901 | 200 | 名额低于占用 | 计划招收人数不能小于当前已占用名额 |
| 40101 | 401 | 未登录 | — |
| 40301 | 403 | 非导师 / 简历不属于本人 | 无权访问该数据 |

---

### 7.3 GET `/teacher/applications`

**角色**：导师  
**用途**：查看本人名下本轮申请、面试分数、同分顺序及拟录取状态。

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `roundNo` | int | 否 | 1 / 2 / 3；不传使用当前轮 |

**响应 `data`**

```json
{
  "serverTime": "2026-11-26T10:00:00",
  "roundNo": 1,
  "roundState": "OPEN",
  "roundStart": "2026-11-23T09:00:00",
  "roundEnd": "2026-11-27T17:00:00",
  "canScore": true,
  "canGrant": true,
  "list": [
    {
      "targetId": 50,
      "applicationId": 1,
      "studentId": 3,
      "staffNo": "2026001001",
      "studentName": "张伟",
      "preferenceNo": 1,
      "score": 92.00,
      "tieOrder": null,
      "scoredAt": "2026-11-26T09:30:00",
      "resumeFileId": 20,
      "resumeFileName": "张伟-简历.pdf",
      "offerId": null,
      "offerStatus": null,
      "confirmDeadline": null,
      "studentAdmitted": false,
      "scoreEditable": true,
      "tieOrderEditable": true
    }
  ]
}
```

**字段说明**

| 字段 | 说明 |
| --- | --- |
| `roundNo` | 查询轮次；无当前轮且未传 roundNo 时返回错误 |
| `roundState` | `OPEN` / `NOT_STARTED` / `ENDED` / `NO_ROUND` |
| `canScore` | 当前轮开放且可评分 |
| `canGrant` | 当前轮开放、本人已提交实训室信息 |
| `offerStatus` | null / `PENDING` / `CONFIRMED` / `ABANDONED` |
| `studentAdmitted` | 该学生是否已有最终录取（任意导师），用于避免重复发放 |
| `scoreEditable` | 当前轮开放、本人名下、且该目标尚未产生 offer |
| `tieOrderEditable` | 同 `scoreEditable`，且该目标已评分 |

**规则**

- 只返回 `application_target.teacher_id = 当前用户 ID` 的目标。
- `roundNo` 不传时使用当前阶段对应的轮次；当前不在录取轮次返回 40901“当前不在录取轮次”。
- 返回前对本人本轮已过 `confirmDeadline` 的 `PENDING` 拟录取做惰性校正，避免教师看到过期的“待确认”。
- 排序：`score DESC`（未评分最后）、`tieOrder ASC`、`id ASC`。
- `studentName` 可为空；为空时前端显示 `staffNo`。
- 不返回学生的其他志愿、调剂意愿或其他导师的分数。
- 学生简历通过 `GET /files/{id}` 下载。
- 前端在申请列表的“简历”列提供“查看”入口，使用 `resumeFileId` 调用 `GET /files/{id}`；`resumeFileId` 为空时按钮禁用。导师只能下载申请到本人名下的学生简历，归属校验见 §5.4。

**错误**：40001（roundNo 非法）、40101、40901（当前不在录取轮次）。

---

### 7.4 PUT `/teacher/applications/{id}/score`

**角色**：导师  
**用途**：录入或修改某条申请的面试分数。

**路径参数**

| 参数 | 说明 |
| --- | --- |
| `id` | `application_target.id` |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `score` | decimal | 是 | 0–100，最多两位小数 |

```json
{
  "score": 92.5
}
```

**响应 `data`**

```json
{
  "targetId": 50,
  "score": 92.50,
  "tieOrder": null,
  "scoredAt": "2026-11-26T10:00:00"
}
```

**业务规则**

1. 目标必须属于当前导师；否则 403。
2. 分数必须为 0–100 的有效数值；否则提示“请填写有效分数”。
3. 当前轮必须开放；本轮结束提示“本轮已结束”。
4. 目标已产生拟录取（任何状态）时，不允许修改分数，提示“已发放拟录取，分数不可修改”。
5. 修改分数后清空该目标的 `tieOrder`；如产生新的同分，导师需重新定序。
6. 未评分目标不参与发放。

**错误**

| code | HTTP | 场景 | 文案 |
| --- | --- | --- | --- |
| 40001 | 200 | 分数非法 | 请填写有效分数 |
| 40901 | 200 | 本轮已结束 | 本轮已结束 |
| 40901 | 200 | 已发放拟录取 | 已发放拟录取，分数不可修改 |
| 40101 | 401 | 未登录 | — |
| 40301 | 403 | 非本人申请 | 无权访问该数据 |
| 40401 | 404 | 目标不存在 | — |

---

### 7.5 PUT `/teacher/applications/{id}/order`

**角色**：导师  
**用途**：为同分学生手动确定先后顺序。

**路径参数**

| 参数 | 说明 |
| --- | --- |
| `id` | `application_target.id` |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `tieOrder` | int | 是 | 正整数，越小越靠前 |

```json
{
  "tieOrder": 1
}
```

**响应 `data`**

```json
{
  "targetId": 50,
  "score": 92.50,
  "tieOrder": 1,
  "updatedAt": "2026-11-26T10:05:00"
}
```

**业务规则**

1. 目标必须属于当前导师；否则 403。
2. 目标必须有面试分数；否则提示“请先录入面试分数”。
3. 同一导师、同一轮、同一分数组内 `tieOrder` 不得重复；否则提示“同分顺序不能重复”。
4. 目标已产生拟录取时，不允许修改顺序，提示“已发放拟录取，顺序不可修改”。
5. 本轮已结束时，提示“本轮已结束”。
6. 前端可对同分组内每个学生分别调用本接口；发放时服务端再次校验同分组是否全部定序。

**错误**

| code | HTTP | 场景 | 文案 |
| --- | --- | --- | --- |
| 40001 | 200 | tieOrder 非正整数 / 重复 | 同分顺序不能重复 |
| 40901 | 200 | 未评分 | 请先录入面试分数 |
| 40901 | 200 | 已发放 | 已发放拟录取，顺序不可修改 |
| 40901 | 200 | 本轮已结束 | 本轮已结束 |
| 40101 | 401 | 未登录 | — |
| 40301 | 403 | 非本人申请 | 无权访问该数据 |
| 40401 | 404 | 目标不存在 | — |

---

### 7.6 GET `/teacher/admissions`

**角色**：导师  
**用途**：名额概览：计划名额、已确认、待确认、剩余可发放。

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `roundNo` | int | 否 | 1 / 2 / 3；不传使用当前轮 |

**响应 `data`**

```json
{
  "serverTime": "2026-11-26T10:00:00",
  "roundNo": 1,
  "roundState": "OPEN",
  "roundStart": "2026-11-23T09:00:00",
  "roundEnd": "2026-11-27T17:00:00",
  "labSubmitted": true,
  "plannedQuota": 4,
  "admittedCount": 1,
  "pendingCount": 1,
  "remainingQuota": 2,
  "canGrant": true
}
```

**字段说明**

| 字段 | 说明 |
| --- | --- |
| `plannedQuota` | 本人计划招收总数（跨轮次累计） |
| `admittedCount` | 本人已形成的最终录取数（含志愿、调剂，全部轮次） |
| `pendingCount` | 本人未过截止的待确认拟录取数（当前轮） |
| `remainingQuota` | `plannedQuota - admittedCount - pendingCount` |
| `canGrant` | 当前轮开放且已提交实训室信息 |

**规则**

- 名额按导师累计总数计算，不按每轮独立。
- 未提交实训室信息时：`plannedQuota=null`、`remainingQuota=null`、`canGrant=false`。
- 当前不在录取轮次且未传 `roundNo` 时返回 40901“当前不在录取轮次”。

**错误**：40001、40101、40901。

---

### 7.7 POST `/teacher/admissions/grant`

**角色**：导师  
**用途**：按分数从高到低、结合名额发放拟录取；学生放弃 / 超时后再次调用可递补。

**请求体**

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `cutoffScore` | decimal | 是 | 0–100，录取分数线 |

```json
{
  "cutoffScore": 85
}
```

**响应 `data`**

```json
{
  "roundNo": 1,
  "grantedCount": 2,
  "offers": [
    {
      "offerId": 100,
      "studentId": 3,
      "staffNo": "2026001001",
      "studentName": "张伟",
      "score": 92.00,
      "status": "PENDING",
      "confirmDeadline": "2026-11-27T17:00:00"
    }
  ],
  "remainingQuota": 0,
  "message": "发放成功"
}
```

**业务规则**

1. 当前阶段必须是录取轮次且未过本轮结束时间；否则提示“本轮拟录取发放已截止”或“当前不在录取轮次”。
2. 导师必须已提交实训室信息；否则不能发放。
3. 锁定 `lab_info` 行，计算：
   ```
   占用 = 最终录取数（全部轮次、含调剂）
        + 未过截止的待确认拟录取数
   剩余 = plannedQuota - 占用
   ```
4. 剩余名额 ≤ 0：提示“名额已满”。
5. 候选目标：
   - 属于当前导师、当前轮；
   - 已评分；
   - `score >= cutoffScore`；
   - 学生无最终录取；
   - 该目标尚无拟录取（含已放弃）；
   - 同分顺序已确定。
6. 若待发放的达标学生中存在未定序的同分组，且会影响发放顺序，返回“请先确定同分学生顺序”，本次不发放（整批回滚）。
7. 无达标学生：提示“暂无可发放的达标学生”。
8. 按 `score DESC, tieOrder ASC, id ASC` 依次发放，直到名额占满或没有达标学生。
9. 每条拟录取：
   - `status=PENDING`；
   - `grantedAt=当前时间`；
   - `confirmDeadline=本轮 stage.end_at`；
   - 不发送短信、邮件、站内推送。
10. 事务提交；唯一索引兜底重复发放 / 超发。
11. 重复点击且名额已满时返回“名额已满”，不生成重复记录。
12. 截止后不再发放或递补。

**错误**

| code | HTTP | 场景 | 文案 |
| --- | --- | --- | --- |
| 40001 | 200 | 分数线非法 | 请填写有效分数 |
| 40901 | 200 | 名额已满 | 名额已满 |
| 40901 | 200 | 同分未定序 | 请先确定同分学生顺序 |
| 40901 | 200 | 无达标学生 | 暂无可发放的达标学生 |
| 40901 | 200 | 已截止 | 本轮拟录取发放已截止 |
| 40901 | 200 | 当前不在轮次 | 当前不在录取轮次 |
| 40101 | 401 | 未登录 | — |
| 40301 | 403 | 非导师 / 数据越权 | 无权访问该数据 |

---

## 8. 负责人接口

### 8.1 GET `/manager/stages`

**角色**：负责人  
**用途**：阶段管理页初始化，返回 7 个固定阶段当前配置。

**响应 `data`**

```json
{
  "serverTime": "2026-11-16T10:00:00",
  "configured": true,
  "currentStageCode": "ROUND_1",
  "stages": [
    {
      "stageCode": "BRIEFING",
      "stageName": "集中宣讲",
      "sortNo": 1,
      "startAt": "2026-11-18T09:00:00",
      "endAt": "2026-11-18T17:00:00"
    },
    {
      "stageCode": "OPEN_DAY",
      "stageName": "实训室开放日",
      "sortNo": 2,
      "startAt": "2026-11-19T09:00:00",
      "endAt": "2026-11-19T17:00:00"
    }
  ]
}
```

**规则**

- `stages` 固定 7 条，按 `sortNo` 升序。
- 未配置时 `startAt` / `endAt` 为 null，`configured=false`。
- `currentStageCode` 为当前阶段；空档 / 结束 / 未配置时为 null。
- 阶段名称、编码、顺序不可修改。

**错误**：40101、40301。

---

### 8.2 PUT `/manager/stages`

**角色**：负责人  
**用途**：保存 7 个阶段时间；可选发布公告。阶段保存成功即生效。

**请求体**

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `changeNo` | string | 是 | 幂等键，建议前端点击保存时生成 UUID，重试复用，最大 64 字符 |
| `stages` | array | 是 | 必须恰好 7 条，覆盖全部固定阶段 |
| `stages[].stageCode` | string | 是 | 固定阶段编码 |
| `stages[].startAt` | datetime | 是 | 开始时间，精确到分钟 |
| `stages[].endAt` | datetime | 是 | 结束时间，精确到分钟 |
| `publishAnnouncement` | boolean | 是 | 是否本次发布公告 |
| `announcement.title` | string | 条件必填 | `publishAnnouncement=true` 时必填，≤ 200 字 |
| `announcement.content` | string | 条件必填 | `publishAnnouncement=true` 时必填 |

```json
{
  "changeNo": "8f14e45f-ceea-467f-a1d5-2f5e8d0b9a11",
  "stages": [
    { "stageCode": "BRIEFING",   "startAt": "2026-11-18T09:00:00", "endAt": "2026-11-18T17:00:00" },
    { "stageCode": "OPEN_DAY",   "startAt": "2026-11-19T09:00:00", "endAt": "2026-11-19T17:00:00" },
    { "stageCode": "ROUND_1",    "startAt": "2026-11-23T09:00:00", "endAt": "2026-11-27T17:00:00" },
    { "stageCode": "ROUND_2",    "startAt": "2026-12-01T09:00:00", "endAt": "2026-12-05T17:00:00" },
    { "stageCode": "ROUND_3",    "startAt": "2026-12-08T09:00:00", "endAt": "2026-12-12T17:00:00" },
    { "stageCode": "ADJUSTMENT", "startAt": "2026-12-15T09:00:00", "endAt": "2026-12-17T17:00:00" },
    { "stageCode": "ENROLLMENT", "startAt": "2026-12-20T09:00:00", "endAt": "2026-12-20T17:00:00" }
  ],
  "publishAnnouncement": true,
  "announcement": {
    "title": "关于调整选拔阶段时间的通知",
    "content": "各阶段时间调整如下：\n\n第一轮录取：……\n\n请各位同学、导师关注各阶段起止时间。"
  }
}
```

**响应 `data`**

不发布公告：

```json
{
  "changeNo": "8f14e45f-ceea-467f-a1d5-2f5e8d0b9a11",
  "stageSaved": true,
  "announcementStatus": "NONE",
  "announcementId": null,
  "message": "保存成功"
}
```

公告发布成功：

```json
{
  "changeNo": "8f14e45f-ceea-467f-a1d5-2f5e8d0b9a11",
  "stageSaved": true,
  "announcementStatus": "PUBLISHED",
  "announcementId": 10,
  "message": "保存成功，公告已发布"
}
```

阶段保存成功、公告发布失败：

```json
{
  "changeNo": "8f14e45f-ceea-467f-a1d5-2f5e8d0b9a11",
  "stageSaved": true,
  "announcementStatus": "FAILED",
  "announcementId": null,
  "message": "阶段已保存，公告发布失败，请重试"
}
```

**业务规则**

1. 只有负责人可调用；学生 / 导师直接访问返回 403 + “仅负责人可配置阶段”。
2. `stages` 必须恰好 7 条，编码必须覆盖全部固定阶段，不可新增、删除、重命名。
3. 7 个阶段时间必须全部填写；任一缺失提示“请填写完整的阶段时间”。
4. 每个阶段开始时间必须早于结束时间；否则提示“阶段结束时间必须晚于开始时间”。
5. 按 `sortNo` 顺序，前一阶段结束不得晚于后一阶段开始；允许空档；否则提示“阶段时间必须按流程顺序设置且不能重叠”。
6. 允许填写已经过去的时间。
7. `publishAnnouncement=true` 时，标题或正文为空提示“公告内容不能为空”，不保存阶段、不发布公告。
8. 阶段保存与 `stage_change` 记录在同一事务提交，保存后立即生效。
9. 公告发布在阶段保存成功后用独立事务执行：
   - 成功：返回“保存成功，公告已发布”；
   - 失败：阶段仍生效，返回“阶段已保存，公告发布失败，请重试”，前端保留 `changeNo` 并提供重试。
10. `changeNo` 重复：直接返回该次保存的已有结果，不重复保存阶段、不重复生成公告。
11. 每次保存最多生成一条公告。
12. 阶段调整不恢复已放弃的拟录取、不重新开放已截止的填报 / 确认、不回滚已提交志愿或录取结果。
13. 公告标题 / 正文的自动生成在前端完成（前端持有旧配置和新表单值），后端只校验并保存；不单独提供预览接口。
    - 首次配置：公告列出全部阶段；
    - 后续修改：只列出本次变更的阶段，并同时展示原时间与新时间。

**错误**

| code | HTTP | 场景 | 文案 |
| --- | --- | --- | --- |
| 40001 | 200 | 阶段数量 / 编码非法 | 提交失败，请重试 |
| 40001 | 200 | 时间缺失 | 请填写完整的阶段时间 |
| 40001 | 200 | 开始不早于结束 | 阶段结束时间必须晚于开始时间 |
| 40001 | 200 | 顺序 / 重叠 | 阶段时间必须按流程顺序设置且不能重叠 |
| 40001 | 200 | 公告内容为空 | 公告内容不能为空 |
| 40901 | 200 | 已截止阶段延长 end_at | 阶段已截止，不能延长 |
| 40301 | 403 | 非负责人 | 仅负责人可配置阶段 |
| 40101 | 401 | 未登录 | — |

> **D3 已截止阶段**：若某阶段旧 `end_at` 已到达或已过（旧 `end_at` <= 服务端当前时间），
> 新 `end_at` 不得晚于旧值；允许保持或缩短，不允许延长。阶段调整不会重新开放已截止窗口。

---

### 8.3 POST `/manager/stages/announcement`

**角色**：负责人  
**用途**：仅重试发布某次阶段保存对应的公告；不重复保存阶段，不重复生成公告。

**请求体**

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `changeNo` | string | 是 | 阶段保存时返回的幂等键 |

```json
{
  "changeNo": "8f14e45f-ceea-467f-a1d5-2f5e8d0b9a11"
}
```

**响应 `data`**

```json
{
  "changeNo": "8f14e45f-ceea-467f-a1d5-2f5e8d0b9a11",
  "announcementStatus": "PUBLISHED",
  "announcementId": 10,
  "message": "公告已发布"
}
```

**业务规则**

1. 按 `changeNo` 读取 `stage_change` 中保存的公告草稿。
2. 若已发布：返回已有公告，幂等成功。
3. 若该次保存未请求发布公告：返回 40901“该次保存未请求发布公告”。
4. 公告标题 / 正文为空：返回 40001“公告内容不能为空”。
5. 只发布公告，不更新 `stage`，不新增 `stage_change`。
6. 行锁 + 唯一索引保证并发重试最多生成一条公告。
7. 公告发布后不可修改、不可删除。

**错误**

| code | HTTP | 场景 | 文案 |
| --- | --- | --- | --- |
| 40001 | 200 | 草稿内容为空 | 公告内容不能为空 |
| 40401 | 404 | changeNo 不存在 | — |
| 40901 | 200 | 未请求发布公告 | 该次保存未请求发布公告 |
| 40301 | 403 | 非负责人 | 仅负责人可配置阶段 |
| 40101 | 401 | 未登录 | — |

---

### 8.4 GET `/manager/stage-changes`

**角色**：负责人  
**用途**：查看阶段保存 / 公告发布记录，用于页面刷新后找回失败公告并重试，以及审计。不返回阶段历史版本。

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | string | 否 | `NONE` / `PENDING` / `PUBLISHED` / `FAILED` |
| `page` | int | 否 | 默认 1 |
| `pageSize` | int | 否 | 默认 10，最大 100 |

**响应 `data`**

```json
{
  "list": [
    {
      "changeNo": "8f14e45f-ceea-467f-a1d5-2f5e8d0b9a11",
      "operatorId": 1,
      "operatorName": "王老师",
      "publishRequested": true,
      "announcementStatus": "FAILED",
      "draftTitle": "关于调整选拔阶段时间的通知",
      "announcementId": null,
      "createdAt": "2026-11-16T09:50:00",
      "updatedAt": "2026-11-16T09:50:05"
    }
  ],
  "total": 1,
  "page": 1,
  "pageSize": 10
}
```

**规则**

- 按 `createdAt DESC` 排序。
- 只返回负责人可见的内部记录；不暴露 SQL、堆栈或敏感信息。
- 不返回阶段旧时间 / 新时间快照；公告草稿正文由重试接口在服务端使用。

**错误**：40001、40101、40301。

---

## 9. 权限矩阵

| 接口 | 学生 | 导师 | 负责人 | 归属 / 条件 |
| --- | --- | --- | --- | --- |
| `POST /auth/login` | 公开 | 公开 | 公开 | — |
| `POST /auth/logout` | 是 | 是 | 是 | 本人会话 |
| `GET /auth/me` | 是 | 是 | 是 | 本人会话 |
| `GET /health` | 公开 | 公开 | 公开 | — |
| `GET /stages/current` | 只读 | 只读 | 只读 | — |
| `GET /announcements` | 只读 | 只读 | 只读 | — |
| `GET /announcements/{id}` | 只读 | 只读 | 只读 | — |
| `GET /labs` | 只读 | 只读 | 只读 | — |
| `GET /labs/{id}` | 只读 | 只读 | 只读 | — |
| `GET /todos` | 本人 | 本人 | 本人 | 当前会话用户 |
| `POST /files/resumes` | 是 | 是 | 否 | 学生 / 导师本人 |
| `GET /files/{id}` | 按归属 | 按归属 | 按归属 | 导师简历全员可读；学生简历本人或收到申请的导师 |
| `GET /student/volunteers` | 本人 | 否 | 否 | `student_id = 当前用户` |
| `GET /student/volunteers/current` | 本人 | 否 | 否 | 同上 |
| `GET /student/available-teachers` | 是 | 否 | 否 | 只读 |
| `POST /student/volunteers` | 本人 | 否 | 否 | 同上 |
| `GET /student/offers` | 本人 | 否 | 否 | `student_id = 当前用户` |
| `POST /student/offers/{id}/confirm` | 本人 | 否 | 否 | 拟录取归属校验 |
| `GET /student/admission` | 本人 | 否 | 否 | `student_id = 当前用户` |
| `GET /teacher/lab` | 否 | 本人 | 否 | `teacher_id = 当前用户` |
| `PUT /teacher/lab` | 否 | 本人 | 否 | 同上 |
| `GET /teacher/applications` | 否 | 本人 | 否 | `application_target.teacher_id = 当前用户` |
| `PUT /teacher/applications/{id}/score` | 否 | 本人 | 否 | 目标归属校验 |
| `PUT /teacher/applications/{id}/order` | 否 | 本人 | 否 | 目标归属校验 |
| `GET /teacher/admissions` | 否 | 本人 | 否 | `teacher_id = 当前用户` |
| `POST /teacher/admissions/grant` | 否 | 本人 | 否 | 同上 |
| `GET /manager/stages` | 否 | 否 | 是 | 仅负责人 |
| `PUT /manager/stages` | 否 | 否 | 是 | 仅负责人 |
| `POST /manager/stages/announcement` | 否 | 否 | 是 | 仅负责人 |
| `GET /manager/stage-changes` | 否 | 否 | 是 | 仅负责人 |

> 路径级角色约束只是第一道闸；所有个人记录查询必须在 SQL 中带当前会话用户条件。

---

## 10. 错误提示文案对照

| 场景 | code | 文案 |
| --- | --- | --- |
| 学工号格式错误 | 40001 | 学工号异常 |
| 未选择身份 | 40001 | 请选择身份 |
| 非 PDF | 40001 | 请上传 PDF 格式的简历 |
| 上传失败 | 50002 | 简历上传失败，请重试 |
| 导师重复 | 40001 | 导师姓名不能重复 |
| 导师无法匹配 / 重名 | 40901 | 导师姓名不存在或存在重名，请联系负责人 |
| 通用提交失败 | 50001 | 提交失败，请重试 |
| 确认已截止 | 40901 | 确认时间已截止 |
| 已确认后再改 | 40901 | 已确认录取，不可更改 |
| 确认失败 | 50001 | 确认失败，请刷新查看结果后重试 |
| 名额满 | 40901 | 名额已满 |
| 分数非法 | 40001 | 请填写有效分数 |
| 同分未定序 | 40901 | 请先确定同分学生顺序 |
| 无达标学生 | 40901 | 暂无可发放的达标学生 |
| 发放已截止 | 40901 | 本轮拟录取发放已截止 |
| 名额低于占用 | 40901 | 计划招收人数不能小于当前已占用名额 |
| 补录未开始 / 已截止 | 40901 | 补录填报尚未开始 / 补录填报已截止 |
| 补录无名额 | 40901 | 本轮暂无可填报名额 |
| 所填导师无名额 | 40901 | 所填导师已无剩余名额，请重新选择 |
| 已录取仍补录 | 40901 | 你已确认录取，无需参加补录 |
| 已录取仍填报 | 40901 | 已被录取！ |
| 阶段未配置 / 无公告 | — | 阶段暂未配置 / 暂无公告 |
| 加载失败 | 50001 | 加载失败，请重试 |
| 阶段时间不完整 | 40001 | 请填写完整的阶段时间 |
| 阶段起止错误 | 40001 | 阶段结束时间必须晚于开始时间 |
| 阶段顺序 / 重叠 | 40001 | 阶段时间必须按流程顺序设置且不能重叠 |
| 公告内容为空 | 40001 | 公告内容不能为空 |
| 阶段保存成功 | 200 | 保存成功 |
| 阶段 + 公告成功 | 200 | 保存成功，公告已发布 |
| 公告发布失败 | 200 | 阶段已保存，公告发布失败，请重试 |
| 非负责人访问阶段管理 | 40301 | 仅负责人可配置阶段 |
| 越权 | 40301 | 无权访问该数据 |
| 当前不在录取轮次 | 40901 | 当前不在录取轮次 |
| 本轮已结束 | 40901 | 本轮已结束 |
| 已发放后改分数 | 40901 | 已发放拟录取，分数不可修改 |
| 已发放后改顺序 | 40901 | 已发放拟录取，顺序不可修改 |
| 同分顺序重复 | 40001 | 同分顺序不能重复 |
| 未评分先排序 | 40901 | 请先录入面试分数 |
| 未请求发布公告 | 40901 | 该次保存未请求发布公告 |
| 公告已发布 | 200 | 公告已发布 |
| 本轮申请已处理不可重填 | 40901 | 本轮志愿已进入处理阶段，不能再修改 |

> 上述新增文案中，“本轮志愿已进入处理阶段，不能再修改”已于 2026-11-18 确认；其余文案（志愿填报尚未开始 / 志愿填报已截止、当前不在录取轮次、本轮已结束、已发放后改分数 / 顺序、同分顺序重复、未评分先排序、未请求发布公告）仍为接口设计补充，需与产品确认后定稿。

---

## 11. 待确认问题

> 2026-11-18 更新：#3 已确认采用“禁止重填”方案，其余问题仍待确认。

| # | 问题 | 当前设计 | 影响 |
| --- | --- | --- | --- |
| 1 | 是否需要“本人历轮志愿”查询接口？ | 提供 `GET /student/volunteers` | 若页面不展示历史，可删除该接口，减少一个读接口 |
| 2 | 是否需要阶段保存 / 公告状态列表接口？ | 提供 `GET /manager/stage-changes` | 用于刷新后找回失败公告；若前端用本地状态保存 `changeNo`，可省略 |
| 3 | 已评分 / 已发放后是否允许重填志愿？ | **已确认（2026-11-18）**：禁止，返回“本轮志愿已进入处理阶段，不能再修改” | 若允许，需要引入申请版本号并调整数据库唯一索引 |
| 4 | 拟录取发放后是否允许修改分数 / 同分顺序？ | 禁止，与原型中已发放行分数只读一致 | 若允许，需要记录发放时分数快照，避免排序与已发放结果不一致 |
| 5 | 本轮结束后是否允许补录分数？ | 禁止，返回“本轮已结束” | 若允许，需明确允许的时间窗口 |
| 6 | 存在未定序同分时，整批发放拒绝还是只跳过该组？ | 整批拒绝，返回“请先确定同分学生顺序” | 若只跳过该组，需要定义部分发放的响应与事务边界 |
| 7 | 发放接口是否接收 `roundNo`？ | 不接收，服务端按当前阶段确定轮次 | 若需要补发历史轮次，需要增加 `roundNo` 并严格校验 |
| 8 | 公告标题 / 正文自动生成放在前端还是后端？ | 前端生成（已有旧配置 + 新表单值），后端只校验保存 | 若要求后端生成，需要增加公告预览接口 |
| 9 | 学生是否需要查看自己的面试分数？ | 不提供；学生只看拟录取与最终录取 | 若需要，增加学生侧分数查询接口并确认是否允许在发放前展示 |
| 10 | 导师是否可以查看历史轮次申请？ | 接口支持 `roundNo`，历史轮次只读 | 若只允许当前轮，可移除 `roundNo` 参数 |
| 11 | 同一实训室总额如何展示？ | `GET /labs` 返回 `labTotalQuota = 同 labName + location 的 plannedQuota 之和` | 若要求严格的实训室实体，需要新增 `lab` 表并调整需求 |
| 12 | 负责人是否需要查看录取明细？ | 按 `CLAUDE.md` 权限矩阵：不可访问 | 若需要，调整权限矩阵并新增负责人查询接口 |
| 13 | `serverTime` 放在响应体还是全局响应头？ | 相关接口的 `data.serverTime` | 若希望所有接口统一，可增加 `X-Server-Time` 响应头 |

---

## 12. 变更记录

| 日期 | 版本 | 变更 |
| --- | --- | --- |
| 2026-11-16 | v1.0 | 初稿：30 个接口、通用约定、权限矩阵、页面映射、错误文案 |
| 2026-11-18 | v1.1 | 确认同轮申请产生面试分数或拟录取后禁止重填；补充导师查看申请学生简历的前端调用说明 |
