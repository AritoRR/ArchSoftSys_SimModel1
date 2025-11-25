package ru.ananev.simmod.ui;

import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import ru.ananev.simmod.core.SimulationCore;
import ru.ananev.simmod.core.SimulationEvent;
import ru.ananev.simmod.core.StatisticsCollector;
import ru.ananev.simmod.model.Buffer;
import ru.ananev.simmod.model.Device;
import ru.ananev.simmod.model.Request;
import ru.ananev.simmod.model.Source;

import java.util.ArrayList;
import java.util.List;

public class Main extends Application {

    private SimulationCore simulationCore;
    private TextArea statusArea;
    private TextArea eventsArea;
    private Label currentTimeLabel;
    private Label statusLabel;
    private Label configLabel;
    private Button startButton;
    private Button stepButton;
    private Button autoButton;
    private Button pauseButton;
    private Button finishButton;
    private Button fastButton;
    private Button resetButton;
    private Button configButton;

    private Thread autoThread;
    private Thread fastThread;
    private volatile boolean autoRunning = false;
    private volatile boolean fastRunning = false;
    private volatile boolean paused = false;

    // Параметры конфигурации по умолчанию для сетевого маршрутизатора
    private int sourceCount = 2;
    private int bufferSize = 3;
    private int deviceCount = 2;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("🌐 Симулятор Сетевого Маршрутизатора");

        // Создаем начальную конфигурацию
        createInitialConfiguration();

        // Создаем интерфейс
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #f0f8ff;");

        // Панель управления
        HBox controlPanel = createControlPanel();

        // Основная информационная панель
        HBox infoPanel = createInfoPanel();

        // Область статусов
        VBox statusPanel = createStatusPanel();

        // Область будущих событий
        VBox eventsPanel = createEventsPanel();

        root.getChildren().addAll(controlPanel, infoPanel, statusPanel, eventsPanel);

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();

        updateUI();
    }

    private void createInitialConfiguration() {
        List<Source> sources = createSources(sourceCount);
        Buffer buffer = new Buffer(1, bufferSize);
        List<Device> devices = createDevices(deviceCount);
        simulationCore = new SimulationCore(sources, buffer, devices);
    }

    private HBox createControlPanel() {
        HBox controlPanel = new HBox(10);
        controlPanel.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("🌐 Симулятор Сетевого Маршрутизатора");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0066cc;");

        configButton = new Button("⚙ Конфигурация");
        startButton = new Button("▶ Старт");
        stepButton = new Button("👣 Шаг");
        autoButton = new Button("🔁 Авто");
        pauseButton = new Button("⏸ Пауза");
        finishButton = new Button("⏹ Завершить");
        fastButton = new Button("⚡ Быстро");
        resetButton = new Button("🔄 Сброс");

        // Начальное состояние кнопок
        stepButton.setDisable(true);
        autoButton.setDisable(true);
        pauseButton.setDisable(true);
        finishButton.setDisable(true);
        fastButton.setDisable(true);

        configButton.setOnAction(e -> showConfigurationDialog());
        startButton.setOnAction(e -> startSimulation());
        stepButton.setOnAction(e -> performStep());
        autoButton.setOnAction(e -> startAutoMode());
        pauseButton.setOnAction(e -> togglePause());
        finishButton.setOnAction(e -> finishSimulation());
        fastButton.setOnAction(e -> startFastMode());
        resetButton.setOnAction(e -> resetSimulation());

        statusLabel = new Label("Статус: Система сконфигурирована");
        statusLabel.setStyle("-fx-text-fill: gray; -fx-font-weight: bold;");

        controlPanel.getChildren().addAll(
                titleLabel, configButton, startButton, stepButton,
                autoButton, pauseButton, finishButton, fastButton, resetButton,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                statusLabel
        );

        return controlPanel;
    }

    private HBox createInfoPanel() {
        HBox infoPanel = new HBox(10);
        infoPanel.setAlignment(Pos.CENTER_LEFT);

        currentTimeLabel = new Label("🕐 Текущее время: 0.000 мс");
        currentTimeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // Добавляем информацию о конфигурации
        configLabel = new Label();
        configLabel.setStyle("-fx-font-size: 12px;");
        updateConfigLabel();

        infoPanel.getChildren().addAll(
                currentTimeLabel,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                configLabel
        );
        return infoPanel;
    }

    private void updateConfigLabel() {
        String configText = String.format("📡 Источники=%d | 🗂Буфер=%d | 💻 Процессоры=%d",
                sourceCount, bufferSize, deviceCount);
        configLabel.setText(configText);
    }

    private VBox createStatusPanel() {
        VBox statusPanel = new VBox(5);
        statusPanel.setStyle("-fx-border-color: #3498db; -fx-border-width: 2; -fx-padding: 10; -fx-background-color: #ecf0f1;");

        Label titleLabel = new Label("=== 🌐 СОСТОЯНИЕ СЕТЕВОГО МАРШРУТИЗАТОРА ===");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");

        statusArea = new TextArea();
        statusArea.setPrefHeight(400);
        statusArea.setEditable(false);
        statusArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px;");

        statusPanel.getChildren().addAll(titleLabel, statusArea);
        return statusPanel;
    }

    private VBox createEventsPanel() {
        VBox eventsPanel = new VBox(5);
        eventsPanel.setStyle("-fx-border-color: #e67e22; -fx-border-width: 2; -fx-padding: 10; -fx-background-color: #fef9e7;");

        Label titleLabel = new Label("=== 📅 БУДУЩИЕ СОБЫТИЯ ===");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #d35400;");

        eventsArea = new TextArea();
        eventsArea.setPrefHeight(200);
        eventsArea.setEditable(false);
        eventsArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px;");

        eventsPanel.getChildren().addAll(titleLabel, eventsArea);
        return eventsPanel;
    }

    private void showConfigurationDialog() {
        Stage configStage = new Stage();
        configStage.setTitle("⚙️ Конфигурация сетевого маршрутизатора");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 20, 20));

        // Поля ввода
        Spinner<Integer> sourceSpinner = new Spinner<>(1, 10, sourceCount);
        sourceSpinner.setEditable(true);

        Spinner<Integer> bufferSpinner = new Spinner<>(1, 20, bufferSize);
        bufferSpinner.setEditable(true);

        Spinner<Integer> deviceSpinner = new Spinner<>(1, 8, deviceCount);
        deviceSpinner.setEditable(true);

        grid.add(new Label("📡 Количество источников трафика:"), 0, 0);
        grid.add(sourceSpinner, 1, 0);
        grid.add(new Label("🗂 Размер буфера пакетов:"), 0, 1);
        grid.add(bufferSpinner, 1, 1);
        grid.add(new Label("💻 Количество процессоров:"), 0, 2);
        grid.add(deviceSpinner, 1, 2);

        Button applyButton = new Button("✅ Применить");
        Button cancelButton = new Button("❌ Отмена");

        applyButton.setOnAction(e -> {
            sourceCount = sourceSpinner.getValue();
            bufferSize = bufferSpinner.getValue();
            deviceCount = deviceSpinner.getValue();

            applyNewConfiguration();
            configStage.close();
        });

        cancelButton.setOnAction(e -> configStage.close());

        HBox buttonBox = new HBox(10, applyButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        grid.add(buttonBox, 0, 3, 2, 1);

        Scene scene = new Scene(grid);
        configStage.setScene(scene);
        configStage.showAndWait();
    }

    private void applyNewConfiguration() {
        // Создаем новую конфигурацию
        List<Source> sources = createSources(sourceCount);
        Buffer buffer = new Buffer(1, bufferSize);
        List<Device> devices = createDevices(deviceCount);

        simulationCore = new SimulationCore(sources, buffer, devices);

        updateConfigLabel();
        updateUI();

        // Сбрасываем состояние кнопок
        resetButtonsToInitialState();

        statusLabel.setText("Статус: Система переконфигурирована");
        statusLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
    }

    private void resetButtonsToInitialState() {
        startButton.setDisable(false);
        stepButton.setDisable(true);
        autoButton.setDisable(true);
        pauseButton.setDisable(true);
        finishButton.setDisable(true);
        fastButton.setDisable(true);
        configButton.setDisable(false);
    }

    private List<Source> createSources(int count) {
        List<Source> sources = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            sources.add(new Source(i, 1.0, "📡 Источник " + i));
        }
        return sources;
    }

    private List<Device> createDevices(int count) {
        List<Device> devices = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            devices.add(new Device(i, "💻 Процессор " + i));
        }
        return devices;
    }

    private void startSimulation() {
        simulationCore.startSimulation();
        startButton.setDisable(true);
        stepButton.setDisable(false);
        autoButton.setDisable(false);
        finishButton.setDisable(false);
        fastButton.setDisable(false);
        statusLabel.setText("Статус: Симуляция запущена");
        statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        updateUI();
    }

    private void performStep() {
        if (!simulationCore.isSimulationStarted()) {
            return;
        }

        boolean stepResult = simulationCore.step();
        updateUI();

        if (!stepResult) {
            simulationComplete();
        } else {
            statusLabel.setText("Статус: Выполнен шаг");
            statusLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
        }
    }

    private void startAutoMode() {
        if (!simulationCore.isSimulationStarted()) {
            return;
        }

        autoRunning = true;
        paused = false;
        stepButton.setDisable(true);
        autoButton.setDisable(true);
        pauseButton.setDisable(false);
        finishButton.setDisable(false);
        fastButton.setDisable(true);
        configButton.setDisable(true);
        statusLabel.setText("Статус: Автоматический режим");
        statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

        autoThread = new Thread(() -> {
            while (autoRunning && simulationCore.isRunning() && simulationCore.getCurrentTime() < 100.0) {
                if (!paused) {
                    stepButton.setDisable(true);
                    boolean stepResult = simulationCore.step();
                    Platform.runLater(this::updateUI);

                    if (!stepResult) {
                        break;
                    }

                    try {
                        Thread.sleep(500); // Задержка 1 секунда между шагами
                    } catch (InterruptedException e) {
                        break;
                    }
                } else {
                    try {
                        stepButton.setDisable(false);
                        Thread.sleep(100);
                        // Короткая пауза для проверки состояния
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }

            Platform.runLater(() -> {
                if (autoRunning) {
                    simulationComplete();
                }
            });
        });

        autoThread.setDaemon(true);
        autoThread.start();
    }

    private void togglePause() {
        paused = !paused;
        if (paused) {
            pauseButton.setText("▶ Продолжить");
            statusLabel.setText("Статус: На паузе");
            statusLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
        } else {
            pauseButton.setText("⏸ Пауза");
            statusLabel.setText("Статус: Автоматический режим");
            statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        }
    }

    private void finishSimulation() {
        // Останавливаем все потоки
        autoRunning = false;
        fastRunning = false;
        paused = false;

        if (autoThread != null && autoThread.isAlive()) {
            autoThread.interrupt();
        }
        if (fastThread != null && fastThread.isAlive()) {
            fastThread.interrupt();
        }

        // Завершаем симуляцию и показываем статистику
        showStatistics();
        resetButtonsToInitialState();
        statusLabel.setText("Статус: Симуляция завершена");
        statusLabel.setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
    }

    private void startFastMode() {
        if (!simulationCore.isSimulationStarted()) {
            return;
        }

        fastRunning = true;
        stepButton.setDisable(true);
        autoButton.setDisable(true);
        pauseButton.setDisable(true);
        finishButton.setDisable(false);
        fastButton.setDisable(true);
        configButton.setDisable(true);
        statusLabel.setText("Статус: Быстрый режим");
        statusLabel.setStyle("-fx-text-fill: purple; -fx-font-weight: bold;");

        fastThread = new Thread(() -> {
            simulationCore.runAutomatic(100.0);

            Platform.runLater(() -> {
                fastRunning = false;
                simulationComplete();
            });
        });

        fastThread.setDaemon(true);
        fastThread.start();
    }

    private void resetSimulation() {
        // Останавливаем все потоки
        autoRunning = false;
        fastRunning = false;
        paused = false;

        if (autoThread != null && autoThread.isAlive()) {
            autoThread.interrupt();
        }
        if (fastThread != null && fastThread.isAlive()) {
            fastThread.interrupt();
        }

        // Пересоздаем симуляцию с текущей конфигурацией
        applyNewConfiguration();

        updateUI();
        statusLabel.setText("Статус: Система сброшена");
        statusLabel.setStyle("-fx-text-fill: gray; -fx-font-weight: bold;");
    }

    private void simulationComplete() {
        autoRunning = false;
        fastRunning = false;
        paused = false;

        stepButton.setDisable(true);
        autoButton.setDisable(true);
        pauseButton.setDisable(true);
        finishButton.setDisable(true);
        fastButton.setDisable(true);
        configButton.setDisable(false);

        updateUI();
        showStatistics();
        statusLabel.setText("Статус: Симуляция завершена");
        statusLabel.setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
    }

    private void updateUI() {
        currentTimeLabel.setText(String.format("🕐 Текущее время: %.3f мс", simulationCore.getCurrentTime()));
        updateStatusArea();
        updateEventsArea();
    }

    private void updateStatusArea() {
        StringBuilder statusText = new StringBuilder();

        statusText.append("=== 📡 ИСТОЧНИКИ ТРАФИКА ===\n");
        for (Source source : simulationCore.getSources()) {
            String sourceName = source.getName() != null ? source.getName() : "Источник " + source.getId();
            statusText.append(String.format("%s: след.пакет=%.3f, отправлено=%d\n",
                    sourceName, source.getNextEventTime(), source.getRequestsCount()));
        }

        statusText.append("\n=== 💻 ПРОЦЕССОРЫ МАРШРУТИЗАЦИИ ===\n");
        for (Device device : simulationCore.getDevices()) {
            String processorName = device.getName() != null ? device.getName() : "Процессор " + device.getId();
            String status = device.isBusy() ? "🔴 Обрабатывает" : "🟢 Свободен";
            String packetInfo = device.isBusy() && device.getCurrentRequest() != null ?
                    String.format(" пакет %s", device.getCurrentRequest().getPacketInfo()) : "";
            statusText.append(String.format("%s: %s%s\n",
                    processorName, status, packetInfo));
        }

        statusText.append("\n=== 🗂 БУФЕР ПАКЕТОВ ===\n");
        Buffer buffer = simulationCore.getBuffer();
        Request[] requests = buffer.getRequests();
        if (requests != null) {
            for (int i = 0; i < requests.length; i++) {
                String posStatus = requests[i] != null ?
                        String.format("%s (поступление: %.3f)", requests[i].getPacketInfo(), requests[i].getBufferArrivalTime()) : "🆓 Свободно";
                String pointer = (i == buffer.getPointer()) ? " <-- след.позиция" : "";
                statusText.append(String.format("Слот %d: %s%s\n", i + 1, posStatus, pointer));
            }
        }

        StatisticsCollector stats = simulationCore.getStatistics();
        statusText.append(String.format("\n=== 📊 СТАТИСТИКА МАРШРУТИЗАТОРА ===\n"));
        statusText.append(String.format("Всего пакетов: %d\n", stats.getTotalRequests()));
        statusText.append(String.format("Успешно маршрутизировано: %d\n", stats.getCompletedRequests()));
        statusText.append(String.format("Потеряно пакетов: %d\n", stats.getRejectedRequests()));
        statusText.append(String.format("Процент потерь: %.1f%%\n", stats.getRejectionProbability() * 100));
        statusText.append(String.format("Среднее время обработки: %.3f мс\n", stats.getAverageServiceTime()));
        statusText.append(String.format("Среднее время ожидания: %.3f мс\n", stats.getAverageWaitTime()));

        statusArea.setText(statusText.toString());
    }

    private void updateEventsArea() {
        StringBuilder eventsText = new StringBuilder();

        List<SimulationEvent> futureEvents = simulationCore.getFutureEvents();
        if (futureEvents.isEmpty()) {
            eventsText.append("Нет запланированных событий\n");
        } else {
            for (SimulationEvent event : futureEvents) {
                switch (event.getType()) {
                    case REQUEST_GENERATED:
                        eventsText.append(String.format("[%.3f] 📡 Поступление пакета от источника %d\n",
                                event.getTime(), event.getSourceId()));
                        break;
                    case SERVICE_COMPLETED:
                        if (event.getRequest() != null) {
                            eventsText.append(String.format("[%.3f] ✅ Обработан пакет %s процессором %d\n",
                                    event.getTime(), event.getRequest().getPacketInfo(), event.getRequest().getDeviceId()));
                        }
                        break;
                }
            }
        }

        eventsArea.setText(eventsText.toString());
    }

    private void showStatistics() {
        StatisticsCollector stats = simulationCore.getStatistics();

        // Сначала сохраняем статистику в файл
        saveStatisticsToFile(stats);

        // Затем показываем в окне (как раньше)
        Stage statsStage = new Stage();
        statsStage.setTitle("📊 Статистика сетевого маршрутизатора");

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        Label titleLabel = new Label("📊 СТАТИСТИКА СЕТЕВОГО МАРШРУТИЗАТОРА");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TextArea statsArea = new TextArea();
        statsArea.setEditable(false);
        statsArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px;");
        statsArea.setPrefSize(600, 500);

        String statsText = generateStatisticsText(stats);
        statsArea.setText(statsText);

        // Добавляем кнопку для открытия папки с файлом
        Button openFolderButton = new Button("📁 Открыть папку с файлом");
        openFolderButton.setOnAction(e -> openStatisticsFolder());

        Button closeButton = new Button("Закрыть");
        closeButton.setOnAction(e -> statsStage.close());

        HBox buttonBox = new HBox(10, openFolderButton, closeButton);
        buttonBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(titleLabel, statsArea, buttonBox);
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root);
        statsStage.setScene(scene);
        statsStage.show();
    }

    private String generateStatisticsText(StatisticsCollector stats) {
        StringBuilder statsText = new StringBuilder();
        statsText.append("=== 📈 ОСНОВНЫЕ ПОКАЗАТЕЛИ ===\n\n");

        // 1. Вероятность отказа в обслуживании заявок
        double rejectionProbability = stats.getRejectionProbability();
        statsText.append(String.format("1. Вероятность отказа в обслуживании: %.4f (%.2f%%)\n",
                rejectionProbability, rejectionProbability * 100));

        // 2. Среднее время пребывания заявок в СМО
        double avgTimeInSystem = stats.getAverageServiceTime() + stats.getAverageWaitTime();
        statsText.append(String.format("2. Среднее время пребывания в СМО: %.3f мс\n", avgTimeInSystem));

        // 3. Коэффициент использования приборов
        double deviceUtilization = calculateDeviceUtilization(stats);
        statsText.append(String.format("3. Коэффициент использования приборов: %.4f (%.2f%%)\n\n",
                deviceUtilization, deviceUtilization * 100));

        statsText.append("=== 📊 ДЕТАЛЬНАЯ СТАТИСТИКА ===\n\n");
        statsText.append(String.format("Всего принято пакетов: %d\n", stats.getTotalRequests()));
        statsText.append(String.format("Успешно маршрутизировано: %d\n", stats.getCompletedRequests()));
        statsText.append(String.format("Потеряно пакетов: %d\n", stats.getRejectedRequests()));
        statsText.append(String.format("Среднее время обработки: %.3f мс\n", stats.getAverageServiceTime()));
        statsText.append(String.format("Среднее время ожидания в буфере: %.3f мс\n\n", stats.getAverageWaitTime()));

        statsText.append("=== 📡 АКТИВНОСТЬ ИСТОЧНИКОВ ===\n");
        stats.getSourceRequests().forEach((sourceId, count) -> {
            String sourceName = getSourceName(sourceId);
            statsText.append(String.format("%s: %d пакетов\n", sourceName, count));
        });

        statsText.append("\n=== 💻 ЗАГРУЗКА ПРОЦЕССОРОВ ===\n");
        stats.getDeviceProcessed().forEach((deviceId, count) -> {
            String processorName = getProcessorName(deviceId);
            double processorUtilization = calculateProcessorUtilization(deviceId, stats);
            statsText.append(String.format("%s: %d пакетов (загрузка: %.1f%%)\n",
                    processorName, count, processorUtilization * 100));
        });

        // Дополнительная статистика
        statsText.append("\n=== 🌐 ЭФФЕКТИВНОСТЬ СИСТЕМЫ ===\n");
        statsText.append(String.format("Общее время симуляции: %.3f мс\n", simulationCore.getCurrentTime()));

        double efficiency = stats.getTotalRequests() > 0 ?
                (double) stats.getCompletedRequests() / stats.getTotalRequests() * 100 : 0;
        statsText.append(String.format("Эффективность маршрутизации: %.1f%%\n", efficiency));

        double avgThroughput = simulationCore.getCurrentTime() > 0 ?
                stats.getCompletedRequests() / simulationCore.getCurrentTime() * 1000 : 0;
        statsText.append(String.format("Пропускная способность: %.1f пак/сек\n", avgThroughput));

        return statsText.toString();
    }

    private void saveStatisticsToFile(StatisticsCollector stats) {
        try {
            // Создаем имя файла с временной меткой
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String fileName = "router_statistics_" + timestamp + ".txt";

            // Создаем папку statistics если её нет
            java.nio.file.Path statsDir = java.nio.file.Paths.get("statistics");
            if (!java.nio.file.Files.exists(statsDir)) {
                java.nio.file.Files.createDirectories(statsDir);
            }

            java.nio.file.Path filePath = statsDir.resolve(fileName);

            // Формируем содержимое для файла (более структурированное)
            String fileContent = generateFileStatistics(stats);

            // Записываем в файл
            java.nio.file.Files.write(filePath, fileContent.getBytes());

            // Показываем сообщение об успешном сохранении
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Статистика сохранена");
                alert.setHeaderText("Статистика успешно сохранена в файл");
                alert.setContentText("Файл: " + filePath.toAbsolutePath());
                alert.showAndWait();
            });

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка сохранения");
                alert.setHeaderText("Не удалось сохранить статистику в файл");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            });
        }
    }

    private String generateFileStatistics(StatisticsCollector stats) {
        StringBuilder sb = new StringBuilder();

        // Заголовок
        sb.append("СТАТИСТИКА СЕТЕВОГО МАРШРУТИЗАТОРА\n");
        sb.append("===================================\n");
        sb.append("Время генерации: ").append(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        sb.append("Конфигурация системы: Источники=").append(sourceCount).append(", Буфер=").append(bufferSize).append(", Процессоры=").append(deviceCount).append("\n\n");

        // Основные показатели
        sb.append("ОСНОВНЫЕ ПОКАЗАТЕЛИ:\n");
        sb.append("--------------------\n");

        double rejectionProbability = stats.getRejectionProbability();
        sb.append(String.format("Вероятность отказа в обслуживании: %.4f (%.2f%%)\n", rejectionProbability, rejectionProbability * 100));

        double avgTimeInSystem = stats.getAverageServiceTime() + stats.getAverageWaitTime();
        sb.append(String.format("Среднее время пребывания в СМО: %.3f мс\n", avgTimeInSystem));

        double deviceUtilization = calculateDeviceUtilization(stats);
        sb.append(String.format("Коэффициент использования приборов: %.4f (%.2f%%)\n\n", deviceUtilization, deviceUtilization * 100));

        // Общая статистика
        sb.append("ОБЩАЯ СТАТИСТИКА:\n");
        sb.append("-----------------\n");
        sb.append(String.format("Всего принято пакетов: %d\n", stats.getTotalRequests()));
        sb.append(String.format("Успешно маршрутизировано: %d\n", stats.getCompletedRequests()));
        sb.append(String.format("Потеряно пакетов: %d\n", stats.getRejectedRequests()));
        sb.append(String.format("Среднее время обработки: %.3f мс\n", stats.getAverageServiceTime()));
        sb.append(String.format("Среднее время ожидания в буфере: %.3f мс\n", stats.getAverageWaitTime()));
        sb.append(String.format("Общее время симуляции: %.3f мс\n\n", simulationCore.getCurrentTime()));

        // Статистика по источникам
        sb.append("СТАТИСТИКА ПО ИСТОЧНИКАМ:\n");
        sb.append("--------------------------\n");
        stats.getSourceRequests().forEach((sourceId, count) -> {
            String sourceName = getSourceName(sourceId);
            double sourcePercentage = stats.getTotalRequests() > 0 ? (double) count / stats.getTotalRequests() * 100 : 0;
            sb.append(String.format("%s: %d пакетов (%.1f%%)\n", sourceName, count, sourcePercentage));
        });
        sb.append("\n");

        // Статистика по процессорам
        sb.append("СТАТИСТИКА ПО ПРОЦЕССОРАМ:\n");
        sb.append("---------------------------\n");
        stats.getDeviceProcessed().forEach((deviceId, count) -> {
            String processorName = getProcessorName(deviceId);
            double processorUtilization = calculateProcessorUtilization(deviceId, stats);
            double processorPercentage = stats.getCompletedRequests() > 0 ? (double) count / stats.getCompletedRequests() * 100 : 0;
            sb.append(String.format("%s: %d пакетов (%.1f%% от общего числа), загрузка: %.1f%%\n",
                    processorName, count, processorPercentage, processorUtilization * 100));
        });
        sb.append("\n");

        // Эффективность системы
        sb.append("ЭФФЕКТИВНОСТЬ СИСТЕМЫ:\n");
        sb.append("----------------------\n");
        double efficiency = stats.getTotalRequests() > 0 ?
                (double) stats.getCompletedRequests() / stats.getTotalRequests() * 100 : 0;
        sb.append(String.format("Эффективность маршрутизации: %.1f%%\n", efficiency));

        double avgThroughput = simulationCore.getCurrentTime() > 0 ?
                stats.getCompletedRequests() / simulationCore.getCurrentTime() * 1000 : 0;
        sb.append(String.format("Пропускная способность: %.1f пакетов/сек\n", avgThroughput));

        return sb.toString();
    }

    private void openStatisticsFolder() {
        try {
            java.nio.file.Path statsDir = java.nio.file.Paths.get("statistics");
            if (!java.nio.file.Files.exists(statsDir)) {
                java.nio.file.Files.createDirectories(statsDir);
            }

            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            desktop.open(statsDir.toFile());
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText("Не удалось открыть папку со статистикой");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private double calculateDeviceUtilization(StatisticsCollector stats) {
        if (simulationCore.getCurrentTime() == 0) return 0;

        // Общее время работы всех приборов
        double totalDeviceTime = 0;
        for (Device device : simulationCore.getDevices()) {
            // Простая оценка - считаем что прибор работал пропорционально количеству обработанных пакетов
            int processedPackets = stats.getDeviceProcessed().getOrDefault(device.getId(), 0);
            totalDeviceTime += processedPackets * stats.getAverageServiceTime();
        }

        // Максимально возможное время работы (все приборы работали все время)
        double maxPossibleTime = simulationCore.getDevices().size() * simulationCore.getCurrentTime();

        return maxPossibleTime > 0 ? totalDeviceTime / maxPossibleTime : 0;
    }

    private double calculateProcessorUtilization(int deviceId, StatisticsCollector stats) {
        if (simulationCore.getCurrentTime() == 0) return 0;

        int processedPackets = stats.getDeviceProcessed().getOrDefault(deviceId, 0);
        double estimatedWorkTime = processedPackets * stats.getAverageServiceTime();

        return estimatedWorkTime / simulationCore.getCurrentTime();
    }

    private String getSourceName(int sourceId) {
        return "📡 Источник " + sourceId;
    }

    private String getProcessorName(int deviceId) {
        return "💻 Процессор " + deviceId;
    }

    public static void main(String[] args) {
        launch(args);
    }
}