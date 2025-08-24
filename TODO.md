了解しました。最新版 PaperMC（1.21 系想定 / Java 21）を前提に、**自作プラグイン中心＋一部既存プラグイン連携**で“逃走中”を再現するための**アーキテクチャ設計**と**チケット分割**を提示します。UI には Scoreboard（右パネル）、BossBar、ActionBar、ホログラム、タブリスト拡張を活用します。参考にする公式 API／主要プラグインの要点は都度出典を付けています。

---

# 全体アーキテクチャ

## 技術前提

* **Paper 1.21.x / Java 21**（Paper 公式の Java 21 インストール手順・推奨が明示） ([PaperMC Docs][1])
* 文字列整形／装飾は **Adventure/MiniMessage** を採用（ActionBar/Title/色付け等） ([PaperMC Docs][2], [Adventure][3])
* 右パネルは **Scoreboard API**、残り時間バーは **BossBar API** を利用 ([hub.spigotmc.org][4])
* リソースパックは **Player#addResourcePack / setResourcePack** を使用（強制配布・プロンプト可） ([yama2211.jp][5], [jd.papermc.io][6])
* エリア制御は **WorldGuard** のリージョン／フラグ連携（入場制御・PVP など） ([WorldGuard][7])
* 経済・所持金は **Vault** 経由で任意エコノミーと連携 ([SpigotMC][8], [Bukkit][9], [GitHub][10])
* プレースホルダは **PlaceholderAPI**（自作 Expansion で `%rfm_*%` を提供） ([wiki.placeholderapi.com][11], [SpigotMC][12])
* ホログラムは **DecentHolograms**（API あり・PAPI 対応、HolographicDisplays は非推奨化により代替） ([DecentHolograms Wiki][13], [Bukkit][14])
* TAB 表示拡張（任意）：**TAB** プラグインの API/Placeholders でタブリストやネームタグを強化 ([GitHub][15])

> ハンター NPC は **Citizens API** を前提とし（ウェイポイント巡回など）、戦闘 AI を外部に頼らず**自作**（視認/FOV/確保半径など）で差別化。必要に応じて **Sentinel** や **MythicMobs** で補助可能。 ([シティズンズウィキ][16], [SpigotMC][17], [Hangar][18])

---

## モジュール分割（自作プラグイン内パッケージ）

```
jp.ertl.rfm
├─ core         : ラン/ルーム管理、状態遷移、タイマー、イベントBus
├─ ui           : Scoreboard/BossBar/ActionBar/Title/音/粒子
├─ mission      : ルールエンジン（Trigger/Condition/Action）、YAMLローダ
├─ hunter       : HunterController（抽象）/ CitizensAdapter / 追跡アルゴリズム
├─ region       : WorldGuardAdapter（入退場・封鎖・一時解放）
├─ econ         : VaultAdapter（積算賞金→通貨換算）
├─ placeholder  : PAPI Expansion（%rfm_timeleft% 等）
├─ pack         : ResourcePackManager（配布・適用・状態イベント）
├─ storage      : SQLite 実装（結果/ランキング/設定キャッシュ）
├─ admin        : コマンド/権限/プリセット
└─ api          : 他プラグイン連携用 API（公開イベント/フック）
```

### 主要コンポーネントの責務

* **Core**：ゲーム状態（LOBBY→RUNNING→RESULT）、残時間・賞金の 1s Tick、捕捉/自首/離脱の集約。
* **UI**：

  * Scoreboard 右パネルに「残り時間／現賞金／逃走者数／ハンター数／ミッション要約」を1秒更新。 ([hub.spigotmc.org][4])
  * BossBar で残時間をプログレス表示・イベント時に色/タイトル変更。 ([hub.spigotmc.org][19])
  * ActionBar/Title は Adventure を通す（色・クリック/hover ヒント）。 ([PaperMC Docs][2], [Adventure][3])
* **Mission**：データ駆動（`missions/*.yml`）。Trigger（時間/残人数/位置/インタラクト）→Condition→Action（エリア開放/ハンター追加/賞金倍率/救出など）。
* **Hunter**：

  * 巡回：Citizens Waypoints を使用（速度・ウェイポイント・トリガ） ([シティズンズウィキ][20])
  * 視認：**FOV + 距離 + Line-of-Sight（RayTrace）** で判定（Bukkit RayTrace/hasLineOfSight を利用）。 ([hub.spigotmc.org][21], [jd.papermc.io][22])
  * 追跡：目標プレイヤーへ逐次パス再計算 or 追尾速度ブースト。一定距離/角度内で**確保**イベント。
* **Region**：WorldGuard フラグの切替（`entry`, `pvp`, `greeting` 等）、ミッションで一時封鎖/解放。 ([WorldGuard][7])
* **Econ**：Vault でサーバ通貨と接続（終了時に換金）。 ([SpigotMC][8])
* **Placeholder**：自作 Expansion を提供（TAB/DecentHolograms から参照可）。 ([wiki.placeholderapi.com][11], [GitHub][23])
* **Pack**：番組風 HUD アイコン/効果音を **addResourcePack** で配信（強制可・プロンプト文言）。状態は **PlayerResourcePackStatusEvent** で監視。 ([yama2211.jp][5], [Bukkit][24])
* **Hologram**：DecentHolograms API（DHAPI）で自首端末やミッション端末の 3D 見出し/進捗表示。クリックアクションで進行。 ([DecentHolograms Wiki][25])

---

## 外部プラグイン採用方針

* **必須（推奨）**：WorldGuard／Vault／PlaceholderAPI／DecentHolograms
  （`plugin.yml` は **softdepend** で順序制御）
* **任意**：Citizens（NPC）、TAB（タブ/ネームタグ強化）、MythicMobs or Sentinel（AI補助） ([シティズンズウィキ][16], [GitHub][15], [Hangar][18], [SpigotMC][17])

---

# 主要データモデル（抜粋）

* `GameSession{ id, state, startTime, endTime, bountyPerSec, areaId, rulesetId }`
* `RunnerState{ uuid, status(ALIVE|CAUGHT|SURRENDERED), currentBounty, lastSeen }`
* `HunterState{ npcId, mode(PATROL|CHASE), fovDeg, viewDistance, patrolRouteId }`
* `Mission{ id, name, trigger, conditions[], actions[], ui {title, desc, bossbar?} }`
* `Trigger{ type(TIME|PLAYER_COUNT|LOCATION|INTERACT|CUSTOM), params }`
* `Action{ type(OPEN_GATE|ADD_HUNTER|MULTIPLY_BOUNTY|UNFREEZE|MESSAGE|REGION_FLAG), params }`

---

# ゲームループ（秒単位）

1. `core.tick()`：残時間/賞金の加算、ミッション進行チェック。
2. `hunter.scan()`：NPC ごとに **距離前処理**→**FOV**→**RayTrace** の順で軽量判定（TPS 低下回避）。 ([hub.spigotmc.org][21])
3. `ui.push()`：Scoreboard/BossBar/ActionBar 更新。 ([hub.spigotmc.org][4])

---

# UI 仕様（リッチ演出）

* **Scoreboard（右）**：

  * `RUN FOR MONEY`／`残り: 12:34`／`賞金: 123,400`／`逃走: 7/12`／`ハンター: 4`／`MISSION: XXX`（PAPI 置換も可）。 ([hub.spigotmc.org][4])
* **BossBar**：残時間割合（イベント時に色/タイトル変更）。 ([hub.spigotmc.org][19])
* **ActionBar/Title**：`ハンター接近 28m`、`自首成立` 等を瞬時に表示（Adventure）。 ([PaperMC Docs][2])
* **ホログラム**（DecentHolograms）：自首端末やミッション端末に 3D 表示＋クリックアクション。 ([DecentHolograms Wiki][25])
* **タブリスト/TAB**（任意）：`[ALIVE]`/`[CAUGHT]` 表示、順位・賞金の並び替え。 ([GitHub][15])
* **リソースパック**：番組風フォント/アイコン適用（`addResourcePack(..., force)`）。 ([yama2211.jp][5])

---

# 安定運用と拡張

* **WorldGuard で逸脱・PVP 無効**（イベントで一時切替） ([WorldGuard][7])
* **Vault 連携で換金**／**SQLite** でランキング永続化 ([SpigotMC][8])
* **PlaceholderAPI Expansion** で `%rfm_*%` を他プラグインに提供（TAB/ホログラム/看板GUIなど） ([wiki.placeholderapi.com][11])
* **パフォーマンス**：視認判定は距離→FOV→RayTrace の段階化（RayTrace は高コストのためサンプリング） ([hub.spigotmc.org][21])

---

# チケット分割（エピック→ストーリー）

## EPIC A：基盤・環境整備

* A-1 Paper 1.21.x サーバ＆Java 21 準備（headless 回避・起動確認）〔完了条件：`java -version` が 21、Paper 起動ログ確認〕 ([PaperMC Docs][1])
* A-2 リポジトリ／CI（Gradle + Paper API + Spotless/Checkstyle）
* A-3 `plugin.yml` と基本イベント配線（enable/disable, command/perm）

## EPIC B：ゲームコア

* B-1 状態機械（LOBBY/COUNTDOWN/RUNNING/RESULT）
* B-2 タイマ＆賞金加算（1s Tick）
* B-3 自首（地点/コマンド/GUI の 3 入口）
* B-4 捕捉イベント（確保→観戦移行）
* B-5 結果集計・ロギング（SQLite）

## EPIC C：UI

* C-1 **Scoreboard** 実装（右パネル・1s 更新・テンプレ構成） ([hub.spigotmc.org][4])
* C-2 **BossBar**（残時間プログレス／色変更 API） ([hub.spigotmc.org][19])
* C-3 **ActionBar/Title**（Adventure + MiniMessage） ([PaperMC Docs][2], [Adventure][3])
* C-4 **PlaceholderAPI Expansion**（`%rfm_timeleft%`, `%rfm_bounty%` ほか） ([wiki.placeholderapi.com][11])
* C-5 **DecentHolograms** 連携（自首端末・ミッション端末・クリックアクション） ([DecentHolograms Wiki][25])
* C-6 **リソースパック** 適用（`addResourcePack` とステータス監視） ([yama2211.jp][5], [Bukkit][24])

## EPIC D：ミッションエンジン

* D-1 DSL 設計（Trigger/Condition/Action の YAML スキーマ）
* D-2 時間トリガ／人数トリガ／地点トリガ／インタラクトトリガ
* D-3 代表アクション：エリア解放(WorldGuard)、ハンター追加、倍率変更、メッセージ、報酬、失敗ペナルティ ([WorldGuard][7])
* D-4 UI 連動（BossBar テーマ、ActionBar 告知、ホログラム更新）

## EPIC E：ハンター（NPC）

* E-1 **Citizens** 連携アダプタ（生成・破棄・ウェイポイント登録） ([シティズンズウィキ][16])
* E-2 視認判定（距離→FOV→RayTrace：`World#rayTraceEntities` 等） ([hub.spigotmc.org][21])
* E-3 追跡アルゴリズム（ターゲット更新・見失い処理・確保半径）
* E-4 巡回→追跡→復帰の状態遷移／TPS 負荷制御（サンプリング間引き）
* E-5 （任意）**Sentinel/MythicMobs** 連携の切替オプション（性能比較） ([SpigotMC][17], [Hangar][18])

## EPIC F：リージョン・ギミック

* F-1 WorldGuard アダプタ（入退制御・PVP・メッセージ） ([WorldGuard][7])
* F-2 ミッションからのフラグ切替（封鎖/解放）

## EPIC G：経済・ランキング

* G-1 Vault 連携（換金比率・支払い／回収） ([SpigotMC][8])
* G-2 ランキング（総獲得／最長生存／貢献度）保存・表示（PAPI/TAB で露出） ([GitHub][23])

## EPIC H：管理運用

* H-1 管理コマンド `start/stop/pause/resume/mission/hunter`
* H-2 権限ノード設計（LuckPerms 利用前提）
* H-3 設定ホットリロード／プリセット（EASY/NORMAL/HARD）

---

# 受け入れ基準（例）

* **B-2**：1 秒ごとに全 ALIVE 逃走者へ `bountyPerSec` を積算し、Scoreboard と PAPI が同期表示（手動/自動テスト）。 ([hub.spigotmc.org][4])
* **C-6**：`/rfm pack test` 実行で対象プレイヤーにリソースパック適用→ `PlayerResourcePackStatusEvent` が `SUCCESSFULLY_LOADED` を返す。 ([Bukkit][24])
* **E-2**：ハンターの視認は **距離 30m・FOV 70°・RayTrace 遮蔽**を満たす時のみ CHASE に遷移。RayTrace の最大距離・レイトレ頻度は設定で調整。 ([hub.spigotmc.org][21])
* **F-1**：`/rg flag rfm_arena entry deny` 状態で入場不可、ミッション成功で `entry allow` に切替し通知。 ([WorldGuard][7])
* **G-1**：ゲーム終了時、Vault 経由で賞金の換金が記録される（残高増分・トランザクションログ）。 ([SpigotMC][8])

---

# 設定ファイルひな型（要点）

* `config.yml`：時間、賞金単価/上限、FOV/視認距離、確保半径、更新間隔、外部プラグイン連携フラグ。
* `missions/*.yml`：

  ```yaml
  id: open_gate_1
  trigger: { type: time, at: "T+05:00" }
  conditions: [ { type: players_alive_min, value: 5 } ]
  actions:
    - { type: region_flag, region: "rfm_gate", flag: "entry", value: "allow" }  # WorldGuard
    - { type: message, scope: all, text: "<gold>ゲートが開放された！" }          # Adventure
    - { type: bossbar, style: success, duration: 5 }
  ```

  （※ 実処理は上記モジュールに委譲） ([WorldGuard][7], [PaperMC Docs][2])

---

# リソースパック配信（オプション設計）

* **サーバ直配**：`Player#addResourcePack(UUID, url, hash, prompt, force)` を使用。 ([yama2211.jp][5])
* **管理プラグイン併用**：外部の ResourcePack 管理プラグイン（内蔵 HTTP / 複数パック管理）も選択肢。 ([SpigotMC][26])

---

# リスク/留意

* **NPC 負荷**：Citizens 大量配置は TPS を圧迫しやすい → 視認チェックの間引き（例：0.2s〜0.5s）と**距離→FOV→RayTrace**の段階化で軽量化。 ([hub.spigotmc.org][21])
* **HolographicDisplays** は非メンテのため **DecentHolograms** を推奨。 ([Bukkit][14])
* **Java 21 前提**（Paper 公式ガイドは Java 21 を案内）— サーバの JDK を統一。 ([PaperMC Docs][1])

---

# 初期 3 スプリント計画（例）

**Sprint 1（基盤＋MVP UI）**

* A-1/A-2/A-3、B-1/B-2/B-4、C-1/C-2、F-1（リージョン基本）
* 成果：1 マップで「カウントダウン→逃走→捕捉→結果」まで通る。Scoreboard/BossBar 稼働。 ([hub.spigotmc.org][4])

**Sprint 2（ミッション＆NPC）**

* D-1〜D-3、E-1/E-2/E-3、C-3（ActionBar/Title）、C-4（PAPI）
* 成果：時間トリガの開放/追加ハンター／メッセージ演出、Citizens 巡回→追跡、近接確保。 ([シティズンズウィキ][20])

**Sprint 3（リッチ化）**

* C-5（ホログラム端末）、C-6（リソースパック）、G-1/G-2（経済・ランキング）、E-4（負荷最適化）、E-5（AI オプション） ([DecentHolograms Wiki][25], [yama2211.jp][5], [SpigotMC][8])

---

必要であれば、上記の **各チケットに詳細な受け入れ条件**（ログ出力、TPS メトリクス、UI スクショ要件）や **Gradle 雛形/`plugin.yml` 雛形**まで即時用意します。導入予定の既存プラグイン（Citizens/TAB など）の採否だけ教えていただければ、`softdepend` と API 取り込みの形を最適化して提示します。

[1]: https://docs.papermc.io/misc/java-install/ "Installing or updating Java | PaperMC Docs"
[2]: https://docs.papermc.io/paper/dev/command-api/arguments/adventure/?utm_source=chatgpt.com "Adventure | PaperMC Docs"
[3]: https://docs.advntr.dev/minimessage/index.html?utm_source=chatgpt.com "MiniMessage - Adventure Documentation (v4.24.0)"
[4]: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/scoreboard/Scoreboard.html?utm_source=chatgpt.com "Scoreboard (Spigot-API 1.21.8-R0.1-SNAPSHOT API)"
[5]: https://yama2211.jp/document/spigot-javadoc/1.20.5/org/bukkit/entity/Player.html?utm_source=chatgpt.com "Player (Spigot-API 1.20.5-R0.1-SNAPSHOT API)"
[6]: https://jd.papermc.io/paper/1.21.8/org/bukkit/entity/Player.html?utm_source=chatgpt.com "Player (paper-api 1.21.8-R0.1-SNAPSHOT API)"
[7]: https://worldguard.enginehub.org/en/latest/regions/flags/?utm_source=chatgpt.com "Region Flags - WorldGuard 7.0 documentation - EngineHub"
[8]: https://www.spigotmc.org/resources/vault.34315/?utm_source=chatgpt.com "Vault | SpigotMC - High Performance Minecraft Community"
[9]: https://dev.bukkit.org/projects/vault?utm_source=chatgpt.com "Overview - Vault - Bukkit Plugins - Projects - Bukkit"
[10]: https://github.com/MilkBowl/VaultAPI?utm_source=chatgpt.com "GitHub - MilkBowl/VaultAPI: API Component of Vault"
[11]: https://wiki.placeholderapi.com/developers/creating-a-placeholderexpansion/?utm_source=chatgpt.com "PlaceholderAPI Wiki - Creating a PlaceholderExpansion"
[12]: https://www.spigotmc.org/wiki/placeholderapi-expansion-tutorial/?utm_source=chatgpt.com "PlaceholderAPI expansion tutorial - SpigotMC"
[13]: https://wiki.decentholograms.eu/?utm_source=chatgpt.com "Welcome - DecentHolograms Wiki"
[14]: https://dev.bukkit.org/projects/holographic-displays?utm_source=chatgpt.com "Overview - Holographic Displays - Bukkit Plugins - Projects ..."
[15]: https://github.com/NEZNAMY/TAB/wiki/Developer-API?utm_source=chatgpt.com "Developer API · NEZNAMY/TAB Wiki · GitHub"
[16]: https://wiki.citizensnpcs.co/API?utm_source=chatgpt.com "API - Citizens Wiki"
[17]: https://www.spigotmc.org/resources/sentinel.22017/?utm_source=chatgpt.com "Sentinel | SpigotMC - High Performance Minecraft Community"
[18]: https://hangar.papermc.io/Lumine/MythicMobs?utm_source=chatgpt.com "MythicMobs - Paper Plugin | Hangar"
[19]: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/boss/BossBar.html?utm_source=chatgpt.com "BossBar (Spigot-API 1.21.8-R0.1-SNAPSHOT API)"
[20]: https://wiki.citizensnpcs.co/Waypoints?utm_source=chatgpt.com "Waypoints - Citizens Wiki"
[21]: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/util/class-use/RayTraceResult.html?utm_source=chatgpt.com "Uses of Class org.bukkit.util.RayTraceResult (Spigot-API 1. ..."
[22]: https://jd.papermc.io/paper/1.21.8/org/bukkit/World.html?utm_source=chatgpt.com "World (paper-api 1.21.8-R0.1-SNAPSHOT API)"
[23]: https://github.com/NEZNAMY/TAB/wiki/Placeholders?utm_source=chatgpt.com "Placeholders · NEZNAMY/TAB Wiki · GitHub"
[24]: https://bukkit.windit.net/javadoc/org/bukkit/event/player/package-summary.html?utm_source=chatgpt.com "org.bukkit.event.player (Spigot-API 1.21.6-R0.1-SNAPSHOT ..."
[25]: https://wiki.decentholograms.eu/api/basic-usage/dhapi/?utm_source=chatgpt.com "DHAPI - DecentHolograms Wiki"
[26]: https://www.spigotmc.org/resources/resourcepack-managing-your-resource-packs.126710/?utm_source=chatgpt.com "ResourcePack - Managing Your Resource Packs - SpigotMC"
