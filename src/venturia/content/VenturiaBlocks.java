package venturia.content;

import arc.util.*;
import mindustry.world.*;
import venturia.world.block.power.*;

/** Blocks of the mod. */
public class VenturiaBlocks{
    public static Block windTurbine;

    public static void load(){
        windTurbine = new WindTurbine("wind-turbine");

        Log.info("[Venturia] blocks: @", windTurbine.name);
    }
}
