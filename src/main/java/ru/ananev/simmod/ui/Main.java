package ru.ananev.simmod.ui;

import javafx.application.Application;
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
    private Button resetButton;
    private Button configButton;

    private Thread autoThread;
    private volatile boolean autoRunning = false;

    // Параметры конфигурации по умолчанию для системы доставки еды
    private int sourceCount = 2;
    private int bufferSize = 3;
    private int deviceCount = 2;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("🍕 Симулятор Системы Доставки Еды");

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

        Label titleLabel = new Label("🍕 Симулятор Доставки Еды");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #d35400;");

        configButton = new Button("⚙️ Конфигурация");
        startButton = new Button("▶️ Старт");
        stepButton = new Button("👣 Шаг");
        autoButton = new Button("⚡ Авто");
        resetButton = new Button("🔄 Сброс");

        configButton.setOnAction(e -> showConfigurationDialog());
        startButton.setOnAction(e -> startSimulation());
        stepButton.setOnAction(e -> performStep());
        autoButton.setOnAction(e -> startAutoMode());
        resetButton.setOnAction(e -> resetSimulation());

        // Изначально только кнопки Конфигурация и Старт активны
        stepButton.setDisable(true);
        autoButton.setDisable(true);

        statusLabel = new Label("Статус: Система сконфигурирована");
        statusLabel.setStyle("-fx-text-fill: gray; -fx-font-weight: bold;");

        controlPanel.getChildren().addAll(
                titleLabel, configButton, startButton, stepButton, autoButton, resetButton,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                statusLabel
        );

        return controlPanel;
    }

    private HBox createInfoPanel() {
        HBox infoPanel = new HBox(10);
        infoPanel.setAlignment(Pos.CENTER_LEFT);

        currentTimeLabel = new Label("🕐 Текущее время: 0.000");
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
        String configText = String.format("🍳 Кухни=%d | 📦 Стол сборки=%d | 🚗 Курьеры=%d",
                sourceCount, bufferSize, deviceCount);
        configLabel.setText(configText);
    }

    private VBox createStatusPanel() {
        VBox statusPanel = new VBox(5);
        statusPanel.setStyle("-fx-border-color: #3498db; -fx-border-width: 2; -fx-padding: 10; -fx-background-color: #ecf0f1;");

        Label titleLabel = new Label("=== 🏪 СОСТОЯНИЕ СИСТЕМЫ ДОСТАВКИ ===");
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
        configStage.setTitle("⚙️ Конфигурация системы доставки");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 20, 20));

        // Поля ввода
        Spinner<Integer> sourceSpinner = new Spinner<>(1, 5, sourceCount);
        sourceSpinner.setEditable(true);

        Spinner<Integer> bufferSpinner = new Spinner<>(1, 10, bufferSize);
        bufferSpinner.setEditable(true);

        Spinner<Integer> deviceSpinner = new Spinner<>(1, 5, deviceCount);
        deviceSpinner.setEditable(true);

        grid.add(new Label("🍳 Количество кухонь:"), 0, 0);
        grid.add(sourceSpinner, 1, 0);
        grid.add(new Label("📦 Размер стола сборки:"), 0, 1);
        grid.add(bufferSpinner, 1, 1);
        grid.add(new Label("🚗 Количество курьеров:"), 0, 2);
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
        startButton.setDisable(false);
        stepButton.setDisable(true);
        autoButton.setDisable(true);

        statusLabel.setText("Статус: Система переконфигурирована");
        statusLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
    }

    private List<Source> createSources(int count) {
        List<Source> sources = new ArrayList<>();
        for (int i = 0; i <= count; i++) {
            sources.add(new Source(i, 1.5, "🍽️ Кухня " + i));
        }
        return sources;
    }

    private List<Device> createDevices(int count) {
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i <= count; i++) {
            devices.add(new Device(i, "🚐 Курьер " + i));
        }
        return devices;
    }

    private void startSimulation() {
        simulationCore.startSimulation();
        startButton.setDisable(true);
        stepButton.setDisable(false);
        autoButton.setDisable(false);
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
            // Симуляция завершена
            stepButton.setDisable(true);
            autoButton.setDisable(true);
            statusLabel.setText("Статус: Симуляция завершена");
            statusLabel.setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
            showStatistics();
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
        stepButton.setDisable(true);
        autoButton.setDisable(true);
        startButton.setDisable(true);
        configButton.setDisable(true);
        statusLabel.setText("Статус: Автоматический режим");
        statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

        autoThread = new Thread(() -> {
            simulationCore.runAutomatic(100.0);

            Platform.runLater(() -> {
                autoRunning = false;
                stepButton.setDisable(true);
                autoButton.setDisable(true);
                configButton.setDisable(false);
                updateUI();
                showStatistics();
                statusLabel.setText("Статус: Симуляция завершена");
                statusLabel.setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
            });
        });

        autoThread.setDaemon(true);
        autoThread.start();

        // Обновление UI в автоматическом режиме
        startUIUpdateThread();
    }

    private void startUIUpdateThread() {
        Thread uiThread = new Thread(() -> {
            while (autoRunning && simulationCore.isRunning()) {
                try {
                    Thread.sleep(100);
                    Platform.runLater(this::updateUI);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        uiThread.setDaemon(true);
        uiThread.start();
    }

    private void resetSimulation() {
        autoRunning = false;
        if (autoThread != null && autoThread.isAlive()) {
            autoThread.interrupt();
        }

        // Пересоздаем симуляцию с текущей конфигурацией
        applyNewConfiguration();

        startButton.setDisable(false);
        stepButton.setDisable(true);
        autoButton.setDisable(true);
        configButton.setDisable(false);
        updateUI();
        statusLabel.setText("Статус: Система сброшена");
        statusLabel.setStyle("-fx-text-fill: gray; -fx-font-weight: bold;");
    }

    private void updateUI() {
        currentTimeLabel.setText(String.format("🕐 Текущее время: %.3f", simulationCore.getCurrentTime()));
        updateStatusArea();
        updateEventsArea();
    }

    private void updateStatusArea() {
        StringBuilder statusText = new StringBuilder();

        statusText.append("=== 🍳 КУХНИ ===\n");
        for (Source source : simulationCore.getSources()) {
            String kitchenName = source.getName() != null ? source.getName() : "Кухня " + source.getId();
            statusText.append(String.format("%s: след.заказ=%.3f, приготовлено=%d\n",
                    kitchenName, source.getNextEventTime(), source.getRequestsCount()));
        }

        statusText.append("\n=== 🚗 КУРЬЕРЫ ===\n");
        for (Device device : simulationCore.getDevices()) {
            String courierName = device.getName() != null ? device.getName() : "Курьер " + device.getId();
            String status = device.isBusy() ? "🚚 В пути" : "🟢 Свободен";
            String deliveryInfo = device.isBusy() && device.getCurrentRequest() != null ?
                    String.format(" доставляет %s", device.getCurrentRequest().getFoodItem()) : "";
            statusText.append(String.format("%s: %s%s\n",
                    courierName, status, deliveryInfo));
        }

        statusText.append("\n=== 📦 СТОЛ СБОРКИ ===\n");
        Buffer buffer = simulationCore.getBuffer();
        Request[] requests = buffer.getRequests();
        if (requests != null) {
            for (int i = 0; i < requests.length; i++) {
                String posStatus = requests[i] != null ?
                        String.format("%s (%.3f)", requests[i].getFoodItem(), requests[i].getBufferArrivalTime()) : "🆓 Свободно";
                String pointer = (i == buffer.getPointer()) ? " <-- след.позиция" : "";
                statusText.append(String.format("Позиция %d: %s%s\n", i + 1, posStatus, pointer));
            }
        }

        StatisticsCollector stats = simulationCore.getStatistics();
        statusText.append(String.format("\n=== 📊 СТАТИСТИКА ===\n"));
        statusText.append(String.format("Всего заказов: %d\n", stats.getTotalRequests()));
        statusText.append(String.format("Доставлено: %d\n", stats.getCompletedRequests()));
        statusText.append(String.format("Испорченных заказов: %d\n", stats.getRejectedRequests()));
        statusText.append(String.format("Процент испорченных: %.1f%%\n", stats.getRejectionProbability() * 100));
        statusText.append(String.format("Среднее время доставки: %.3f\n", stats.getAverageServiceTime()));
        statusText.append(String.format("Среднее время ожидания: %.3f\n", stats.getAverageWaitTime()));

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
                        eventsText.append(String.format("[%.3f] 🍽️ Готовится заказ на %s\n",
                                event.getTime(), event.getSourceId()));
                        break;
                    case SERVICE_COMPLETED:
                        if (event.getRequest() != null) {
                            eventsText.append(String.format("[%.3f] ✅ Доставлен %s курьером %s\n",
                                    event.getTime(), event.getRequest().getFoodItem(), event.getRequest().getDeviceId()));
                        }
                        break;
                }
            }
        }

        eventsArea.setText(eventsText.toString());
    }

    private void showStatistics() {
        StatisticsCollector stats = simulationCore.getStatistics();

        Stage statsStage = new Stage();
        statsStage.setTitle("📊 Статистика доставки");

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        Label titleLabel = new Label("📊 СТАТИСТИКА СИСТЕМЫ ДОСТАВКИ");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TextArea statsArea = new TextArea();
        statsArea.setEditable(false);
        statsArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px;");
        statsArea.setPrefSize(600, 500);

        StringBuilder statsText = new StringBuilder();
        statsText.append("=== 📈 ОБЩАЯ СТАТИСТИКА ===\n\n");
        statsText.append(String.format("Всего принято заказов: %d\n", stats.getTotalRequests()));
        statsText.append(String.format("Успешно доставлено: %d\n", stats.getCompletedRequests()));
        statsText.append(String.format("Испорчено (потеряно): %d\n", stats.getRejectedRequests()));
        statsText.append(String.format("Процент испорченных: %.1f%%\n\n", stats.getRejectionProbability() * 100));

        statsText.append(String.format("Среднее время доставки: %.3f мин\n", stats.getAverageServiceTime()));
        statsText.append(String.format("Среднее время ожидания курьера: %.3f мин\n\n", stats.getAverageWaitTime()));

        statsText.append("=== 🍳 ЭФФЕКТИВНОСТЬ КУХОНЬ ===\n");
        stats.getSourceRequests().forEach((sourceId, count) -> {
            String kitchenName = getKitchenName(sourceId);
            statsText.append(String.format("%s: %d заказов\n", kitchenName, count));
        });

        statsText.append("\n=== 🚗 ЗАГРУЗКА КУРЬЕРОВ ===\n");
        stats.getDeviceProcessed().forEach((deviceId, count) -> {
            String courierName = getCourierName(deviceId);
            statsText.append(String.format("%s: %d доставок\n", courierName, count));
        });

        // Дополнительная статистика для системы доставки
        statsText.append("\n=== 📊 ДОПОЛНИТЕЛЬНО ===\n");
        statsText.append(String.format("Общее время симуляции: %.3f мин\n", simulationCore.getCurrentTime()));

        double efficiency = stats.getTotalRequests() > 0 ?
                (double) stats.getCompletedRequests() / stats.getTotalRequests() * 100 : 0;
        statsText.append(String.format("Эффективность системы: %.1f%%\n", efficiency));

        statsArea.setText(statsText.toString());

        Button closeButton = new Button("Закрыть");
        closeButton.setOnAction(e -> statsStage.close());

        root.getChildren().addAll(titleLabel, statsArea, closeButton);
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root);
        statsStage.setScene(scene);
        statsStage.show();
    }

    private String getKitchenName(int sourceId) {
        switch (sourceId) {
            default: return "🍽️ Кухня " + sourceId;
        }
    }

    private String getCourierName(int deviceId) {
        switch (deviceId) {
            default: return "🚐 Курьер " + deviceId;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}