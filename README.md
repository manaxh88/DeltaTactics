# 三角洲战术助手 (DeltaTactics)

<p align="center">
  <b>针对第一人称硬核战术射击游戏《三角洲行动》（Delta Force）打造的现代化 Android 战术搞钱辅助工具</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin-purple.svg" alt="Language" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg" alt="Compose" />
  <img src="https://img.shields.io/badge/Version-v2.8.6-orange.svg" alt="Version" />
</p>

---

## 🎯 核心功能一览

### 1. 避难所特勤处制造利润排行榜
- **4 大工作台分类**：防具台、弹药台、医疗台、枪械台实时制造方案。
- **双维度智能排序**：支持按「总净利润」（单批赚取最多）与「时薪/h」（单位时间产出最高）自由倒排。
- **原材料清单展开**：点击卡片即时展示合成该物品所需的各项原料明细。

### 2. 高级子弹自选包收益榜
- **多档位覆盖**：收录 3 级自选包、4 级自选包、5 级自选包及通行证高级自选包。
- **首选推荐横幅**：自动测算各口径单发市价与整包发数，标出当前拍卖行变现收益最高的最优解（如 3 级首选 `.300 BLK`，4 级首选 `7.62×51mm M80`）。

### 3. 鼠鼠卡战备计算器
- **5 大价值门槛**：11W（机密大坝/长弓）、18W（航天绝密）、55W、60W、78W（潮汐监狱）。
- **三大流派预设**：枪械流、防具均衡流、极速跑刀流。
- **假账配平机制**：自动计算离达标线还缺多少哈夫币，智能推荐低成本假账配件快速凑满战备。

### 4. S11 赛季任务全题库
- **116 题四阶段全收录**：沉舟、星火、面具、众生全阶段任务与正确答案。
- **搜索与快速定位**：支持关键词实时模糊过滤检索。
- **进度持久化**：支持任务完成状态勾选并本地存储，提供一键重置功能。

### 5. 局内摩斯电码智能解码器
- **滴/嗒即时录入**：专为局内门锁电码设计，短音（•）与长音（-）轻按录入。
- **自动数字解码**：每输入 5 音自动解算对应 1 位阿拉伯数字，支持一键复制 4 位密码。

### 6. 每日密码与热门配装
- **每日密码**：零号大坝、长弓溪谷、巴克什、航天基地、潮汐监狱、AZ3 密码大号直观展示。
- **热门枪械配装**：主流改枪方案与改枪码一键复制。
- **武器属性对比**：主流突击步枪与冲锋枪射速、伤害、射程与操控横向对比。

---

## 🛠 技术架构与技术栈

- **架构设计**：MVI / MVVM + Clean Architecture + 离线优先（Offline-First）
- **开发语言**：Kotlin 2.0+
- **界面开发**：Jetpack Compose + Material Design 3
- **异步处理**：Kotlin Coroutines + Flow + StateFlow
- **持久化**：Android SharedPreferences / Local JSON Cache
- **系统适配**：Android Edge-to-Edge 边到边沉浸式布局，适配系统手势小白条与挖孔屏
- **性能优化**：硬加速线条边框替代高耗阴影重绘，冷启动首帧零延迟，全界面 60/120 FPS 极速丝滑

---

## 🏗 本地编译与构建

### 环境要求
- Android Studio Ladybug (2024.2+) 或更高版本
- JDK 17
- Android SDK 34 (Android 14)
- Gradle 8.7+

### 构建步骤
```bash
# 克隆仓库
git clone <your-repo-url>
cd keen-faraday

# 编译 Debug APK
./gradlew assembleDebug

# 构建产物路径
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 开源许可

本项目遵循 MIT 开源许可证。
