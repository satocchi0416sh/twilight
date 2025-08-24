# RunForMoney - 逃走中 Minecraft Plugin

PaperMC 1.21.x 向けの「逃走中」番組再現プラグインです。

## 機能

### 🏃 ゲームシステム
- **リアルタイム賞金システム**: 秒単位で賞金が積み上がります（デフォルト: ¥100/秒）
- **自首システム**: いつでも自首して賞金を確定できます (`/surrender`)
- **観戦モード**: 確保されたプレイヤーは自動的に観戦モードに移行

### 👁️ ハンターシステム
- **高度なNPC AI**: 巡回 → 視認 → 追跡 → 確保の完全自動化
- **リアルタイム視認判定**: FOV + 距離 + RayTrace による正確な視線判定
- **追跡システム**: 見失っても一定時間は最後の位置を追跡

### 🎯 ミッションシステム
- **データ駆動設計**: YAML設定ファイルで柔軟なミッション定義
- **多様なトリガー**: 時間、プレイヤー数、確率、位置などに対応
- **豊富なアクション**: ハンター追加、賞金変動、エリア制御など

### 🖥️ リッチUI
- **右サイドバー**: 残り時間、現在賞金、逃走者数、ハンター数をリアルタイム表示
- **BossBar**: 残り時間の進捗表示とミッション通知
- **ActionBar**: ハンター接近警告、ミッション状況などの瞬間表示
- **PlaceholderAPI**: `%rfm_*%` プレースホルダーで外部連携

### 🏆 統計・ランキング
- **詳細統計**: 総獲得賞金、勝利数、最長生存時間などを記録
- **ランキング表示**: 各種統計のトップ10を表示
- **データ永続化**: SQLiteによる安全なデータ保存

## 必要環境

- **Paper 1.21.x** (Java 21以上)
- **必須依存**: なし (スタンドアロン動作可能)

## 推奨プラグイン

- **WorldGuard**: エリア制御機能
- **Vault**: 経済システム連携  
- **PlaceholderAPI**: プレースホルダー機能
- **Citizens**: 高度なNPC制御
- **DecentHolograms**: 3Dホログラム表示

## インストール

1. [Releases](../../releases) から最新版をダウンロード
2. `plugins/` フォルダに `.jar` ファイルを配置
3. サーバーを再起動
4. `/rfm start` でゲーム開始！

## 基本コマンド

### 管理者コマンド (`/rfm`)
```
/rfm start              - ゲーム開始
/rfm stop               - ゲーム強制終了
/rfm pause/resume       - 一時停止/再開
/rfm status             - 現在状態表示
/rfm addhunter [数]     - ハンター追加
/rfm setbounty <金額>   - 賞金レート変更
/rfm mission complete <ID> - ミッション強制完了
/rfm ranking [type]     - ランキング表示
```

### プレイヤーコマンド
```
/surrender              - 自首（賞金確定）
```

## 設定

### config.yml (主要設定)
```yaml
game:
  duration: 600                 # ゲーム時間（秒）
  bounty-per-second: 100        # 賞金/秒
  max-bounty: 1000000           # 賞金上限

hunter:
  initial-count: 3              # 初期ハンター数
  view-distance: 30.0           # 視認距離
  field-of-view: 70.0           # 視野角
  chase-speed: 1.2              # 追跡速度
```

### ミッション設定 (missions/*.yml)
```yaml
id: "hunter_release"
name: "ハンター追加投入"
trigger:
  type: "TIME"
  value: "T+05:00"              # 開始から5分後
actions:
  - type: "add_hunter"
    count: 2                    # ハンター2体追加
```

## API

### プレースホルダー
```
%rfm_timeleft%          - 残り時間 (MM:SS)
%rfm_bounty%            - 現在賞金 (フォーマット済み)
%rfm_status%            - プレイヤー状態
%rfm_runners%           - 生存逃走者数
%rfm_hunters%           - ハンター数
```

### イベント API
```java
@EventHandler
public void onGameStart(GameStartEvent event) {
    // ゲーム開始時の処理
}

@EventHandler  
public void onPlayerCaught(PlayerCaughtEvent event) {
    // プレイヤー確保時の処理
}
```

## ビルド

```bash
git clone https://github.com/your-repo/RunForMoney.git
cd RunForMoney
./gradlew shadowJar
```

生成されるJARファイル: `build/libs/RunForMoney-*.jar`

## ライセンス

MIT License - 詳細は [LICENSE](LICENSE) ファイルを参照

## サポート

- **Issues**: [GitHub Issues](../../issues)
- **Wiki**: [プラグイン詳細ガイド](../../wiki)
- **Discord**: [サポートサーバー](https://discord.gg/your-server)

---

**「最後まで逃げ切れ！」** 🏃‍♂️💨