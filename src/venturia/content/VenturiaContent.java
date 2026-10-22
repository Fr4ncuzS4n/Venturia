package venturia.content;

import arc.util.*;
import mindustry.world.meta.*;

/** Common constants and environment attributes of the mod. */
public class VenturiaContent{
    /** Planet name. Temporary - see AGENTS.md 4.1. */
    public static final String planetName = "venturia";
    /** Faction name. Temporary - see AGENTS.md 4.4. */
    public static final String factionName = "cobalt";

    /** Base wind strength of the planet (without weather). */
    public static final float baseWind = 0.3f;

    /** Wind environment attribute: strength (0..1+), sign = direction. */
    public static final Attribute wind = attribute("wind");

    /** Safe attribute creation: does not duplicate on content reload. */
    public static Attribute attribute(String name){
        return Attribute.exists(name) ? Attribute.get(name) : Attribute.add(name);
    }

    public static void load(){
        Log.info("[Venturia] wind attribute id = @", wind.id);
    }
}
