package venturia.world.planet;

import mindustry.content.*;
import mindustry.graphics.g3d.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.Env;
import venturia.content.*;

/** Venturia: the third planet, where the wind is the main source of power. */
public class VenturiaPlanet extends Planet{

    public VenturiaPlanet(){
        super("planet", Planets.sun, 1f, 3); // radius, sectorSize

        generator = new VenturiaPlanetGenerator(this);
        meshLoader = () -> new HexMesh(this, 5);
        cloudMeshLoader = () -> new HexMesh(this, 5);

        defaultEnv = Env.terrestrial | Env.oxygen;
        defaultAttributes.set(VenturiaContent.wind, VenturiaContent.baseWind);

        alwaysUnlocked = true;
        accessible = true;
        visible = true;

        // TODO (scope B): Cobalt core instead of the vanilla core shard.
        defaultCore = Blocks.coreShard;
    }
}
