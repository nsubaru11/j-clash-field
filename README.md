# J-Clash Field
ネットワークシステム開発演習（Java演習） - 授業課題

ゲーム名: J-Clash Field

## 概要
サーバー権威型のオンラインアクションゲーム。通信層とゲームロジックを分離し、サーバー側で状態を決定します。

## ディレクトリ構成

- src/client: クライアント実装
- src/server: サーバー実装
- src/network: 通信層
- src/model: ゲームのドメインモデル
- resources: 画像などのリソース
- scripts: 起動用バッチ
- jars: クライアント/サーバーJAR出力先
- out: 生成物(自動生成; 手動編集しない)

## エントリポイント

- クライアント: `client.ClientMain` (`src/client/ClientMain.java`)
- サーバー: `server.ServerMain` (`src/server/ServerMain.java`)

## ビルド/実行 (Windows)

- サーバー起動: `scripts\start_server.bat`
- クライアント起動: `scripts\start_client.bat`
- 出力先: `out\production\j-clash-field`

## JAR実行

- サーバー: `java -jar jars\J-Clash-Field-server.jar`
- クライアント: `java -jar jars\J-Clash-Field-client.jar [host] [port]`

-----

# オンラインアクションゲームサーバー 基本設計書

## 1. アーキテクチャ概要

本システムは、リアルタイムアクションゲームのバックエンドとして、多人数同期、低遅延、および高い保守性を実現するために**「サーバー権威型（Server-Authoritative）」**を採用する。

通信層とゲームロジック層を明確に分離し、両者を**コマンドキュー（Command Queue）**で接続するプロデューサー・コンシューマーパターンを適用する。

フルスクリーン画面想定で、ゲーム画面は固定サイズで内部スクリーンの形で行う。

-----

## 2. クラス構成と責務

### 2.1 GameServer (管理者)

サーバーのエントリーポイント。接続の受付とゲームルームのライフサイクル管理を行う。

* **主な責務**:
    * **接続監視**: 指定ポートで `ServerSocket` を開き、新規接続を受け付ける。
    * **ルーム管理**: `GameRoom` の生成、保持、削除を行う。
    * **マッチメイキング**: 新規接続した `ClientHandler` を適切な `GameRoom` に割り振る。
    * **クリーンアップ**: ゲームが終了した、または全員切断された `GameRoom` を検知し、メモリから解放する。

### 2.2 GameRoom (ゲームエンジン / 調停者)

1つの対戦セッションを管理する「ゲームの心臓部」。独立したスレッドとして動作し、ゲームループを回す。

* **主な責務**:
    * **スレッド管理**: ゲームループ（Game Loop）を実行し、一定間隔（例: 60FPS）で時間を進める。
    * **コマンド処理**: 各 `ClientHandler` から送られてきたコマンドをキューから取り出し、順次処理する。
    * **ゲーム進行管理**: ゲーム開始、終了判定、制限時間の管理を行う。
    * **一斉配信**: 更新されたゲーム状態（全プレイヤーの座標など）を全 `ClientHandler` を通じてブロードキャストする。
    * **メンバー管理**: `Player` オブジェクトと `ClientHandler` の紐付け（Map）を管理する。

### 2.3 Player (ゲームデータ / ロジック)

ゲーム内に存在するプレイヤーの実体（アバター）。**通信機能は一切持たない**純粋なドメインモデル。

* **主な責務**:
    * **状態保持**: 座標 (x, y)、HP、プレイヤー名、向き、状態異常、使用キャラクターIDなどを保持する。
    * **フラグ管理**: `isReady`（準備完了）、`isConnected`（接続中）、`isDead`（死亡）などのステータス管理。
    * **更新ロジック**: `update(command)` メソッドを持ち、入力に応じて自身の座標計算やステータス変更を行う（物理演算含む）。

### 2.4 ClientHandler (通信アダプター / メッセンジャー)

クライアントとのTCPソケット通信を担当する。ゲームの内容については関知せず、データの運搬に徹する。

* **主な責務**:
    * **受信 (Producer)**: クライアントからの生データ（文字列やJSON）を受信し、`Command` オブジェクトに変換して `GameRoom`
      のキューに投入する。
    * **送信**: `GameRoom` から受け取ったゲーム状態データを、クライアントが理解できる形式にして送信する。
    * **接続維持**: 切断検知を行い、切断時は `GameRoom` へ通知する。

### 2.5 Command (データ構造体)

通信スレッドとゲームスレッドの間で受け渡されるメッセージの単位。

* **保有データ**:
    * `playerId`: 誰からの命令か
    * `type`: 命令の種類（例: MOVE, JUMP, ATTACK, READY, RESIGN）
    * `payload`: 詳細データ（例: 移動方向ベクトル、攻撃ボタンの種類）
    * `timestamp`: 受信時刻（ラグ補正などで使用する可能性があるため）

-----

## 3. 並行処理モデル（スレッド設計）

システムは以下の異なるスレッドグループによって駆動する。

1. **Main/Accept Thread (1つ)**
    * `GameServer` が稼働。新規接続のみを待機。
2. **Communication Threads (クライアント数分)**
    * `ClientHandler` が稼働。`readLine()` でのブロッキング待機が主。
    * ここでの処理は「受信→キュー投入」のみとし、極力軽量に保つ。
3. **Game Loop Threads (ルーム数分)**
    * `GameRoom` が稼働。
    * `Queue.poll()` → `Update` → `Sleep` のサイクルを厳密に守る。
    * `Player` オブジェクトへの書き込みはこのスレッドからのみ行う（スレッドセーフの担保）。

-----

## 4. データフロー・シーケンス

### 4.1 ゲーム中の入力処理（1フレームの流れ）

1. **Input (Client -\> Server)**
    * クライアントが「右移動」ボタンを押す。
    * `ClientHandler` が受信し、`Command(playerId, "MOVE_RIGHT")` を作成。
    * `GameRoom` の `ConcurrentLinkedQueue` に `Command` を `add` する。
2. **Process (Game Loop)**
    * `GameRoom` のループが回ってくる。
    * キューから `Command` を取り出す。
    * 該当する `Player` オブジェクトの `update("MOVE_RIGHT")` を呼ぶ。
    * `Player` の内部座標 `x` が加算される。
3. **Physics & Rules**
    * 全プレイヤーの移動後、`GameRoom` が当たり判定チェックを行う。
    * 場外落下などのルール判定を行う。
4. **Broadcast (Server -\> Client)**
    * `GameRoom` が全 `Player` の現在の情報をまとめた「WorldState」を作成。
    * 各 `ClientHandler` の `send(WorldState)` を呼び出し、全クライアントへ送信。

### 4.2 切断・再接続の処理

* **切断時**:
    * `ClientHandler` が `IOException` を検知。
    * `GameRoom` に切断を通知。
    * `GameRoom` は、該当 `Player` の `isConnected` フラグを `false` にする（**Playerオブジェクトは削除しない**）。
    * ゲーム内では「操作不能キャラ」として残る、あるいは一時退場扱いとする。
* **再接続時**:
    * クライアントが再接続し、名前（またはID）を送信。
    * `GameServer` -\> `GameRoom` が既存の `Player` リストを検索。
    * 同名の `Player` がいれば、新しい `ClientHandler` をその `Player` に紐付け直す。
    * `isConnected` を `true` に戻し、ゲーム復帰させる。

-----

## 5. まとめ：この設計のメリット

* **責務の分離**: 通信エラーがゲームロジックを壊すことがなく、逆にゲームのバグが通信を切断させることもない。
* **同期ずれの防止**: すべての計算が「GameRoomスレッド」という単一のタイムラインで行われるため、クライアントごとの処理速度差による矛盾が生じにくい。
* **拡張性**: 新しいアクションやアイテムを追加する場合、変更はほぼ `Player` クラス（と `Command` 定義）のみで完結し、通信部分は変更不要。
