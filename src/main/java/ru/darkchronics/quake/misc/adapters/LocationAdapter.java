/*
 * DarkChronics-Quake, a Quake minigame plugin for Minecraft servers running PaperMC
 * 
 * Copyright (C) 2024-present Polyzium
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.darkchronics.quake.misc.adapters;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Type;

public class LocationAdapter implements JsonDeserializer<Location>, JsonSerializer<Location> {

    public static final LocationAdapter INSTANCE = new LocationAdapter();

    @Override
    public Location deserialize( JsonElement json, Type type, JsonDeserializationContext jsonDeserializationContext ) throws JsonParseException {

        if ( !json.isJsonObject() ) {
            throw new JsonParseException( "not a JSON object" );
        }

        final JsonObject obj = (JsonObject) json;
        final JsonElement world = obj.get( "world" );
        final JsonElement x = obj.get( "x" );
        final JsonElement y = obj.get( "y" );
        final JsonElement z = obj.get( "z" );
        final JsonElement yaw = obj.get( "yaw" );
        final JsonElement pitch = obj.get( "pitch" );

        if ( world == null || x == null || y == null || z == null || yaw == null || pitch == null ) {
            throw new JsonParseException( "Malformed location json string!" );
        }

        if ( !world.isJsonPrimitive() || !((JsonPrimitive) world).isString() ) {
            throw new JsonParseException( "world is not a string" );
        }

        if ( !x.isJsonPrimitive() || !((JsonPrimitive) x).isNumber() ) {
            throw new JsonParseException( "x is not a number" );
        }

        if ( !y.isJsonPrimitive() || !((JsonPrimitive) y).isNumber() ) {
            throw new JsonParseException( "y is not a number" );
        }

        if ( !z.isJsonPrimitive() || !((JsonPrimitive) z).isNumber() ) {
            throw new JsonParseException( "z is not a number" );
        }

        if ( !yaw.isJsonPrimitive() || !((JsonPrimitive) yaw).isNumber() ) {
            throw new JsonParseException( "yaw is not a number" );
        }

        if ( !pitch.isJsonPrimitive() || !((JsonPrimitive) pitch).isNumber() ) {
            throw new JsonParseException( "pitch is not a number" );
        }

        World worldInstance = Bukkit.getWorld( world.getAsString() );
        if (worldInstance == null) {
            throw new IllegalArgumentException("Unknown/not loaded world");
        }

        return new Location( worldInstance, x.getAsDouble(), y.getAsDouble(), z.getAsDouble(), yaw.getAsFloat(), pitch.getAsFloat() );

    }

    @Override
    public JsonElement serialize( Location location, Type type, JsonSerializationContext jsonSerializationContext ) {

        final JsonObject obj = new JsonObject();
        obj.addProperty( "world", location.getWorld().getName() );
        obj.addProperty( "x", location.getX() );
        obj.addProperty( "y", location.getY() );
        obj.addProperty( "z", location.getZ() );
        obj.addProperty( "yaw", location.getYaw() );
        obj.addProperty( "pitch", location.getPitch() );
        return obj;

    }

}
