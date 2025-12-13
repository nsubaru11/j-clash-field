package server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * ログの設定を行うクラスです。
 */
final class ServerLoggingConfig {
	private static boolean initialized;

	private ServerLoggingConfig() {
	}

	/**
	 * ログの設定を行います。
	 * コンソールとファイルの両方に出力を行います。
	 */
	static synchronized void initialize() {
		if (initialized) return;

		try {
			// ログの保存先の設定
			Path logDir = Paths.get("logs");
			Files.createDirectories(logDir);
			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
			Path logFile = logDir.resolve("server-" + timestamp + ".log");

			// ログに関するすべての設定の初期化
			Logger rootLogger = Logger.getLogger("");
			for (Handler handler : rootLogger.getHandlers()) {
				rootLogger.removeHandler(handler);
			}

			// ログのフォーマット（日時、クラス名、メソッド名、ログレベル、メッセージの順に整形）
			Formatter formatter = new SimpleFormatter();

			// ログの出力先
			ConsoleHandler consoleHandler = new ConsoleHandler();
			consoleHandler.setLevel(Level.INFO);
			consoleHandler.setFormatter(formatter);

			// ログの保存先
			FileHandler fileHandler = new FileHandler(logFile.toString(), true);
			fileHandler.setLevel(Level.FINE);
			fileHandler.setFormatter(formatter);

			// ログの設定（ファイル出力とコンソール出力どちらも行う）
			rootLogger.setLevel(Level.FINE);
			rootLogger.addHandler(consoleHandler);
			rootLogger.addHandler(fileHandler);

			// ログのシャットダウン時の処理
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				for (Handler handler : rootLogger.getHandlers()) {
					handler.flush();
					handler.close();
				}
			}));

			initialized = true;
		} catch (final IOException e) {
			throw new IllegalStateException("ログの初期化に失敗しました", e);
		}
	}
}
