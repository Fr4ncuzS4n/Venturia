package venturia.world.planet;

import arc.math.*;
import arc.math.geom.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.maps.generators.*;
import mindustry.type.*;
import mindustry.type.Weather.WeatherEntry;
import mindustry.world.*;
import venturia.content.*;

/** Minimal terrain generator of Venturia: sand, sand walls and copper ore. */
public class VenturiaPlanetGenerator extends PlanetGenerator{
    /** Chance of a tile to contain copper ore. */
    public static final float oreChance = 0.12f;

    public final VenturiaPlanet planet;

    public VenturiaPlanetGenerator(VenturiaPlanet planet){
        this.planet = planet;
    }

    @Override
    public void genTile(Vec3 point, TileGen gen){
        gen.floor = Blocks.sand;
        gen.block = Blocks.sandWall;
        gen.overlay = ore(point) ? Blocks.oreCopper : Blocks.air;
    }

    @Override
    public void addWeather(Sector sector, Rules rules){
        super.addWeather(sector, rules); // vanilla weather by terrain

        // Every second sector is windier: gales are more frequent there, calm is rarer.
        boolean windy = sector.id % 2 == 0;

        rules.weather.add(new WeatherEntry(VenturiaWeathers.gale,
            windy ? 45f : 90f, windy ? 90f : 180f, 150f, 300f));
        rules.weather.add(new WeatherEntry(VenturiaWeathers.calm,
            windy ? 150f : 60f, windy ? 240f : 120f, 120f, 240f));
    }

    /** Deterministic per-tile ore check. */
    private boolean ore(Vec3 point){
        Rand rand = new Rand((long)(point.x * 1000f) * 73856093L ^ (long)(point.z * 1000f) * 19349663L);
        return rand.nextFloat() < oreChance;
    }
}
