package dev.rogu.forgetowns.data;

import com.google.gson.*;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.level.LevelEvent;
import dev.rogu.forgetowns.data.Plot.PlotType;

public class TownDataStorage {

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(GovernmentType.class, new GovernmentTypeAdapter())
        .registerTypeAdapter(Plot.class, new PlotAdapter())
        .registerTypeAdapter(BlockPos.class, new BlockPosAdapter())
        .create();
    private static Map<String, Town> towns = new HashMap<>();
    private static Map<String, Nation> nations = new HashMap<>();

    public static Map<String, Town> getTowns() {
        return towns;
    }

    public static Map<String, Nation> getNations() {
        return nations;
    }

    public static void save(LevelEvent.Save event) {
        Path path = event
            .getLevel()
            .getServer()
            .getWorldPath(new LevelResource("forgetowns.json"));
        try (FileWriter writer = new FileWriter(path.toFile())) {
            GSON.toJson(new DataHolder(towns, nations), writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load(LevelEvent.Load event) {
        Path path = event
            .getLevel()
            .getServer()
            .getWorldPath(new LevelResource("forgetowns.json"));
        try (FileReader reader = new FileReader(path.toFile())) {
            DataHolder data = GSON.fromJson(reader, DataHolder.class);
            towns = data.towns != null ? data.towns : new HashMap<>();
            nations = data.nations != null ? data.nations : new HashMap<>();
            towns
                .values()
                .forEach(town ->
                    town.setLevel(
                        event.getLevel().getServer().getLevel(Level.OVERWORLD)
                    )
                );
        } catch (IOException e) {
            towns = new HashMap<>();
            nations = new HashMap<>();
        }
    }

    private static class DataHolder {

        Map<String, Town> towns;
        Map<String, Nation> nations;

        DataHolder(Map<String, Town> towns, Map<String, Nation> nations) {
            this.towns = towns;
            this.nations = nations;
        }
    }

    private static class GovernmentTypeAdapter
        implements
            JsonSerializer<GovernmentType>, JsonDeserializer<GovernmentType> {

        @Override
        public JsonElement serialize(
            GovernmentType src,
            Type typeOfSrc,
            JsonSerializationContext context
        ) {
            return new JsonPrimitive(src.name());
        }

        @Override
        public GovernmentType deserialize(
            JsonElement json,
            Type typeOfT,
            JsonDeserializationContext context
        ) throws JsonParseException {
            return GovernmentType.valueOf(json.getAsString());
        }
    }

    private static class PlotAdapter
        implements JsonSerializer<Plot>, JsonDeserializer<Plot> {

        @Override
        public JsonElement serialize(
            Plot src,
            Type typeOfSrc,
            JsonSerializationContext context
        ) {
            JsonObject obj = new JsonObject();
            if (src.getOwner() != null) obj.addProperty(
                "owner",
                src.getOwner().toString()
            );
            JsonArray corners = new JsonArray();
            src
                .getCorners()
                .forEach(corner -> corners.add(context.serialize(corner)));
            obj.add("corners", corners);
            obj.addProperty("price", src.getPrice());
            if (src.getSignPos() != null) obj.add(
                "signPos",
                context.serialize(src.getSignPos())
            );
            JsonArray invited = new JsonArray();
            src.getInvited().forEach(uuid -> invited.add(uuid.toString()));
            obj.add("invited", invited);
            obj.addProperty("type", src.getType().name());
            return obj;
        }

        @Override
        public Plot deserialize(
            JsonElement json,
            Type typeOfT,
            JsonDeserializationContext context
        ) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            UUID owner = obj.has("owner")
                ? UUID.fromString(obj.get("owner").getAsString())
                : null;
            List<BlockPos> corners = new ArrayList<>();
            obj
                .get("corners")
                .getAsJsonArray()
                .forEach(elem ->
                    corners.add(context.deserialize(elem, BlockPos.class))
                );
            int price = obj.get("price").getAsInt();
            PlotType type = PlotType.valueOf(obj.get("type").getAsString());
            Plot plot = new Plot(corners, price, type);
            if (obj.has("signPos")) plot.setSignPos(
                context.deserialize(obj.get("signPos"), BlockPos.class)
            );
            plot.setOwner(owner);
            if (obj.has("invited")) {
                obj
                    .get("invited")
                    .getAsJsonArray()
                    .forEach(elem ->
                        plot.invite(UUID.fromString(elem.getAsString()))
                    );
            }
            return plot;
        }
    }

    private static class BlockPosAdapter
        implements JsonSerializer<BlockPos>, JsonDeserializer<BlockPos> {

        @Override
        public JsonElement serialize(
            BlockPos src,
            Type typeOfSrc,
            JsonSerializationContext context
        ) {
            JsonObject obj = new JsonObject();
            obj.addProperty("x", src.getX());
            obj.addProperty("y", src.getY());
            obj.addProperty("z", src.getZ());
            return obj;
        }

        @Override
        public BlockPos deserialize(
            JsonElement json,
            Type typeOfT,
            JsonDeserializationContext context
        ) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            return new BlockPos(
                obj.get("x").getAsInt(),
                obj.get("y").getAsInt(),
                obj.get("z").getAsInt()
            );
        }
    }
}
