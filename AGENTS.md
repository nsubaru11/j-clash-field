# AGENTS

## 目的

このリポジトリは、ネットワークアクションゲーム用のサーバー/クライアント(Java)を実装する授業課題です。
通信とゲームロジックの分離を前提に、サーバー権威型の構成で進めます。

## 主要ディレクトリ

- src/client: クライアント実装
- src/server: サーバー実装
- src/network: 通信層
- src/model: ゲームのドメインモデル
- resources: 画像などのリソース
- scripts: 起動用バッチ
- out: 生成物(自動生成; 手動編集しない)
- jars: クライアント/サーバーJAR出力先

## ビルド/実行 (Windows)

- サーバー起動: `scripts\start_server.bat`
- クライアント起動: `scripts\start_client.bat`

上記バッチは `src` をコンパイルし、`out\production\online-action-game-netprog` に出力します。

## エントリポイント

- クライアント: `client.ClientMain` (`src/client/ClientMain.java`)
- サーバー: `server.ServerMain` (`src/server/ServerMain.java`)

## コーディング方針

- 通信処理とゲームロジックは分離する。
- サーバーが権威を持つ(クライアントは入力送信のみ、状態決定はサーバー)。
- `src/model` は通信依存を持たせない。

## テスト

- start_server.bat, start_client.bat を実行して、クライアントとサーバーを起動する。
