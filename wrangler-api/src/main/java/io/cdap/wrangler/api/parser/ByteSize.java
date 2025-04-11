package io.cdap.wrangler.api.parser;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import io.cdap.wrangler.api.annotations.PublicEvolving;


@PublicEvolving
public class ByteSize implements Token {
    private final long bytes;

    public ByteSize(String value) {
        this.bytes = parseByteSize(value);
    }

    private long parseByteSize(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Byte size cannot be null or empty");
        }
        
        // Extract values
        String unit = value.replaceAll("[0-9.]+", "").toUpperCase().trim();
        String numberPart = value.replaceAll("[^0-9.]+", "").trim();

        // Check if either unit or number missing
        if (unit.isEmpty() || numberPart.isEmpty()) {
            throw new IllegalArgumentException("Invalid ByteSize value: " + value);
        }

        double num = Double.parseDouble(numberPart);

        switch (unit) {
            case "KB":
                return (long)(num * 1000);
            case "MB":
                return (long)(num * Math.pow(1000, 2));
            case "GB":
                return (long)(num * Math.pow(1000, 3));
            case "TB":
                return (long)(num * Math.pow(1000, 4));
            case "PB":
                return (long)(num * Math.pow(1000, 5));
            case "B":
                return (long)num;
            default:
                throw new IllegalArgumentException("Invalid byte type: " + value);
        }        
        
    }

    public long getBytes() {
        return this.bytes;
    }

    @Override
    public Object value() {
        return this.bytes;
    }

    @Override
    public TokenType type() {
        return TokenType.BYTE_SIZE;
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", this.type().name());
        object.addProperty("bytes", this.bytes);
        return object;
    }

    
}
