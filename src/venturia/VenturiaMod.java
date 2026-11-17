package venturia;

import arc.util.*;
import mindustry.mod.*;
import venturia.content.*;

/** Mod entry point. All content must be created inside {@link #loadContent()}. */
public class VenturiaMod extends Mod{

    public VenturiaMod(){
        Log.info("[Venturia] constructor loaded.");
    }

    @Override
    public void loadContent(){
        Log.info("[Venturia] loading content.");

        VenturiaContent.load();      // attributes, constants
        VenturiaWeathers.load();     // weather (wind)
        VenturiaPlanets.load();      // planet + sectors (before items: shownPlanets)
        VenturiaItems.load();        // items
        VenturiaBlocks.load();       // blocks
        VenturiaUnits.load();        // units
        VenturiaTechTree.load();     // tech trees (after blocks and planet!)

        Log.info("[Venturia] content loaded.");
    }
}
