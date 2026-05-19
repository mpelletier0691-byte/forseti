# 在电脑上建立数字案件文件夹

无论使用 Windows、macOS 还是 Linux，同一文件夹结构都能让自诉案件井然有序。Forseti 的 **笔记** 标签页用于思考；**案件文件夹** 才是正式记录。

## 结构（请原样复制）

```
~/Cases/
  YYYY-MM-DD_<案件简称>/
    00_INDEX.md                <- 每个文件一行摘要
    01_pleadings/              <- 起诉状、答辩状、修正诉状
    02_motions/
       <日期>_<方>_<标题>.pdf
    03_discovery/
       outgoing/               <- 您发出的请求
       incoming/               <- 您收到的答复
       privilege_log.xlsx
    04_evidence/
       documents/
       photos/
       audio/
       video/
       hashes.txt              <- 各文件 SHA-256（保管链）
    05_correspondence/
       email/                  <- 导出为 .eml 或 .pdf
       letters/
       text_messages/
    06_court_orders/
    07_briefs_and_exhibits/
       motion_<id>/
          brief.pdf
          ex_A_<简称>.pdf
          ex_B_<简称>.pdf
    08_research/                <- 保存的案例、法规、文章
    09_billing_and_costs/
    10_archive/                 <- 被取代的草稿；勿删除
```

## 命名规范

`YYYY-MM-DD_<发送方>_<短标题>.<扩展名>`

> 示例：`2026-05-08_pl_motion-to-compel-rule-37.pdf`

可排序、可脚本处理，不用空格（用连字符）。

## 备份——3-2-1 原则

- 每份文件 **3** 份拷贝。
- 至少 **2** 种介质（笔记本 SSD + 外置硬盘）。
- **1** 份异地（加密云或亲属处硬盘）。

敏感案件文件宜使用 **端到端加密** 云（Cryptomator + Dropbox、Tresorit 或 Proton Drive），而非明文 Drive/iCloud。

## 追踪文书去向

- 每封发出的邮件：密送自己，导出至 `05_correspondence/email/`。
- 每封认证邮件：将绿色回执扫描至 `05_correspondence/letters/`。
- 每次传真/电子提交确认：保存 PDF 至相应文件夹。

## 草稿版本

- 使用 `_v1`、`_v2`、`_FINAL`、`_FILED` 后缀。
- **已提交** 版本放在 `01_pleadings/`、`02_motions/` 等——勿放 `10_archive/`。
- 更早草稿放入 `10_archive/`。

## 文件完整性

对证据一次性计算哈希，以证明未被篡改：

```sh
# macOS / Linux
find 04_evidence -type f -exec shasum -a 256 {} \; > 04_evidence/hashes.txt
```

```powershell
# Windows PowerShell
Get-ChildItem -Recurse 04_evidence | Get-FileHash -Algorithm SHA256 |
  Export-Csv 04_evidence\hashes.csv
```

## `00_INDEX.md` 应写什么

项目符号列表，每项含日期、发送方、文书标题与链接。每次保存新文件即更新。日后法官问「那份文件究竟在哪里出示的？」时，未来的您会感谢现在的您。

> 仅供参考，不构成法律意见。
