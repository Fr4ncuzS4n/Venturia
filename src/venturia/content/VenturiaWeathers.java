package venturia.content;

import arc.util.*;
import mindustry.gen.*;
import mindustry.type.*;

/**
 * Weather of Venturia.
 * <p>
 * Weather attributes are <b>added</b> to the rules attributes (see Logic.update()),
 * so a weather shifts the wind instead of replacing it.
 */
public class VenturiaWeathers{
    /** Storm: strong wind. */
    public static Weather gale;
    /** Calm: weak reverse breeze. */
    public static Weather calm;

    public static void load(){
        gale = new Weather("gale"){{
            attrs.set(VenturiaContent.wind, 0.7f);   // base wind + 0.7 = full gale
            duration = 3f * Time.toMinutes;
            sound = Sounds.wind;
            soundVol = 0.6f;
        }};

        calm = new Weather("calm"){{
            attrs.set(VenturiaContent.wind, -0.2f);  // base wind - 0.2 = almost no wind
            duration = 2f * Time.toMinutes;
        }};
    }
}
