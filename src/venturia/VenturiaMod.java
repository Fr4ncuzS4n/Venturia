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

        VenturiaContent.load();      // attributes, liquids, items
        VenturiaWeathers.load();     // weather (wind)
        VenturiaBlocks.load();       // blocks
        VenturiaUnits.load();        // units
        VenturiaPlanets.load();      // planet + sectors
        VenturiaTechTree.load();     // tech trees (after blocks and planet!)

        Log.info("[Venturia] content loaded.");
    }
}
