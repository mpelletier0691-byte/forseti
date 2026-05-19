# 将既有案件导入 Forseti

您可能已在手机、网盘和若干邮件文件夹中散落数月文书、截图与 PDF。Forseti 旨在吸收这种杂乱，并归入清晰、可重复的结构。

本指南说明 **案件档案** 中的导入按钮（文件夹导入与图片导入）、**Brokkr Forge 案件文件夹布局**，以及使自动分类更准确的命名习惯。

---

## 1. Brokkr Forge 布局（每个案件的结构）

Forseti 创建的每个案件均使用相同的十一文件夹骨架。您无需自行设计——保存新案件时 Forseti 即会生成。编号确保无论使用何种文件管理器，文件夹均按诉讼顺序排列。

```
Case_001_<您的标题>/
├── 00_Case_Overview/        笔记、当事人列表、案件索引
├── 01_Pleadings/            起诉状、答辩状、动议、命令
├── 02_Service_of_Process/   Proof_of_Service, Summons, Correspondence
├── 03_Discovery/            Interrogatories, Requests_for_Production,
│                            Admissions, Depositions, Discovery_Responses
├── 04_Evidence/             Photos, PDFs, Screenshots, Audio, Video
├── 05_Motions/              Drafts, Filed, Court_Responses
├── 06_Correspondence/       Opposing_Party, Court, Misc
├── 07_Deadlines/            日程条目 + Completed/
├── 08_Exhibits/             Labels and Final_Exhibits
├── 09_Hearings/             Notices, Prep, Outcomes
├── 10_Trial/                Trial_Brief, Witness_List, Jury_Instructions, Final_Binder
├── 98_Scans/                相机扫描收件箱（未匹配关键词）
└── 99_Inbox/                自动路由无法分类时放入此处
```

落在 **99_Inbox/** 的文件可在 **案件档案 → 打开案件** 中手动移动。

---

## 2. 两个导入按钮

打开案件档案，编辑（或创建）要填充的案件，在 **起诉状已提交** 下方：

| 按钮 | 适用情形 |
| --- | --- |
| **导入案件文件文件夹** | 手机、USB-OTG 或 Google Drive 中已有本案大量混杂文件。 |
| **导入图片 / 截图** | 只需选取特定照片、截图或短 PDF，无需导入周围全部内容。 |

两者均经 **同一自动路由器**；区别仅在于 *选择范围*：整棵树或手工筛选列表。

### 2.1 文件夹导入

点按后打开 Android SAF 目录选择器。选择案件文件夹并 *使用此文件夹*，Forseti 将：

1. 遍历树中每个文件（含子文件夹）。
2. 检查文件名与扩展名。
3. 将各文件放入对应 Brokkr Forge 文件夹。
4. 在 `00_INDEX.txt` 记录每次移动以备查。
5. 显示提示：*"已导入 42 个文件 · 6 个进入 99_Inbox"*。

无法分类者进入 `99_Inbox/`。打开案件（案件档案 → 点卡片）后手动移动——通常每文件一次点按即可。

### 2.2 图片导入

打开系统文件选择器，筛选图片、PDF、音频与视频。多选后点 *打开*，运行同一路由器。

适用于图库有上千张照片但只需五月破损楼梯那七张的情形。

---

## 3. 通过重命名提高路由准确度

路由器先查看 **文件名**，再查看 **扩展名**。

导入前 **重命名** 是提升准确度最快的方法。示例：

| 不佳名称 | 良好名称 | 目标位置 |
| --- | --- | --- |
| `IMG_20240312_103144.jpg` | `2024-03-12_broken_stair_evidence.jpg` | `04_Evidence/Photos/` |
| `Document (3).pdf` | `2024-04-01_motion_to_dismiss.pdf` | `05_Motions/Drafts/` |
| `Screenshot_20240419_181203.png` | `2024-04-19_screenshot_text_threats.png` | `04_Evidence/Screenshots/` |
| `scan001.pdf` | `2024-05-08_proof_of_service_summons.pdf` | `02_Service_of_Process/Proof_of_Service/` |

不必全部重命名——Forseti 可识别 `complaint`、`answer`、`motion`、`discovery`、`deposition`、`interrog`、`subpoena`、`service`、`summons`、`exhibit`、`hearing`、`witness`、`trial brief`、`order`、`judgment` 等关键词。其余将进入 `99_Inbox/` 供您分拣。

---

## 4. 新手推荐流程

1. 在 **案件档案** 中创建案件，至少填写标题、法院与案号。
2. 在图库中 **重命名重要截图与照片**——五分钟很值得。
3. 在 **编辑案件** 对话框点 **导入文件夹**，指向杂乱源文件夹。
4. 打开案件并浏览 **99_Inbox/**，将遗留文件移入正确 Brokkr Forge 文件夹。
5. 在 **期限** 标签页设置截止日——Forseti 将在 `07_Deadlines/` 创建对应子文件夹。
6. 从邮件或 PDF 阅读器使用 **分享 → Forseti → 案件工作区** 持续添加文书。

---

## 5. 在案件内打开、重命名与改路径

文件进入工作区后无需离开 Forseti。

- 在 **案件档案 → 您的案件** 中 **点按任意文件** 用内置查看器打开：
  - PDF 使用与 FRCP 相同的双指缩放阅读器，带 **朗读** 按钮。
  - 图片为缩放预览。
  - 文本/markdown 可选择——长按复制。
  - 其他格式提供 **在其他应用中打开**。
- 用铅笔图标 **重命名**。每次重命名后 Forseti 询问 *是否移至其他文件夹？* — 一键选择新位置或 **保持在此**。
- **分享** 经 Android 分享表发送（FileProvider；其他应用获临时只读 URL）。
- **删除** 从案件工作区永久移除。

> **提示。** 名为 `scan001.pdf` 的扫描收据会进入 `98_Scans/`。打开后 **重命名** → `2024-08-14_receipt_repair_invoice.pdf`，再选 `04_Evidence/PDFs/`。约 10 秒完成。

---

## 6. 隐私

导入的每个文件均复制到 Forseti 应用私有存储 `Android/data/com.forseti/files/case_workspace/`。不上传；除非您对具体文件点 **分享**。自动备份仅在重装时用同一 Google 账户恢复数据。

---

> **Forseti 理念：** 人工智能或许强大，但人的精神与毅力更为强大——愿此助您走完诉讼之路。
