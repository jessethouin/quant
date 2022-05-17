package com.jessethouin.quant.conf;

import lombok.Getter;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum AssetClassType {
    CRYPTO("crypto"),
    US_EQUITY("us_equity");

    @Getter
    private final String assetClass;
    private static final Map<String, AssetClassType> ENUM_MAP;

    AssetClassType(String assetClass) {
        this.assetClass = assetClass;
    }

    static {
        Map<String, AssetClassType> map = new ConcurrentHashMap<>();
        for (AssetClassType instance : AssetClassType.values()) {
            map.put(instance.getAssetClass().toLowerCase(),instance);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    public static AssetClassType get(String name) {
        return ENUM_MAP.get(name.toLowerCase());
    }
}
