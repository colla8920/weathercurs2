package org.example;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WeatherApp extends Application {

    private final UserService userService = new UserService();
    private final WeatherService weatherService = new WeatherService();

    private Stage primaryStage;
    private String currentUsername = "Гость";
    private final String defaultCity = "Липецк";

    private ScrollPane mainScrollPane;
    private VBox weekCard;
    private HBox hoursTimeline;
    private Label cityLabel, tempLabel, statusLabel, locationInfoLabel, coordinatesLabel;
    private Label feelsLikeValueLabel, windValueLabel, humidityLabel, pressureLabel, popValueLabel, weekTitle;
    private TextField searchField;

    private double xOffset = 0, yOffset = 0;
    private static final Map<String, String> conditionMap = new HashMap<>();

    static {
        conditionMap.put("clear", "Ясно");
        conditionMap.put("partly-cloudy", "Малооблачно");
        conditionMap.put("cloudy", "Облачно");
        conditionMap.put("overcast", "Пасмурно");
        conditionMap.put("drizzle", "Морось");
        conditionMap.put("light-rain", "Небольшой дождь");
        conditionMap.put("rain", "Дождь");
        conditionMap.put("heavy-rain", "Сильный дождь");
        conditionMap.put("showers", "Ливень");
        conditionMap.put("wet-snow", "Дождь со снегом");
        conditionMap.put("light-snow", "Небольшой снег");
        conditionMap.put("snow", "Снег");
        conditionMap.put("snow-showers", "Снегопад");
        conditionMap.put("hail", "Град");
        conditionMap.put("thunderstorm", "Гроза");
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        LoggerService.log("INFO", "Приложение запущено.");
        showAuthScreen();
    }

    private void showAuthScreen() {
        VBox root = new VBox();
        root.getStyleClass().add("main-background");
        root.setSpacing(30);
        setupWindowDragging(root);

        VBox centerBox = new VBox(20);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(40));

        VBox authCard = new VBox(15);
        authCard.getStyleClass().add("glass-card");
        authCard.setMaxWidth(360);
        authCard.setAlignment(Pos.CENTER);

        TextField loginField = new TextField();
        loginField.setPromptText("Логин");
        loginField.getStyleClass().add("search-field");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Пароль");
        passwordField.getStyleClass().add("search-field");

        HBox buttons = new HBox(15);
        buttons.setAlignment(Pos.CENTER);
        Button loginBtn = new Button("Войти");
        loginBtn.getStyleClass().add("modern-button");
        Button registerBtn = new Button("Регистрация");
        registerBtn.getStyleClass().add("modern-button");
        buttons.getChildren().addAll(registerBtn, loginBtn);

        authCard.getChildren().addAll(loginField, passwordField, buttons);
        centerBox.getChildren().addAll(authCard);
        root.getChildren().addAll(createHeader(), centerBox);

        loginBtn.setOnAction(e -> {
            String login = loginField.getText().trim();
            if (userService.login(login, passwordField.getText().trim())) {
                currentUsername = login;
                LoggerService.log("INFO", "Пользователь " + login + " вошел в систему.");
                showMainScreen();
            } else {
                LoggerService.log("WARN", "Неудачная попытка входа: " + login);
                showNativeAlert(Alert.AlertType.ERROR, "Ошибка", "Неверный логин или пароль.");
            }
        });

        registerBtn.setOnAction(e -> {
            String login = loginField.getText().trim();
            if (userService.register(login, passwordField.getText().trim())) {
                LoggerService.log("INFO", "Регистрация нового пользователя: " + login);
                showNativeAlert(Alert.AlertType.INFORMATION, "Успех", "Пользователь зарегистрирован.");
            } else {
                LoggerService.log("WARN", "Ошибка регистрации: пользователь уже существует (" + login + ")");
                showNativeAlert(Alert.AlertType.ERROR, "Ошибка", "Пользователь уже существует.");
            }
        });

        Scene scene = new Scene(root, 520, 500);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
        fadeAnimation(root);
    }

    private void showMainScreen() {
        VBox root = new VBox();
        root.getStyleClass().add("main-background");
        setupWindowDragging(root);

        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(20));

        searchField = new TextField();
        searchField.setPromptText("Поиск города...");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(260);

        Button searchBtn = new Button("Найти");
        searchBtn.getStyleClass().add("modern-button");

        Label userLabel = new Label("Пользователь: " + currentUsername);
        userLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.75); -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topBar.getChildren().addAll(searchField, searchBtn, spacer, userLabel);

        VBox content = new VBox(25);
        content.setPadding(new Insets(25));

        VBox heroCard = new VBox(10);
        heroCard.getStyleClass().add("glass-card");
        heroCard.setAlignment(Pos.CENTER);
        cityLabel = new Label("Загрузка...");
        cityLabel.getStyleClass().add("city-label");
        locationInfoLabel = new Label();
        locationInfoLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.75); -fx-font-size: 15px;");
        coordinatesLabel = new Label();
        coordinatesLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 13px;");
        tempLabel = new Label("--°");
        tempLabel.getStyleClass().add("temp-label");
        statusLabel = new Label("Получение данных...");
        statusLabel.getStyleClass().add("status-label");
        heroCard.getChildren().addAll(cityLabel, locationInfoLabel, coordinatesLabel, tempLabel, statusLabel);

        VBox hourlyCard = new VBox(20);
        hourlyCard.getStyleClass().add("glass-card");
        hoursTimeline = new HBox(20);
        hoursTimeline.setAlignment(Pos.CENTER);
        hourlyCard.getChildren().addAll(new Label("ПОЧАСОВОЙ ПРОГНОЗ"), hoursTimeline);

        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(20); metricsGrid.setVgap(20);
        feelsLikeValueLabel = new Label("--°");
        windValueLabel = new Label("--");
        humidityLabel = new Label("--");
        pressureLabel = new Label("--");
        popValueLabel = new Label("--");
        metricsGrid.add(createMetricCard("Ощущается как", feelsLikeValueLabel), 0, 0);
        metricsGrid.add(createMetricCard("Ветер", windValueLabel), 1, 0);
        metricsGrid.add(createMetricCard("Влажность", humidityLabel), 0, 1);
        metricsGrid.add(createMetricCard("Давление", pressureLabel), 1, 1);
        metricsGrid.add(createMetricCard("Осадки", popValueLabel), 2, 0);

        weekCard = new VBox(12);
        weekCard.getStyleClass().add("glass-card");
        weekTitle = new Label("ПРОГНОЗ");
        Button exportBtn = new Button("Экспорт отчета");
        exportBtn.getStyleClass().add("modern-button");

        content.getChildren().addAll(heroCard, hourlyCard, metricsGrid, weekTitle, weekCard, exportBtn);
        mainScrollPane = new ScrollPane(content);
        mainScrollPane.setFitToWidth(true);
        mainScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        root.getChildren().addAll(createHeader(), topBar, mainScrollPane);

        searchBtn.setOnAction(e -> {
            String city = searchField.getText().trim();
            LoggerService.log("INFO", "Поиск погоды для: " + city);
            fetchWeatherDataAsync(city);
        });

        exportBtn.setOnAction(e -> {
            try {
                // Собираем все данные в одну строку
                String reportData = String.format(
                        "Temperature: %s\n" +
                                "Feels like: %s\n" +
                                "Wind: %s\n" +
                                "Humidity: %s\n" +
                                "Pressure: %s",
                        tempLabel.getText(),
                        feelsLikeValueLabel.getText(),
                        windValueLabel.getText(),
                        humidityLabel.getText(),
                        pressureLabel.getText()
                );

                ReportGenerator.generateWeatherReport(cityLabel.getText(), reportData);
                LoggerService.log("INFO", "Full report generated for: " + cityLabel.getText());
                showNativeAlert(Alert.AlertType.INFORMATION, "Success", "Report saved successfully.");
            } catch (Exception ex) {
                LoggerService.log("ERROR", "Report generation failed: " + ex.getMessage());
                showNativeAlert(Alert.AlertType.ERROR, "Error", "Failed to save report.");
            }
        });

        Scene scene = new Scene(root, 900, 760);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
        fadeAnimation(root);
        fetchWeatherDataAsync(defaultCity);
    }

    private void fetchWeatherDataAsync(String city) {
        if (city == null || city.isBlank()) return;
        statusLabel.setText("Загрузка данных...");
        new Thread(() -> {
            try {
                JSONObject json = weatherService.getWeatherData(city);
                JSONObject fact = json.getJSONObject("fact");
                JSONObject locationInfo = json.getJSONObject("location_info");

                Platform.runLater(() -> {
                    cityLabel.setText(city.substring(0,1).toUpperCase() + city.substring(1));
                    locationInfoLabel.setText(locationInfo.getString("country") + ", " + locationInfo.getString("region") + "\n" + locationInfo.getString("type") + " " + locationInfo.getString("place"));
                    coordinatesLabel.setText(String.format("Координаты: %.4f°, %.4f°", locationInfo.getDouble("lat"), locationInfo.getDouble("lon")));
                    tempLabel.setText(fact.getInt("temp") + "°");
                    statusLabel.setText(translateCondition(fact.getString("condition")));
                    feelsLikeValueLabel.setText(fact.getInt("feels_like") + "°");
                    windValueLabel.setText(fact.optDouble("wind_speed", 0) + " м/с");
                    humidityLabel.setText(fact.optInt("humidity", 0) + "%");
                    pressureLabel.setText(fact.optInt("pressure_mm", 0) + " мм");

                    weekCard.getChildren().clear();
                    hoursTimeline.getChildren().clear();
                    JSONArray forecasts = json.getJSONArray("forecasts");
                    weekTitle.setText("Прогноз на " + forecasts.length() + " дней");

                    extractHourlyData(forecasts.getJSONObject(0));
                    for (int i = 0; i < forecasts.length(); i++) addForecastDay(forecasts.getJSONObject(i), i);
                });
            } catch (Exception ex) {
                LoggerService.log("ERROR", "Ошибка загрузки погоды: " + ex.getMessage());
                Platform.runLater(() -> showNativeAlert(Alert.AlertType.ERROR, "Ошибка", ex.getMessage()));
            }
        }).start();
    }

    // --- Остальные методы (extractHourlyData, addForecastDay, UI helpers) остаются без изменений ---
    private void extractHourlyData(JSONObject forecast) {
        JSONArray hours = forecast.getJSONArray("hours");
        int count = 0;
        for (int i = 0; i < hours.length() && count < 5; i++) {
            JSONObject hour = hours.getJSONObject(i);
            hoursTimeline.getChildren().add(createHourCard(hour.getString("hour") + ":00", hour.getInt("temp") + "°", hour.optInt("prec_prob", 0) + "%"));
            count++;
        }
    }

    private void addForecastDay(JSONObject forecast, int index) {
        JSONObject day = forecast.getJSONObject("parts").getJSONObject("day");
        String cond = translateCondition(day.optString("condition"));
        int max = day.optInt("temp_max", day.optInt("temp_avg", 0));
        int min = day.optInt("temp_min", day.optInt("temp_avg", 0));
        weekCard.getChildren().add(createWeekRow(formatDay(forecast.getString("date"), index), cond, max, min, day.optInt("prec_prob", 0)));
    }

    private VBox createMetricCard(String title, Label value) {
        VBox box = new VBox(10);
        box.getStyleClass().add("metric-card");
        Label t = new Label(title); t.setStyle("-fx-text-fill: rgba(255,255,255,0.6);");
        value.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        box.getChildren().addAll(t, value);
        return box;
    }

    private VBox createHourCard(String time, String temp, String rain) {
        VBox box = new VBox(10);
        box.getStyleClass().add("metric-card");
        box.setAlignment(Pos.CENTER);
        box.getChildren().addAll(new Label(time), new Label(temp) {{setStyle("-fx-font-size:18px;-fx-font-weight:bold;");}}, new Label("💧 " + rain));
        return box;
    }

    private HBox createWeekRow(String day, String cond, int max, int min, int rain) {
        HBox row = new HBox(25);
        row.getStyleClass().add("week-row");
        row.getChildren().addAll(new Label(day) {{setPrefWidth(140);}}, new Label(cond) {{setPrefWidth(220);}}, new Label(max + "°/" + min + "°"), new Label("💧 " + rain + "%"));
        return row;
    }

    private HBox createHeader() {
        HBox h = new HBox(10); h.setAlignment(Pos.CENTER_RIGHT); h.setPadding(new Insets(10));
        Button m = new Button("—"), c = new Button("✕");
        m.setOnAction(e -> primaryStage.setIconified(true));
        c.setOnAction(e -> System.exit(0));
        h.getChildren().addAll(m, c);
        return h;
    }

    private void fadeAnimation(VBox root) {
        FadeTransition f = new FadeTransition(Duration.millis(700), root);
        f.setFromValue(0); f.setToValue(1); f.play();
    }

    private String translateCondition(String c) { return conditionMap.getOrDefault(c, c); }
    private String formatDay(String date, int i) {
        if (i == 0) return "Сегодня";
        if (i == 1) return "Завтра";
        String s = LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEEE", new Locale("ru")));
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
    private void showNativeAlert(Alert.AlertType t, String title, String c) {
        Alert a = new Alert(t); a.setTitle(title); a.setContentText(c); a.showAndWait();
    }
    private void setupWindowDragging(Pane r) {
        r.setOnMousePressed(e -> { xOffset = e.getSceneX(); yOffset = e.getSceneY(); });
        r.setOnMouseDragged(e -> { primaryStage.setX(e.getScreenX() - xOffset); primaryStage.setY(e.getScreenY() - yOffset); });
    }
    public static void main(String[] args) { launch(args); }

    private void handleError(String userMessage, Exception ex) {
        String logMessage = userMessage + ": " + (ex != null ? ex.getMessage() : "Unknown error");
        LoggerService.log("ERROR", logMessage);
        Platform.runLater(() -> showNativeAlert(Alert.AlertType.ERROR, "System Error", userMessage));
    }
}