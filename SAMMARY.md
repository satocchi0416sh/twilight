# 調査サマリ（番組「逃走中」の要点）

* 限られたエリア内で“逃走者”が“ハンター”から逃げ切るゲーム。時間が進むほど賞金が積み上がり、最後まで残れば総取り。途中で捕まるとゼロ。番組中には多様な「ミッション」や「追加ハンター投入」「救出」などのイベントが入る。公式でも“限られたエリア”“ハンター”“ミッション”“心理戦”が中核だと説明されている。 ([フジテレビ][1], [Japan Program Catalog][2])
* 賞金は秒単位で積算（例：100円/秒）というフォーマットが多く、回によって上限や単価は変動。 ([TV Tropes][3], [The Review Geek][4], [ウィキペディア][5])
* 途中リタイアで“その時点までの賞金を確定させて離脱”できる「自首」ルールがある（場所・手続きが指定されることが多い）。 ([フジテレビ][6], [アットウィキ][7])
* ミッションの典型：ハンター放出／追加投入の阻止、強制失格系、仲間救出、封鎖解除・エリア開放、アイテム獲得など。 ([アノポスト][8], [アットウィキ][9])

---

# 要件定義（PaperMC 用「逃走中」再現プラグイン）

## 1) 基本ゲームフロー

* **ロビー → チュートリアル → ゲーム開始カウントダウン → 逃走フェーズ（ミッション挿入） → 終了判定 → リザルト**
* **役割**：逃走者／ハンター（NPC）。ハンターは所定のルート巡回→視認で追跡。視認判定は\*\*視野角＋視線遮蔽（LoS）\*\*で実装。*（番組演出上“視界に捉えてから追跡”の要素を模倣）*。
* **賞金**：ルーム設定で「単価（円/秒）」「上限」「段階的ボーナス」を指定。サーバー内通貨（Vault 経由）へ換金可能。 ([SpigotMC][10])
* **自首システム**：専用地点/GUI/コマンド（例：/surrender）で離脱し、積算賞金を確定。

## 2) 表示/UI（“右パネル”含むリッチ演出）

* **右サイドバー（Scoreboard）**：残り時間、現在賞金、ハンター数、ミッション進行状況、仲間の残存数など。PaperのScoreboard APIで実装。 ([jd.papermc.io][11])
* **BossBar**：残り時間の進捗や、追跡警告（色・タイトル変更で緊迫感）。 ([jd.papermc.io][12])
* **ActionBar/Title**：ミッション開始・更新・失敗/成功、近傍ハンター警告などの瞬間通知は Adventure API で。 ([Adventure][13], [PaperMC][14])
* **ホログラム**：自首地点やミッション端末に立体見出し・カウント表示。既存 Holograms 系と連携可能。 ([Hangar][15])
* **TAB/スコアボード拡張**：タブリストにプレイヤー状態（逃走中/確保/離脱）やチーム色を表示。 ([Hangar][16])
* **リソースパック配信**：番組風フォント/アイコン（ハンター警戒アイコン等）を `Player#addResourcePack(..., force)` でプッシュ。 ([jd.papermc.io][17])

## 3) ハンター（NPC）実装

* **NPC基盤**：Citizens を前提に、追跡/戦闘は Sentinel もしくは自前AIを併用。パトロール → 視認 → 追跡 → 近接“確保”判定。 ([シティズンズウィキ][18], [SpigotMC][19])
* **高度AI（任意）**：MythicMobs のAIゴール・スキルで加速や短時間スタン耐性等を付与し、難度調整。 ([Hangar][20], [GitLab][21])
* **パフォーマンス**：同時NPC数を上限管理、巡回はウェイポイント+チャンク内優先、視認は一定tick間引き。

## 4) エリア・ギミック

* **ゲームエリア/封鎖**：WorldGuard で入退制御・PvP・飛行などのフラグ設定。ミッションで一時解放/封鎖切替。 ([WorldGuard][22], [Bukkit][23])
* **自首/救出/端末**：指定地点にホログラム＋看板GUI。ミッション端末はインタラクトで進行。 ([Hangar][15])
* **アイテム**：冷凍弾/煙幕/デコイ等の“番組風”アイテム（リソースパックのアイコン＋クールダウンUI）。※公式アプリでも“冷凍銃”等の演出があるため雰囲気再現に好適。

## 5) ミッション・イベント（データ駆動）

* **種類例**

  * **放出阻止/追加投入**：タイマー内に端末を同時起動→成功でハンター投入数-α。
  * **強制失格系**：未達で対象者“失格”（観戦へ）。 ([アットウィキ][9])
  * **救出**：拘束エリアへ到達し一定時間滞在で仲間を解放。
  * **封鎖解除**：通行不能ゲートの解錠。
  * **賞金変動**：一定時間“単価2倍”や“ボーナス一括付与”。
* **編集性**：YAML/JSONで「トリガー（時刻/残人数/地点）」「ゴール」「報酬/ペナルティ」「表示文言（MiniMessage）」を定義。典型ミッションの参考は番組解説記事をベースにプリセット化。 ([アノポスト][8])

## 6) 賞金・経済・ランキング

* **内部ポイント**を秒積算し、終了時に **Vault 互換エコノミーへ換金（比率可変）**。外部看板/メニューでも残高表示。 ([SpigotMC][10])
* **ランキング**：総獲得、最長生存、ミッション貢献度。PlaceholderAPIでスコアボードやTABに差し込み。 ([GitHub][24])

## 7) 管理者機能

* **コマンド**：`/rfm start|stop|pause|resume|addhunter|setbounty|mission <id> ...`
* **パラメータ**：時間、単価、ハンター数上限、エリアID、ミッション有効/無効、NPC速度など。
* **権限**：LuckPerms 推奨。 ([LuckPerms][25])
* **プリセット**：ビギナー/通常/ハード（ハンター視認距離・巡回密度・ミッション密度）。

## 8) 互換性・技術前提

* **Paper 1.21.x / Java 21 以上**（Paper公式ドキュメントでも Java 21 を明記）。 ([PaperMC Docs][26])
* 推奨外部：Citizens、Sentinel（または自前AI）、WorldGuard、PlaceholderAPI、TAB、Holograms、任意の Vault 互換エコノミー。 ([シティズンズウィキ][18], [SpigotMC][19], [Bukkit][23], [GitHub][24], [Hangar][16])

---

# 画面設計（例）

* **右サイドバー**：

  * タイトル：`run for money`
  * `残り時間`／`現賞金`（積算表示）／`ハンター`（生存/合計）／`逃走者`（生存/合計）／`ACTIVE MISSION` 要約
  * 行更新は1秒間隔（負荷軽減）。Paper Scoreboard API で実装。 ([jd.papermc.io][11])
* **BossBar**：残り時間割合、ミッション中は色変更＋タイトルで強調。 ([jd.papermc.io][12])
* **ActionBar**：`ハンター接近！ 27m` など距離警告、クールダウン、自首成立通知。Adventureで送信。 ([Adventure][13])
* **ホログラム**：自首地点「現在賞金：xxxxx」、端末「\[E]で起動（残り20秒）」等。 ([Hangar][15])

---

# データ・拡張性

* **設定**：`config.yml`（ゲーム共通）、`missions/*.yml`、`waypoints/*.yml`（巡回経路）、`rewards.yml`。
* **API**：ミッション/イベントはリスナーで拡張可能（他プラグインからトリガー発火）。
* **プレースホルダ**：`%rfm_timeleft%`, `%rfm_bounty%`, `%rfm_hunters%` などを PlaceholderAPI に登録。 ([GitHub][24])

---

# セキュリティ/運営

* **エリア逸脱やフライ**：WorldGuard フラグで抑止、必要に応じて Extra Flags 系で細かく制御。 ([WorldGuard][22], [SpigotMC][27])
* **パフォーマンス**：NPC視認は距離・FOVで前処理、ハンターAI tick レート間引き、チャンク外非アクティブ化。
* **ログ**：確保時の座標・原因、ミッション成否、賞金確定ログを残す。

---

# 実装フェーズ提案（短期→豪華化）

1. **MVP**：

   * 基本ルール（時間・賞金・自首・確保）、右サイドバー、BossBar、1～2種ミッション、WorldGuard連携、Citizensハンター（単純追跡）。
2. **演出強化**：

   * ActionBar/Title、ホログラム端末、TAB拡張、アイテム（冷凍/煙幕）とクールダウンUI、難度プリセット。
3. **高度化**：

   * MythicMobsでAI強化、複合ミッション（協力/投票）、ランキング/経済連携、リソースパックで番組風HUD。 ([Hangar][20])

---

必要なら、この要件をそのまま**チケット分割（タスク化）**と**設定ファイルの雛形**まで落とし込みます。サーバーのバージョン（Paperの細番）と導入予定プラグインの有無（Citizens・WorldGuard 等）を教えてください。

[1]: https://www.fujitv.co.jp/tosochu/top.html?utm_source=chatgpt.com "逃走中 - フジテレビ"
[2]: https://www.japan-programcatalog.com/en/program/runformoney?utm_source=chatgpt.com "Run For Money - Japan Program Catalog"
[3]: https://tvtropes.org/pmwiki/pmwiki.php/Series/RunForMoneyTousouchuu?utm_source=chatgpt.com "Run For Money: Tousouchuu (Series) - TV Tropes"
[4]: https://www.thereviewgeek.com/runforthemoney-s1review/?utm_source=chatgpt.com "Run for the Money Season 1 Review - Sprinting to game ..."
[5]: https://fr.wikipedia.org/wiki/Run_for_Money?utm_source=chatgpt.com "Run for Money — Wikipédia"
[6]: https://www.fujitv.co.jp/tosochu/about.html?utm_source=chatgpt.com "逃走中｜逃走中とは? - フジテレビ"
[7]: https://w.atwiki.jp/aniwotawiki/pages/48456.html?utm_source=chatgpt.com "自首 (run for money 逃走中) - アニヲタWiki (仮) - atwiki（アット ..."
[8]: https://umiwave.work/tosochu-mission/?utm_source=chatgpt.com "逃走中のミッション一覧！これまでのミッション内容をすべて ..."
[9]: https://w.atwiki.jp/chronosplayer/pages/978.html?utm_source=chatgpt.com "強制失格ミッションまとめ - クロノス参戦プレイヤーwiki | 逃走 ..."
[10]: https://www.spigotmc.org/resources/vault.34315/?utm_source=chatgpt.com "Vault | SpigotMC - High Performance Minecraft Community"
[11]: https://jd.papermc.io/paper/1.21.4/org/bukkit/scoreboard/Scoreboard.html?utm_source=chatgpt.com "Scoreboard (paper-api 1.21.4-R0.1-SNAPSHOT API)"
[12]: https://jd.papermc.io/paper/1.21.4/org/bukkit/boss/BossBar.html?utm_source=chatgpt.com "BossBar (paper-api 1.21.4-R0.1-SNAPSHOT API)"
[13]: https://docs.advntr.dev/?utm_source=chatgpt.com "Adventure Documentation (v4.24.0)"
[14]: https://forums.papermc.io/threads/unable-to-send-an-action-bar-to-a-player-with-the-player-sendactionbar-method.851/?utm_source=chatgpt.com "Solved - PaperMC"
[15]: https://hangar.papermc.io/TheNextLvl/Holograms?utm_source=chatgpt.com "Holograms - Paper Plugin | Hangar"
[16]: https://hangar.papermc.io/NEZNAMY/TAB?utm_source=chatgpt.com "TAB - Paper Plugin | Hangar"
[17]: https://jd.papermc.io/paper/1.21.1/org/bukkit/entity/Player.html?utm_source=chatgpt.com "Player (paper-api 1.21.1-R0.1-SNAPSHOT API)"
[18]: https://wiki.citizensnpcs.co/Commands?utm_source=chatgpt.com "Commands - Citizens Wiki"
[19]: https://www.spigotmc.org/resources/sentinel.22017/?utm_source=chatgpt.com "Sentinel | SpigotMC - High Performance Minecraft Community"
[20]: https://hangar.papermc.io/Lumine/MythicMobs?utm_source=chatgpt.com "MythicMobs - Paper Plugin | Hangar"
[21]: https://git.lumine.io/mythiccraft/MythicMobs/-/wikis/Mobs/AI?version_id=66ed980950b56a16addbd0eab6607537bd461e51&utm_source=chatgpt.com "AI · Wiki · MythicCraft / MythicMobs · GitLab"
[22]: https://worldguard.enginehub.org/en/latest/regions/flags/?utm_source=chatgpt.com "Region Flags - WorldGuard 7.0 documentation - EngineHub"
[23]: https://dev.bukkit.org/projects/worldguard?utm_source=chatgpt.com "Overview - WorldGuard - Bukkit Plugins - Projects - Bukkit"
[24]: https://github.com/PlaceholderAPI/PlaceholderAPI/wiki/Placeholders?utm_source=chatgpt.com "Placeholders · PlaceholderAPI/PlaceholderAPI Wiki · GitHub"
[25]: https://luckperms.net/?utm_source=chatgpt.com "LuckPerms"
[26]: https://docs.papermc.io/paper/getting-started/?utm_source=chatgpt.com "Getting started - PaperMC Docs"
[27]: https://www.spigotmc.org/resources/worldguard-extra-flags.4823/?utm_source=chatgpt.com "WorldGuard Extra Flags | SpigotMC - High Performance ..."
