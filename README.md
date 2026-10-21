# 太一

一个纯本地、无需联网、无需登录的 Android 练习记录应用，用于记录每日「静功」与「桩功」的练习时长，并可播放本机导入的引导音频。

- 包名：`com.jinggong.timer`
- 最低版本：Android 8.0（API 26）
- targetSdk：35
- 技术栈：Kotlin 2.x、Jetpack Compose、Material 3、Room、DataStore、Media3 ExoPlayer
- 架构：单 Activity + Navigation Compose，MVVM，手动依赖注入（`AppContainer`）

## 构建

需要 JDK 17+、Android SDK Platform 35、Build Tools 35.0.0。

```bash
./gradlew assembleDebug
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`

已复制的交付 APK：`dist/taiyi-1.0.4-debug.apk`

工作区开发时使用了便携工具链，`local.properties` 指向本机 SDK，不提交到版本库。构建脚本优先使用阿里云 Maven 镜像，缺失时回退 Google Maven / Maven Central。

## 构建验证

- `./gradlew :app:assembleDebug`：成功
- `./gradlew :app:testDebugUnitTest`：13 个测试全部通过
- `./gradlew :app:lintDebug`：0 error（33 个 warning，主要是未使用字符串和图标建议）
- APK：包名 `com.jinggong.timer`，versionName `1.0.4`（versionCode 5），minSdk 26，targetSdk 35，debug 签名，约 20.6 MB
- Manifest 已确认：无 INTERNET 权限；前台 Service `foregroundServiceType=mediaPlayback`、`exported=false`

## 1.0.4 更新

- App 名称改为「太一」。
- 新增外观设置：跟随系统、日间、夜晚三种皮肤模式，可在设置页即时切换。
- 日间模式使用米白纸感背景与深绿色主色，夜晚模式保留原有深色禅意主题。
- 状态栏/导航栏图标会随皮肤自动切换明暗。

## 1.0.3 更新

- 应用图标替换为新的静坐线稿圆形标识，包含自适应图标、圆形图标、启动页图标和 Android 13 主题图标。

## 1.0.2 更新

- 练习暂停时，引导音频同步暂停；继续练习时，如果暂停前音频正在播放，则从原位置继续。
- 练习结束（保存或放弃）时，音频停止并归零；通过通知结束练习也会停止音频。
- 暂停期间切换音频只更新选择，不会在暂停中意外播放。

## 1.0.1 更新

- 练习卡片改为内容自适应高度，修复底部长文本被裁切的问题；移除容易被底栏遮挡的页脚文字。
- 开始计时时默认自动播放当前选择的引导音频；可在设置中关闭「开始计时时自动播放音频」。

## 功能

- 今日页：静功/桩功卡片、今日累计、练习次数、最近练习、目标进度环、连续天数、进行中提示。
- 练习页：单调时钟正计时、开始/暂停/继续/结束、目标时长轻提示、大号计时、屏幕常亮、音频播放与切换。
- 前台 Service：计时与音频共用一个常驻通知，支持暂停/继续/结束；结束会回到练习页进入保存/放弃小结。
- 音频管理：SAF 导入到 `filesDir/audio/`，支持重命名、删除、试听、静功/桩功/通用归属。
- 历史：按日期分组倒序、模式筛选、编辑时长/备注、确认删除。
- 统计：今日/本周/本月/总计、最近 30 天 Canvas 堆叠柱状图、月历热力图、连续与累计天数。
- 数据：全部记录导出为 UTF-8 BOM 的 CSV；清空记录需二次确认。
- 设置：每日目标、默认音频、自动播放、循环、各模式目标、HyperOS 后台引导、关于与隐私说明。

## 计时恢复

- 运行中使用 `SystemClock.elapsedRealtime()`，暂停时把当前片段累加进 `accumulatedMs`。
- 开始、暂停、继续、结束以及每 15 秒检查点写入 DataStore。
- 进程被杀后，同一启动周期内根据单调时钟补算；通过 `Settings.Global.BOOT_COUNT` 检测重启，重启后恢复到最近检查点并暂停，不把关机时间算入练习。
- 计时状态由 `TimerManager` 统一持有，Activity、Compose 与前台 Service 共享同一份 StateFlow。

## 权限

应用不申请互联网访问权限（INTERNET），也不申请存储权限。Media3 会合并一个只读的 `ACCESS_NETWORK_STATE` 权限用于判断网络类型，但它不能访问互联网。

- `POST_NOTIFICATIONS`：Android 13+ 通知权限。拒绝后仍可计时，但常驻通知可能不显示。
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK`：计时与音频前台服务。
- `WAKE_LOCK`：息屏播放音频。
- `VIBRATE`：目标时长到达提示。

## 小米 / HyperOS

为了保证息屏、切后台后计时与音频不被系统中断，建议在系统应用设置中：

1. 省电策略设为「无限制」。
2. 允许「自启动」。
3. 允许通知。

设置页的「后台运行」卡片提供跳转到应用系统设置的按钮。

## CSV

导出使用 `ACTION_CREATE_DOCUMENT`，编码为 UTF-8 BOM，换行 CRLF，表头：

```text
日期,开始时间,模式,时长秒,备注
```

备注中的逗号、双引号、换行会按 CSV 规则转义。

## 数据与隐私

- Room 数据库 `jinggong.db` 与音频文件都保存在应用私有目录。
- `allowBackup=false`，关闭系统自动备份。
- 没有 INTERNET 权限、账号、云同步、埋点或广告。
- 卸载应用会删除全部记录与音频；可使用 CSV 导出自行备份。

## 说明

当前开发环境没有连接 Android 真机。APK 已完成编译与单元测试；息屏 10 分钟、通知按钮、CSV 打开等真机场景需要在小米 14 上验收。
