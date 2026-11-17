package venturia.content;

import arc.graphics.*;
import arc.util.*;
import mindustry.type.*;

/** Items of the mod. */
public class VenturiaItems{
    /** Cobalt alloy: the building material of the future Cobalt Compact faction. */
    public static Item cobalt;

    public static void load(){
        cobalt = new Item("cobalt", Color.valueOf("2f6fd0")){{
            cost = 1.1f;
            healthScaling = 0.2f;
            // Starter material: there is no tech tree yet (scope B).
            alwaysUnlocked = true;
            // Venturia-exclusive: does not appear on other planets.
            shownPlanets.add(VenturiaPlanets.venturia);
        }};

        Log.info("[Venturia] items: @", cobalt.name);
    }
}
