package venturia.world.block.power;

import arc.math.*;
import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.world.blocks.power.*;
import venturia.content.*;

import static mindustry.type.ItemStack.*;

/**
 * Wind turbine: the output scales with the wind strength of the environment.
 * <p>
 * The wind is read from the environment attributes, which are recalculated every frame
 * in Logic.update() from the rules attributes and the active weather.
 */
public class WindTurbine extends PowerGenerator{
    /** Maximum output at wind strength 1.0. */
    public static final float maxOutput = 20f;

    public WindTurbine(String name){
        super(name);

        requirements(Category.power, with(Items.copper, 60, Items.lead, 20));
        size = 2;
        powerProduction = maxOutput;
    }

    public class WindTurbineBuild extends GeneratorBuild{
        @Override
        public void updateTile(){
            // The sign of the wind is the direction, the strength is the absolute value.
            float wind = Mathf.clamp(Math.abs(VenturiaContent.wind.env()), 0f, 1f);
            productionEfficiency = enabled ? wind : 0f;
        }
    }
}
