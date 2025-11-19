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
    private Button startButton;
    private Button stepButton;
    private Button autoButton;
    private Button resetButton;

    private Thread autoThread;
    private volatile boolean autoRunning = false;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Симулятор СМО");

        // Создаем модель системы
        List<Source> sources = createSources();
        Buffer buffer = new Buffer(1, 3);
        List<Device> devices = createDevices();

        simulationCore = new SimulationCore(sources, buffer, devices);

        // Создаем интерфейс
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #f0f0f0;");

        // Панель управления
        HBox controlPanel = createControlPanel();

        // Основная информационная панель
        HBox infoPanel = createInfoPanel();

        // Область статусов
        VBox statusPanel = createStatusPanel();

        // Область будущих событий
        VBox eventsPanel = createEventsPanel();

        root.getChildren().addAll(controlPanel, infoPanel, statusPanel, eventsPanel);

        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.show();

        updateUI();
    }

    private HBox createControlPanel() {
        HBox controlPanel = new HBox(10);
        controlPanel.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Симулятор СМО");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        startButton = new Button("Старт");
        stepButton = new Button("Шаг");
        autoButton = new Button("Авто");
        resetButton = new Button("Сброс");

        startButton.setOnAction(e -> startSimulation());
        stepButton.setOnAction(e -> performStep());
        autoButton.setOnAction(e -> startAutoMode());
        resetButton.setOnAction(e -> resetSimulation());

        // Изначально только кнопка Старт активна
        stepButton.setDisable(true);
        autoButton.setDisable(true);

        statusLabel = new Label("Статус: Нажмите Старт");
        statusLabel.setStyle("-fx-text-fill: gray; -fx-font-weight: bold;");

        controlPanel.getChildren().addAll(
                titleLabel, startButton, stepButton, autoButton, resetButton,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                statusLabel
        );

        return controlPanel;
    }

    private HBox createInfoPanel() {
        HBox infoPanel = new HBox(10);

        currentTimeLabel = new Label("Текущее время: 0.000");
        currentTimeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        infoPanel.getChildren().add(currentTimeLabel);
        return infoPanel;
    }

    private VBox createStatusPanel() {
        VBox statusPanel = new VBox(5);
        statusPanel.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-padding: 10;");

        Label titleLabel = new Label("=== СОСТОЯНИЯ СИСТЕМЫ ===");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        statusArea = new TextArea();
        statusArea.setPrefHeight(300);
        statusArea.setEditable(false);
        statusArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px;");

        statusPanel.getChildren().addAll(titleLabel, statusArea);
        return statusPanel;
    }

    private VBox createEventsPanel() {
        VBox eventsPanel = new VBox(5);
        eventsPanel.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-padding: 10;");

        Label titleLabel = new Label("=== БУДУЩИЕ СОБЫТИЯ ===");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        eventsArea = new TextArea();
        eventsArea.setPrefHeight(200);
        eventsArea.setEditable(false);
        eventsArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px;");

        eventsPanel.getChildren().addAll(titleLabel, eventsArea);
        return eventsPanel;
    }

    private List<Source> createSources() {
        List<Source> sources = new ArrayList<>();
        sources.add(new Source(1, 1.0));
        sources.add(new Source(2, 0.5));
        sources.add(new Source(3, 0.8));
        return sources;
    }

    private List<Device> createDevices() {
        List<Device> devices = new ArrayList<>();
        devices.add(new Device(1));
        devices.add(new Device(2));
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
        statusLabel.setText("Статус: Автоматический режим");
        statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

        autoThread = new Thread(() -> {
            simulationCore.runAutomatic(10.0);

            Platform.runLater(() -> {
                autoRunning = false;
                stepButton.setDisable(true);
                autoButton.setDisable(true);
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

        // Создаем новую симуляцию
        List<Source> sources = createSources();
        Buffer buffer = new Buffer(1, 3);
        List<Device> devices = createDevices();
        simulationCore = new SimulationCore(sources, buffer, devices);

        startButton.setDisable(false);
        stepButton.setDisable(true);
        autoButton.setDisable(true);
        updateUI();
        statusLabel.setText("Статус: Нажмите Старт");
        statusLabel.setStyle("-fx-text-fill: gray; -fx-font-weight: bold;");
    }

    private void updateUI() {
        currentTimeLabel.setText(String.format("Текущее время: %.3f", simulationCore.getCurrentTime()));
        updateStatusArea();
        updateEventsArea();
    }

    private void updateStatusArea() {
        StringBuilder statusText = new StringBuilder();

        statusText.append("=== ИСТОЧНИКИ ===\n");
        for (Source source : simulationCore.getSources()) {
            statusText.append(String.format("И%d: след.событие=%.3f, создано=%d\n",
                    source.getId(), source.getNextEventTime(), source.getRequestsCount()));
        }

        statusText.append("\n=== ПРИБОРЫ ===\n");
        for (Device device : simulationCore.getDevices()) {
            String status = device.isBusy() ? "Занят" : "Свободен";
            String requestInfo = device.isBusy() && device.getCurrentRequest() != null ?
                    String.format(" заявка %s", device.getCurrentRequest().getId()) : "";
            statusText.append(String.format("П%d: %s%s\n",
                    device.getId(), status, requestInfo));
        }

        statusText.append("\n=== БУФЕР ===\n");
        Buffer buffer = simulationCore.getBuffer();
        Request[] requests = buffer.getRequests();
        if (requests != null) {
            for (int i = 0; i < requests.length; i++) {
                String posStatus = requests[i] != null ?
                        String.format("заявка %s", requests[i].getId()) : "Свободно";
                String pointer = (i == buffer.getPointer()) ? " <-- УБ" : "";
                statusText.append(String.format("Позиция %d: %s%s\n", i + 1, posStatus, pointer));
            }
        }

        StatisticsCollector stats = simulationCore.getStatistics();
        statusText.append(String.format("\n=== ОТКАЗЫ ===\n"));
        statusText.append(String.format("Всего отказов: %d\n", stats.getRejectedRequests()));
        statusText.append(String.format("Вероятность отказа: %.3f\n", stats.getRejectionProbability()));

        statusArea.setText(statusText.toString());
    }

    private void updateEventsArea() {
        StringBuilder eventsText = new StringBuilder();

        List<SimulationEvent> futureEvents = simulationCore.getFutureEvents();
        if (futureEvents.isEmpty()) {
            eventsText.append("Нет будущих событий\n");
        } else {
            for (SimulationEvent event : futureEvents) {
                switch (event.getType()) {
                    case REQUEST_GENERATED:
                        eventsText.append(String.format("[%.3f] Создание заявки источником И%d\n",
                                event.getTime(), event.getSourceId()));
                        break;
                    case SERVICE_COMPLETED:
                        if (event.getRequest() != null) {
                            eventsText.append(String.format("[%.3f] Завершение обслуживания заявки %s прибором %s\n",
                                    event.getTime(), event.getRequest().getId(), event.getRequest().getDeviceId()));
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
        statsStage.setTitle("Статистика симуляции");

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        Label titleLabel = new Label("СТАТИСТИКА СИМУЛЯЦИИ");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        TextArea statsArea = new TextArea();
        statsArea.setEditable(false);
        statsArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px;");
        statsArea.setPrefSize(500, 400);

        StringBuilder statsText = new StringBuilder();
        statsText.append("=== ОБЩАЯ СТАТИСТИКА ===\n");
        statsText.append(String.format("Всего создано заявок: %d\n", stats.getTotalRequests()));
        statsText.append(String.format("Обслужено заявок: %d\n", stats.getCompletedRequests()));
        statsText.append(String.format("Отказов: %d\n", stats.getRejectedRequests()));
        statsText.append(String.format("Вероятность отказа: %.3f\n", stats.getRejectionProbability()));
        statsText.append(String.format("Среднее время ожидания: %.3f\n", stats.getAverageWaitTime()));
        statsText.append(String.format("Среднее время обслуживания: %.3f\n", stats.getAverageServiceTime()));

        statsText.append("\n=== ПО ИСТОЧНИКАМ ===\n");
        stats.getSourceRequests().forEach((sourceId, count) ->
                statsText.append(String.format("Источник И%d: %d заявок\n", sourceId, count)));

        statsText.append("\n=== ПО ПРИБОРАМ ===\n");
        stats.getDeviceProcessed().forEach((deviceId, count) ->
                statsText.append(String.format("Прибор П%d: %d заявок\n", deviceId, count)));

        statsArea.setText(statsText.toString());

        Button closeButton = new Button("Закрыть");
        closeButton.setOnAction(e -> statsStage.close());

        root.getChildren().addAll(titleLabel, statsArea, closeButton);
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root);
        statsStage.setScene(scene);
        statsStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}