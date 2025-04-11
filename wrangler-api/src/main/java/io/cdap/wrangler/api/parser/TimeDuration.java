package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cdap.wrangler.api.annotations.PublicEvolving;

@PublicEvolving
public class TimeDuration implements Token {
    private final long milliseconds;

    public TimeDuration(String value) {
        this.milliseconds = parseTimeDuration(value);
    }

    private long parseTimeDuration(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Time duration cannot be null or empty");
        }

        // Extract values
        String unit = value.replaceAll("[0-9.]+", "").toLowerCase().trim();
        String numberPart = value.replaceAll("[^0-9.]+", "").trim();

        // Check if either unit or number is missing
        if (unit.isEmpty() || numberPart.isEmpty()) {
            throw new IllegalArgumentException("Invalid TimeDuration value: " + value);
        }

        double num = Double.parseDouble(numberPart);

        switch (unit) {
            case "ms":
                return (long)num;
            case "s":
            case "sec":
                return (long)(num * 1000);
            case "m":
            case "min":
                return (long)(num * 60 * 1000);
            case "h":
            case "hr":
                return (long)(num * 3600 * 1000);
            case "d":
            case "day":
                return (long)(num * 86400 * 1000);
            default:
                throw new IllegalArgumentException("Invalid time unit: " + unit);
        }
    }

    public long getMilliseconds() {
        return this.milliseconds;
    }

    @Override
    public Object value() {
        return this.milliseconds;
    }

    @Override
    public TokenType type() {
        return TokenType.TIME_DURATION;
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", this.type().name());
        object.addProperty("milliseconds", this.milliseconds);
        return object;
    }
}
