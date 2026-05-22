package org.example;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class WeatherService {

    private static final String API_KEY = "ed36a986-58c7-4cb8-b706-e2aaaa2caf21";

    public JSONObject getWeatherData(String city) throws Exception {
        JSONObject locationInfo = getLocationInfo(city);

        // Используем Locale.US, чтобы разделителем была точка
        String urlString = String.format(Locale.US,
                "https://api.weather.yandex.ru/v2/forecast?lat=%f&lon=%f&lang=ru_RU&limit=7&hours=true",
                locationInfo.getDouble("lat"), locationInfo.getDouble("lon"));

        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("X-Yandex-Weather-Key", API_KEY);

        if (connection.getResponseCode() != 200) {
            throw new Exception("Ошибка API погоды: " + connection.getResponseCode());
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);

            JSONObject weather = new JSONObject(response.toString());
            weather.put("location_info", locationInfo);
            return weather;
        }
    }

    private JSONObject getLocationInfo(String city) throws Exception {
        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String geoUrl = "https://nominatim.openstreetmap.org/search?q=" + encodedCity + "&format=json&addressdetails=1&limit=1";

        HttpURLConnection connection = (HttpURLConnection) new URL(geoUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "WeatherApp");

        if (connection.getResponseCode() != 200) throw new Exception("Ошибка поиска города.");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);

            JSONArray array = new JSONArray(response.toString());
            if (array.isEmpty()) throw new Exception("Локация не найдена.");

            JSONObject object = array.getJSONObject(0);
            JSONObject address = object.getJSONObject("address");

            JSONObject result = new JSONObject();
            // Используем Locale.US для безопасного парсинга
            result.put("lat", Double.parseDouble(object.getString("lat")));
            result.put("lon", Double.parseDouble(object.getString("lon")));
            result.put("country", address.optString("country", "Неизвестная страна"));
            result.put("region", address.optString("state", "Неизвестный регион"));

            String village = address.optString("village", "");
            String town = address.optString("town", "");
            String cityName = address.optString("city", "");

            if (!village.isEmpty()) {
                result.put("type", "Село");
                result.put("place", village);
            } else {
                result.put("type", "Город");
                result.put("place", !town.isEmpty() ? town : cityName);
            }
            return result;
        }
    }
}