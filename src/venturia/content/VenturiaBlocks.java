package venturia.content;

import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.blocks.production.GenericCrafter;
import venturia.world.block.power.*;

import static mindustry.type.ItemStack.*;

/** Blocks of the mod. */
public class VenturiaBlocks{
    public static WindTurbine windWheel, windTurbine;
    public static GenericCrafter cobaltForge;
    public static Wall cobaltWall, cobaltWallLarge;

    public static void load(){
        // Starter power: the output scales with the wind strength (see WindTurbine).
        windWheel = new WindTurbine("wind-wheel", 1.2f){{
            requirements(Category.power, with(Items.copper, 25, Items.lead, 10));
            size = 1;
            alwaysUnlocked = true;
            shownPlanets.add(VenturiaPlanets.venturia);
        }};

        windTurbine = new WindTurbine("wind-turbine", 5f){{
            requirements(Category.power, with(Items.copper, 60, Items.lead, 20));
            size = 2;
            alwaysUnlocked = true;
            shownPlanets.add(VenturiaPlanets.venturia);
        }};

        // Copper + lead -> cobalt alloy, powered by wind turbines.
        cobaltForge = new GenericCrafter("cobalt-forge"){{
            requirements(Category.crafting, with(Items.copper, 30, Items.lead, 25));
            craftEffect = Fx.smeltsmoke;
            outputItem = new ItemStack(VenturiaItems.cobalt, 1);
            craftTime = 60f;
            size = 2;
            hasPower = true;
            alwaysUnlocked = true;
            shownPlanets.add(VenturiaPlanets.venturia);
            consumeItems(with(Items.copper, 1, Items.lead, 2));
            consumePower(0.6f);
        }};

        // Cobalt armor walls: the first use of the alloy.
        cobaltWall = new Wall("cobalt-wall"){{
            requirements(Category.defense, with(VenturiaItems.cobalt, 6));
            health = 130 * 4; // vanilla style: base * wallHealthMultiplier
            alwaysUnlocked = true;
            shownPlanets.add(VenturiaPlanets.venturia);
        }};

        cobaltWallLarge = new Wall("cobalt-wall-large"){{
            requirements(Category.defense, ItemStack.mult(cobaltWall.requirements, 4));
            health = 130 * 4 * 4;
            size = 2;
            alwaysUnlocked = true;
            shownPlanets.add(VenturiaPlanets.venturia);
        }};

        Log.info("[Venturia] blocks: @, @, @, @, @",
            windWheel.name, windTurbine.name, cobaltForge.name, cobaltWall.name, cobaltWallLarge.name);
    }
}
