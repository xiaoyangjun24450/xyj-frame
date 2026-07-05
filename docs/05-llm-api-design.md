# 大模型接口设计

本文说明 FocusPod Android App 当前如何调用大模型：在什么条件下触发、通过什么方式发送、请求到哪个地址、发送什么内容、期望收到什么内容，并给出每个接口的具体输入和返回示例。

## 接口总览

| 场景 | 当前实现 | 调用方式 | 触发条件 | 地址 |
| --- | --- | --- | --- | --- |
| 辅导页 AI 语音辅导 | `DoubaoDialogTutoringAiService` | 火山引擎豆包 Dialog 语音 SDK，底层 WebSocket | 进入辅导页时启动会话；学生之后通过麦克风持续对话 | `wss://openspeech.bytedance.com/api/v3/realtime/dialogue` |
| 批卷页图片批改 | `DoubaoMultimodalGradingAiService` | HTTP `POST`，火山方舟 Responses API | 进入批卷页后等待 5 秒拍照，拍照成功后发起批改 | `https://ark.cn-beijing.volces.com/api/v3/responses` |

配置来自 `local.properties` 或 Gradle Property，最终写入 `BuildConfig`。当前 App 是直连火山引擎接口，生产版本建议把 API Key 放到后端，由后端转发调用。

## 1. 辅导页 AI 语音辅导

### 触发条件

App 进入 `StudyPage.TUTORING` 时触发。这个页面用于原始例题辅导，也用于考试不达标后的错题复习题辅导。

触发流程：

1. `StudyFlow.enterCurrentPage()` 发现当前页是 `TUTORING`。
2. 调用 `startTutoringVoiceSession()`。
3. `DoubaoDialogTutoringAiService.startSession(stage, question, listener)` 启动豆包 Dialog 会话。
4. 会话启动后，App 通过 `SayHello` 发送一段开场白。
5. 后续学生说话由 SDK 录音并发送，App 只接收 ASR 字幕和 AI 字幕/回复事件。

如果 `doubao.appId` 或 `doubao.token` 为空，App 不发起会话，并在页面提示“豆包语音参数未配置”。

### 调用方式和地址

App 不手写 WebSocket 请求，而是通过豆包 `SpeechEngine` SDK 配置连接参数：

```text
dialogAddress = wss://openspeech.bytedance.com
dialogUri     = /api/v3/realtime/dialogue
resourceId    = volc.speech.dialog
```

完整连接地址等价于：

```text
wss://openspeech.bytedance.com/api/v3/realtime/dialogue
```

### 鉴权和基础配置

```properties
doubao.appId=你的 APP ID
doubao.appKey=默认等于 doubao.appId
doubao.token=你的 Access Token
doubao.uid=student-001
doubao.resourceId=volc.speech.dialog
doubao.dialogAddress=wss://openspeech.bytedance.com
doubao.dialogUri=/api/v3/realtime/dialogue
doubao.botName=豆包
doubao.logLevel=WARN
```

### App 发送的消息

#### 1. 启动会话：`DIRECTIVE_START_ENGINE`

发送时机：进入辅导页并准备开始当前题的语音辅导。

发送内容：

```json
{
  "dialog": {
    "bot_name": "豆包",
    "SubtitleConfig": {
      "DisableRTSSubtitle": false,
      "SubtitleMode": 1
    },
    "system_role": "你是学习辅导老师, 请用中文短句分步引导学生思考,只能提示读题、找条件、列关系式、检查单位和过程,不要直接给最终答案，也不要代写完整解题过程.\n当前题目标题：例题：分数应用题\n当前题干：一根绳子长 **24 米**，用去了 $\\frac{1}{3}$。还剩多少米？\n考察知识点：求一个数的几分之几、剩余量计算\n本题辅导要求：引导学生先找总量，再理解用去了三分之一，最后思考剩余量怎么计算。不要直接告诉最终答案。\n如果学生询问题干、条件或当前题目，请依据当前题干回答；仍然不要直接给最终答案。",
    "speaking_style": "用中文短句，语气耐心，分步提问。"
  },
  "SubtitleConfig": {
    "DisableRTSSubtitle": false,
    "SubtitleMode": 1
  }
}
```

字段说明：

| 字段 | 说明 |
| --- | --- |
| `dialog.bot_name` | 豆包机器人名称，默认“豆包” |
| `dialog.system_role` | 当前题上下文和辅导规则，由 `prompts/tutoring_context_prompt.txt` 渲染 |
| `dialog.speaking_style` | AI 说话风格，由 `prompts/tutoring_speaking_style.txt` 读取 |
| `SubtitleConfig.DisableRTSSubtitle` | 是否关闭实时字幕；当前为 `false` |
| `SubtitleConfig.SubtitleMode` | 字幕模式；当前为 `1`，快速字幕 |

#### 2. 播放开场白：`DIRECTIVE_EVENT_SAY_HELLO`

发送时机：收到会话启动成功事件后。

发送内容：

```json
{
  "content": "请先读题，找出已知条件；你也可以直接说出卡住的地方。"
}
```

### 期望收到的消息

App 主要消费以下几类 SDK 回调：

| 回调类型 | App 处理方式 |
| --- | --- |
| 会话启动成功 | 发送 `SayHello` 开场白 |
| 学生 ASR 文本 | 展示为学生消息，更新底部语音字幕 |
| AI Chat/TTS 文本 | 展示为 AI 消息，更新底部语音字幕 |
| TTS 字幕二进制包 | 解包后按 `data[].text` 展示字幕 |
| 会话失败、连接失败、引擎错误 | 当前页显示错误提示，并由流程决定是否重试 |

App 对文本字段做了兼容解析，优先读取这些字段：

```text
content, text, utterance, result, sentence, subtitle
```

字幕包期望解出如下结构：

```json
{
  "data": [
    {
      "userId": "student-001",
      "text": "我不知道先算什么。",
      "definite": true,
      "paragraph": true,
      "sequence": 12,
      "roundId": 3
    }
  ]
}
```

其中 `userId` 等于当前 `doubao.uid` 时，App 认为这条字幕来自学生；否则认为来自 AI。

### 返回示例

学生说“我不知道先算什么”后，SDK 可能回调一条 ASR 文本：

```json
{
  "text": "我不知道先算什么。"
}
```

AI 回复时，SDK 可能回调一条 Chat/TTS 文本：

```json
{
  "content": "先找总量。题里绳子一共多少米？"
}
```

App 期望的业务效果：

```json
{
  "studentMessage": "我不知道先算什么。",
  "aiMessage": "先找总量。题里绳子一共多少米？",
  "shouldRevealFinalAnswer": false
}
```

## 2. 批卷页图片批改

### 触发条件

辅导页或考试页检测到手机翻转后进入批卷页：

```text
a_z > 7 稳定 3 秒
```

进入批卷页后：

1. 显示后置摄像头预览。
2. 等待 5 秒并提示学生不要遮挡试卷。
3. 自动拍照。
4. 拍照成功后调用豆包多模态批卷接口。
5. 批改成功后提示手机翻回正面。
6. 检测到 `|a_z| < 2` 后进入批卷结果页。

如果 `doubao.multimodal.apiKey` 或 `doubao.multimodal.model` 为空，App 不发送 HTTP 请求，直接返回“豆包多模态批卷参数未配置”。

### 调用方式和地址

```http
POST https://ark.cn-beijing.volces.com/api/v3/responses
Authorization: Bearer ${doubao.multimodal.apiKey}
Content-Type: application/json
```

Endpoint 可通过配置覆盖：

```properties
doubao.multimodal.endpoint=https://ark.cn-beijing.volces.com/api/v3/responses
```

### App 发送的消息

请求体字段：

| 字段 | 说明 |
| --- | --- |
| `model` | `doubao.multimodal.model`，可以是模型 ID 或 Endpoint ID |
| `instructions` | 固定批卷提示词，来自 `prompts/grading_multimodal_prompt.txt` |
| `input[].content[]` | 用户消息内容，包含文字题目信息和图片 |
| `input_text` | 当前阶段、考试轮次、题目标题、题干、标准答案、本题批卷要求 |
| `input_image.image_url` | `data:image/jpeg;base64,...` 形式的答案图片 |
| `input_image.detail` | 当前固定为 `high` |
| `text.format` | JSON Schema，要求模型只返回指定字段 |
| `thinking.type` | 当前固定为 `disabled` |
| `stream` | 当前固定为 `false` |

### 输入示例

```json
{
  "model": "doubao-seed-2-0-mini-260428",
  "instructions": "你是严谨的小学作业批改老师。请结合学生纸面答案图片、题干、标准答案和本题批卷要求进行批改。\n\n批改规则：\n1. 只根据图片中能看清的学生作答批改，不要臆测被遮挡或看不清的内容。\n2. 请按请求中的“本题满分”给分，分数必须在 0 到本题满分之间，可以是小数。\n3. 学生最终答案与标准答案等价且符合题意时，直接给本题满分，不要因为步骤不够详细扣分。\n4. 学生最终答案错误、缺失或与题意不符时，不得给满分；结合能看清的列式、关键步骤、单位和方法给过程分。\n5. 反馈要面向学生，中文短句，最多 2 句话。\n6. 批改依据要简洁说明关键判断点，尤其说明答案是否正确。\n7. 下一步建议要给出一个具体可执行的改进方向。\n8. 不返回 pass、passed、是否通过等字段。\n\n请严格按请求中的 JSON Schema 返回。",
  "input": [
    {
      "role": "user",
      "type": "message",
      "content": [
        {
          "type": "input_text",
          "text": "当前阶段：EXAMPLE\n考试轮次：1\n题目标题：例题：分数应用题\n题干：一根绳子长 **24 米**，用去了 $\\frac{1}{3}$。还剩多少米？\n标准答案：16 米\n本题满分：100 分\n本题批卷要求：检查学生是否先算出用去 8 米，再算出剩余 16 米，并注意单位。"
        },
        {
          "type": "input_image",
          "image_url": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD...",
          "detail": "high"
        }
      ]
    }
  ],
  "max_output_tokens": 1600,
  "thinking": {
    "type": "disabled"
  },
  "stream": false,
  "text": {
    "format": {
      "type": "json_schema",
      "name": "grading_result",
      "strict": true,
      "schema": {
        "type": "object",
        "properties": {
          "score": {
            "type": "number",
            "minimum": 0,
            "maximum": 100
          },
          "feedback": {
            "type": "string"
          },
          "reason": {
            "type": "string"
          },
          "suggestion": {
            "type": "string"
          }
        },
        "required": [
          "score",
          "feedback",
          "reason",
          "suggestion"
        ],
        "additionalProperties": false
      }
    }
  }
}
```

### 期望收到的消息

App 期望 HTTP 状态码为 2xx，且响应不是 `status = incomplete`。

App 会从以下位置读取模型输出文本：

1. 根字段 `output_text`
2. `output[]` 中 `type = message` 的 `content[].text`
3. `output[]` 中 `type = output_text` 的 `text`

模型输出文本必须是 JSON，或被标记为 `json` 的代码块包裹的 JSON。最终 JSON 必须符合：

```json
{
  "score": 0,
  "feedback": "string",
  "reason": "string",
  "suggestion": "string"
}
```

字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `score` | number | 0 到本题满分之间的分数，可以是小数 |
| `feedback` | string | 面向学生的简短反馈，最多 2 句话 |
| `reason` | string | 批改依据，说明关键判断点 |
| `suggestion` | string | 下一步可执行建议 |

不要返回：

```text
pass, passed, isPassed, 是否通过
```

是否通过由考试成绩页根据学习计划 `passScore` 统一判断：考试总分达到 `passScore` 才达标。考试总分为本轮考试各题得分相加；每题满分为 `100 / 本轮考试题目数量`；考试题未拿到本题满分时视为错题。错题辅导后只重考这些错题对应的原考试题。

### 返回示例

豆包 Responses API 返回示例：

```json
{
  "id": "resp-202607050001",
  "object": "response",
  "created_at": 1783188000,
  "status": "completed",
  "model": "doubao-seed-2-0-mini-260428",
  "output": [
    {
      "id": "msg-001",
      "type": "message",
      "role": "assistant",
      "content": [
        {
          "type": "output_text",
          "text": "{\"score\":100,\"feedback\":\"答案正确，单位也写清楚了。\",\"reason\":\"学生算出剩余 16 米，最终答案与标准答案一致。\",\"suggestion\":\"继续保持最后检查单位的习惯。\"}"
        }
      ]
    }
  ]
}
```

App 解析后的业务结果：

```json
{
  "questionId": "example-001",
  "stage": "EXAMPLE",
  "score": 100,
  "feedback": "答案正确，单位也写清楚了。",
  "reason": "学生算出剩余 16 米，最终答案与标准答案一致。",
  "suggestion": "继续保持最后检查单位的习惯。"
}
```

### 异常和重试

| 情况 | App 行为 |
| --- | --- |
| 拍照失败 | 当前批卷页显示提示，1 秒后重新进入 5 秒拍摄准备 |
| HTTP 非 2xx | 当前批卷页显示 AI 批改失败，1 秒后重试 |
| 响应 `status = incomplete` | 当前批卷页显示 AI 批改失败，1 秒后重试 |
| 响应缺少输出文本 | 当前批卷页显示 AI 批改失败，1 秒后重试 |
| 输出不是合法 JSON | 当前批卷页显示 AI 批改失败，1 秒后重试 |

成功解析后，App 只在 logcat 输出最终用于业务的 JSON：

```text
Doubao grading result JSON={"score":100,"feedback":"答案正确，单位也写清楚了。","reason":"学生算出剩余 16 米，最终答案与标准答案一致。","suggestion":"继续保持最后检查单位的习惯。"}
```
