package venturia.content;

import arc.util.*;
import venturia.world.planet.*;

/** Planets of the mod. */
public class VenturiaPlanets{
    public static VenturiaPlanet venturia;

    public static void load(){
        venturia = new VenturiaPlanet();

        Log.info("[Venturia] planet: @", venturia.name);
    }
}
