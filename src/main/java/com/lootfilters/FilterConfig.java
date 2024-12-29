package com.lootfilters;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.lootfilters.serde.ColorDeserializer;
import com.lootfilters.serde.ColorSerializer;
import com.lootfilters.serde.RuleDeserializer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.runelite.api.TileItem;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@Getter
@EqualsAndHashCode
public class FilterConfig {
    private final Rule rule;
    private final DisplayConfig display;

    public static String toJson(List<FilterConfig> filters) {
        var gson = new GsonBuilder()
                .registerTypeAdapter(Color.class, new ColorSerializer())
                .create();
        return gson.toJson(filters);
    }

    public static List<FilterConfig> fromJson(String json) {
        var gson = new GsonBuilder()
                .registerTypeAdapter(Color.class, new ColorDeserializer())
                .registerTypeAdapter(Rule.class, new RuleDeserializer())
                .create();
        return gson.fromJson(json, new TypeToken<ArrayList<FilterConfig>>() {}.getType());
    }

    public FilterConfig(Rule rule, DisplayConfig display) {
        this.rule = rule;
        this.display = display;
    }

    public boolean test(LootFiltersPlugin plugin, TileItem item) {
        return rule.test(plugin, item);
    }
}
