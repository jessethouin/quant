package com.jessethouin.quant.conf;

import lombok.Getter;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum AssetClassTypes {
    CRYPTO("crypto"),
    US_EQUITY("us_equity");

    @Getter
    private final String assetClass;
    private static final Map<String, AssetClassTypes> ENUM_MAP;

    AssetClassTypes(String assetClass) {
        this.assetClass = assetClass;
    }

    static {
        Map<String, AssetClassTypes> map = new ConcurrentHashMap<>();
        for (AssetClassTypes instance : AssetClassTypes.values()) {
            map.put(instance.getAssetClass().toLowerCase(),instance);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    public static AssetClassTypes get(String name) {
        return ENUM_MAP.get(name.toLowerCase());
    }
}
