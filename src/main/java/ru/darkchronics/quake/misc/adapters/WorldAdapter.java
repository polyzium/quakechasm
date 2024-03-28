package ru.darkchronics.quake.misc.adapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.IOException;

public class WorldAdapter extends TypeAdapter<World> {
    @Override
    public void write(JsonWriter out, World value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        out.value(value.getName());
    }

    @Override
    public World read(JsonReader in) throws IOException {
        String worldName = in.nextString();
        return Bukkit.getWorld(worldName);
    }
}
