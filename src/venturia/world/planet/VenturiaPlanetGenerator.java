package venturia.world.planet;

import arc.graphics.Color;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.FloatSeq;
import arc.struct.Seq;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.maps.generators.*;
import mindustry.type.*;
import mindustry.type.Weather.WeatherEntry;
import mindustry.world.*;
import venturia.content.*;

/**
 * Terrain generator of Venturia: a desert planet with open sand seas, dark dune ridges,
 * rocky highlands, salt flats and rare water oases.
 * <p>
 * Ore richness depends on the sector position (see {@link #generate()}).
 */
public class VenturiaPlanetGenerator extends PlanetGenerator{
    /** Noise scale: lower = larger terrain features. */
    public static final float terrainScl = 5f;
    /** Height bands of the terrain (calibrated for pow(noise, 2.2), see rawHeight). */
    public static final float waterLevel = 0.13f;
    public static final float shoreLevel = 0.18f;
    public static final float duneLevel = 0.42f;
    public static final float rockLevel = 0.6f;
    /** Ridged-noise threshold for wall ridges (~15-20% of tiles). */
    public static final float wallThreshold = 0.31f;

    public final VenturiaPlanet planet;

    public VenturiaPlanetGenerator(VenturiaPlanet planet){
        this.planet = planet;
    }

    /** Raw terrain height of a planet point (0..1+). */
    float rawHeight(Vec3 position){
        return Mathf.pow(Simplex.noise3d(seed, 6, 0.5f, 1f / terrainScl,
            position.x * terrainScl, position.y * terrainScl, position.z * terrainScl), 2.2f);
    }

    /** Floor block for a planet point. */
    Block getFloor(Vec3 position){
        float height = Mathf.clamp(rawHeight(position) * 1.15f);

        // Salt flats in the middle heights
        float salt = Simplex.noise3d(seed + 2, 3, 0.5f, 1f / 4f,
            position.x * terrainScl, position.y * terrainScl + 10f, position.z * terrainScl);
        if(height > shoreLevel && height < duneLevel && salt > 0.68f) return Blocks.salt;

        if(height < waterLevel) return Blocks.water;
        if(height < shoreLevel) return Blocks.sandWater;
        if(height < duneLevel) return Blocks.sand;
        if(height < rockLevel) return Blocks.darksand;
        return Blocks.stone;
    }

    @Override
    public void genTile(Vec3 position, TileGen tile){
        tile.floor = getFloor(position);
        tile.overlay = Blocks.air;
        tile.block = Blocks.air;

        // Walls only along narrow ridged-noise bands: ridges in the desert.
        // Liquid floors have no wall (Blocks.air) and stay open.
        if(Ridged.noise3d(seed + 1, position.x, position.y, position.z, 2, 22) > wallThreshold){
            tile.block = tile.floor.asFloor().wall;
        }
    }

    @Override
    public float getHeight(Vec3 position){
        // Planet mesh: flatten everything below sea level.
        return Math.max(rawHeight(position), waterLevel);
    }

    @Override
    public void getColor(Vec3 position, Color out){
        out.set(getFloor(position).mapColor).a(1f - getFloor(position).albedo);
    }

    @Override
    protected float noise(float x, float y, double octaves, double falloff, double scl, double mag){
        // Scale up the projected sector patch: without this the noise is nearly
        // constant over a sector and the ore passes produce nothing.
        Vec3 v = sector.rect.project(x, y).scl(5f);
        return Simplex.noise3d(seed, octaves, falloff, 1f / scl, v.x, v.y, v.z) * (float)mag;
    }

    @Override
    protected void generate(){
        distort(10f, 6f);

        // Ore richness depends on the sector position on the planet.
        Seq<Block> ores = Seq.with(Blocks.oreCopper, Blocks.oreLead);
        float poles = Math.abs(sector.tile.v.y);
        float nmag = 0.5f;
        float scl = 1f;
        float addscl = 1.3f;

        if(Simplex.noise3d(seed, 2, 0.5, scl, sector.tile.v.x, sector.tile.v.y, sector.tile.v.z) * nmag + poles > 0.25f * addscl){
            ores.add(Blocks.oreCoal);
        }

        if(Simplex.noise3d(seed, 2, 0.5, scl, sector.tile.v.x + 1, sector.tile.v.y, sector.tile.v.z) * nmag + poles > 0.5f * addscl){
            ores.add(Blocks.oreTitanium);
        }

        FloatSeq frequencies = new FloatSeq();
        for(int i = 0; i < ores.size; i++){
            frequencies.add(rand.random(-0.1f, 0.01f) - i * 0.01f + poles * 0.04f);
        }

        pass((x, y) -> {
            if(!floor.asFloor().hasSurface()) return;

            int offsetX = x - 4, offsetY = y + 23;
            for(int i = ores.size - 1; i >= 0; i--){
                Block entry = ores.get(i);
                float freq = frequencies.get(i);
                if(Math.abs(0.5f - noise(offsetX, offsetY + i * 999, 2, 0.7, (40 + i * 2))) > 0.22f + i * 0.01 &&
                    Math.abs(0.5f - noise(offsetX, offsetY - i * 999, 1, 1, (30 + i * 4))) > 0.37f + freq){
                    ore = entry;
                    break;
                }
            }
        });

        trimDark();
        median(2);
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
}
