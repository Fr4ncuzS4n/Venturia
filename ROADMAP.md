# ROADMAP.md — Venturia

План разработки по выбранному объёму **A. Skeleton** (этапы **M0 → M3**).
Все API-факты, соглашения и подводные камни — в [AGENTS.md](AGENTS.md).

---

## 0. Объём и границы

**Входит в объём A:**

| Этап | Результат |
|---|---|
| M0 | мод переименован, точка входа и бандлы на месте |
| M1 | каркас контента (`VenturiaContent/Blocks/Units/Planets/TechTree`) |
| M2 | планета Venturia видна в игре, сектор генерируется, приземление работает |
| M3 | ветер: атрибут, погоды (шторм/штиль), ветрогенератор, разброс по секторам |

**Не входит (отложено до объёмов B и C):** своя фракция и ядро, тех-дерево, юниты,
пресеты секторов и волны врагов, полировка и публикация в Mod Browser.

**Цоль этапа (Definition of Done, коротко):** в игре есть планета Venturia, на неё можно
приземлиться, и на ней работает ветер — ветрогенератор вырабатывает мощность, пропорциональную
силе ветра, а ветер меняется по погоде (шторм/штиль) и отличается от сектора к сектору.

**Граф этапов:** `M0 → M1 → M2 → M3`. M2 — рискованный (его делаем «spike'ом», минимальным кодом),
поэтому порядок менять нельзя: без работающей планеты остальное не проверить.

---

## 1. M0 — Переименование мода

**Зачем:** развести шаблон и проект; задать внутреннее имя мода (`venturia`), которое с этого
момента становится префиксом **всего** контента и ключей бандлов.

### Задачи

> **Текущее состояние рабочего дерева:** класс уже переименован в `VenturiaMod`
> (`src/example/VenturiaMod.java`, пакет пока `example`), в `mod.hjson` обновлён `main`.
> Осталось доделать шаги ниже.

- [x] `mod.hjson` → новые значения (ниже): важно поменять `name` — это префикс контента.
- [x] Перенести класс в `src/venturia/VenturiaMod.java` с пакетом `venturia`
      (или оставить пакет `example` — тогда поправить пути в AGENTS.md и этом файле).
- [x] Убрать из класса диалог с «лягушкой» и `Events.on(ClientLoadEvent.class, …)`.
- [x] Удалить `assets/sprites/frog.png`.
- [x] Создать `assets/bundles/bundle.properties` (EN) и `assets/bundles/bundle_ru.properties` (RU)
      — пока с одним тестовым ключом, чтобы проверить, что бандлы подхватываются.
- [ ] (Опционально) `icon.png` в корень репозитория — `jar` включает его в сборку.

### Файлы

```
mod.hjson                                  # переписать
src/venturia/VenturiaMod.java             # новый (из src/example/VenturiaMod.java)
src/example/VenturiaMod.java              # удалить после переноса
assets/sprites/frog.png                   # удалить
assets/bundles/bundle.properties          # новый
assets/bundles/bundle_ru.properties       # новый
```

### Код

`mod.hjson`:

```hjson
displayName: "Venturia"
name: "venturia"
author: "Fr4ncuzS4n"
main: "venturia.VenturiaMod"
description: "Adds a third planet to Mindustry, powered by wind."
version: 0.1.0
minGameVersion: 160.3
java: true
```

`src/venturia/VenturiaMod.java`:

```java
package venturia;

import arc.*;
import mindustry.mod.*;

public class VenturiaMod extends Mod{

    public VenturiaMod(){
        Log.info("[Venturia] constructor loaded.");
    }

    @Override
    public void loadContent(){
        Log.info("[Venturia] loading content.");
    }
}
```

`assets/bundles/bundle.properties`:

```properties
planet.venturia.name = Venturia
planet.venturia.description = A third planet where the wind is the main source of power.
```

### Проверка

- [x] `./gradlew jar --offline` → `build/libs/VenturiaDesktop.jar` собирается без ошибок.
- [ ] Мод появляется в списке модов игры под именем «Venturia».
- [ ] В логе видно `[Venturia] constructor loaded.` и `[Venturia] loading content.`
- [ ] В бандле читается ключ (например, в окне описания планеты — на этом этапе достаточно
      того, что ключ не «сырой»).

### Риски

- `name` в `mod.hjson` — необратимый после релиза: попадает в имена контента и в сейвы.
  Переименовывать только до первого публичного релиза.

---

## 2. M1 — Каркас контента

**Зачем:** завести единые классы и порядок регистрации, чтобы дальше только наполнять их.

### Задачи

- [x] `VenturiaContent` — константы (имя планеты, имя фракции) + безопасное создание атрибута ветра.
- [x] `VenturiaBlocks`, `VenturiaUnits`, `VenturiaPlanets`, `VenturiaTechTree` — `load()` с логом.
- [x] `VenturiaMod.loadContent()` — вызовы в правильном порядке (см. ниже).

### Файлы

```
src/venturia/content/VenturiaContent.java
src/venturia/content/VenturiaBlocks.java
src/venturia/content/VenturiaUnits.java
src/venturia/content/VenturiaPlanets.java
src/venturia/content/VenturiaTechTree.java
```

### Код

`VenturiaContent.java`:

```java
package venturia.content;

import mindustry.world.meta.*;

/** Общие константы и атрибуты мода. Здесь же — единственное место для переименований. */
public class VenturiaContent{
    /** Имя планеты. Временное — см. AGENTS.md §4.1. */
    public static final String planetName = "venturia";
    /** Название фракции. Временное — см. AGENTS.md §4.4. */
    public static final String factionName = "cobalt";

    /** Атрибут окружения «ветер». */
    public static Attribute wind = attribute("wind");

    /** Безопасное создание атрибута: не дублирует при повторной загрузке контента. */
    public static Attribute attribute(String name){
        return Attribute.exists(name) ? Attribute.get(name) : Attribute.add(name);
    }

    public static void load(){
        Log.info("[Venturia] attributes: wind = " + wind.id);
    }
}
```

`VenturiaMod.loadContent()`:

```java
@Override
public void loadContent(){
    VenturiaContent.load();      // атрибуты, жидкости, предметы
    VenturiaBlocks.load();       // блоки
    VenturiaUnits.load();        // юниты
    VenturiaPlanets.load();      // планета + секторы
    VenturiaTechTree.load();     // тех-деревья (после создания блоков!)
    Log.info("[Venturia] content loaded.");
}
```

> Весь контент создаётся **только** здесь: префикс мода ставится лишь пока
> `Vars.content.setCurrentMod(mod)` активен (AGENTS.md §3).

### Проверка

- [x] `./gradlew compileJava --offline` — без ошибок.
- [ ] В логе: `[Venturia] attributes: wind = 0` (id 0 — атрибут создан первым в моде).
- [ ] В логе видно префикс: любой созданный контент должен называться `venturia-…`
      (на этом этапе можно временно создать тестовый блок и посмотреть его `name` в логе).

### Риски

- Атрибут создаётся в `loadContent()` — если забыть, `Attribute.get("wind")` упадёт
  с `IllegalArgumentException` при первом обращении (§6.2 AGENTS.md).

---

## 3. M2 — Spike: минимальная планета

**Зачем:** проверить главный риск проекта — цепляется ли модальная планета к игре
(UI планет, генерация сектора, приземление). Код намеренно минимальный, без красоты.

### Задачи

- [x] `VenturiaPlanet extends Planet` — создаётся с `parent = Planets.sun`.
- [x] `VenturiaPlanetGenerator extends mindustry.maps.generators.PlanetGenerator` — минимальный рельеф.
- [x] `meshLoader` / `cloudMeshLoader` → `new HexMesh(this, 5)`.
- [x] `defaultEnv`, `defaultAttributes` (базовый ветер), `alwaysUnlocked`, `accessible`.
- [x] `defaultCore = Blocks.coreShard` — временно, чтобы было куда приземляться.
- [x] `ruleSetter` не нужен: `Planet.applyRules` перезаписывает `env`/`attributes` после него (AGENTS.md §4.1).

### Файлы

```
src/venturia/content/VenturiaPlanets.java      // создание планеты + секторов
src/venturia/world/planet/VenturiaPlanetGenerator.java
```

### Код

`VenturiaPlanet`:

```java
package venturia.world.planet;

import mindustry.content.*;
import mindustry.graphics.g3d.*;
import mindustry.type.*;
import mindustry.world.*;
import venturia.content.*;

public class VenturiaPlanet extends Planet{
    public VenturiaPlanet(){
        super(VenturiaContent.planetName, Planets.sun, 1f, 3); // radius, sectorSize(3 = hex-сетка)

        generator = new VenturiaPlanetGenerator(this);
        meshLoader = () -> new HexMesh(this, 5);
        cloudMeshLoader = () -> new HexMesh(this, 5);

        defaultEnv = Env.terrestrial | Env.oxygen;          // mindustry.world.meta.Env
        defaultAttributes.set(VenturiaContent.wind, 0.3f);   // базовая сила ветра

        alwaysUnlocked = true;
        accessible = true;
        defaultCore = Blocks.coreShard;                       // временно, до своей фракции

        ruleSetter = rules -> {
            rules.env |= Env.terrestrial | Env.oxygen;
        };
    }
}
```

`VenturiaPlanetGenerator` (минимальный вариант — сплошной рельеф):

```java
package venturia.world.planet;

import arc.math.geom.*;
import mindustry.maps.generators.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.content.*;

public class VenturiaPlanetGenerator extends PlanetGenerator{
    public final VenturiaPlanet planet;

    public VenturiaPlanetGenerator(VenturiaPlanet planet){
        this.planet = planet;
    }

    @Override
    public void genTile(Vec3 point, TileGen gen){
        gen.floor = Blocks.sand;
        gen.block = Blocks.sandWall;                          // стена
        gen.overlay = ore(point) ? Blocks.oreCopper : Blocks.air; // руда-накладка
    }
}
```

> Базовый `PlanetGenerator.generate(Tiles, Sector, WorldParams)` сам обходит прямоугольник сектора
> и вызывает `genTile(Vec3, TileGen)` для каждого тайла — достаточно переопределить `genTile`.
> Заготовки для «красивого» рельефа есть в `mindustry.maps.planet.SerpuloPlanetGenerator`
> и `ErekirPlanetGenerator` (плюс хелперы `BasicGenerator`: `terrain`, `noise`, `ore`, `cliffs`).

### Проверка

- [ ] Планета **видна в интерфейсе выбора планет** (иконка/кружок рядом с Солнцем).
- [ ] По клику открывается вид планеты, сетка секторов рендерится без ошибок.
- [ ] На сектор можно приземлиться: мир грузится, есть ядро (`core-shard`), можно поставить блоки.
- [ ] В логе нет исключений; `Planet.init()` нашёл тех-дерево (пока пустое) без NPE.
- [ ] Проверить, что планета действительно добавилась в `Planets.sun.children`
      (конструктор `Planet` делает это сам — AGENTS.md §3).

### Риски

| Риск | Что делать |
|---|---|
| Планета не видна в UI | проверить, что создание планеты происходит **внутри `loadContent()`** и `parent = Planets.sun`; проверить `visible`/`accessible` |
| Сектор не генерируется | переопределить `generate(Tiles, Sector, WorldParams)` напрямую и заполнить тайлы вручную — это надёжнее, чем `genTile` |
| Не приземлиться | временно поставить `defaultCore = Blocks.coreShard` и `allowLaunchSchematics = true` |
| Краш в `Planet.init()` | в `Planet.init()` ищется `TechTree.roots.find(n -> n.planet == this)` — без корня дерева это `null`, но не должно падать; проверить стек трейс |

---

## 4. M3 — Ветер

**Зачем:** реализовать главную механику — энергия, зависящая от переменного ветра.

### Задачи

- [x] Погоды `gale` (шторм) и `calm` (штиль) — объекты `mindustry.type.Weather` с `attrs`.
- [x] Расписание погод: `Rules.weather` + `PlanetGenerator.addWeather(Sector, Rules)` —
      разный набор погод для разных секторов.
- [x] Блок `wind-turbine`: мощность ∝ \|wind\|, полоска выработки, `requirements`, спрайт.
- [x] Чтение ветра: `state.envAttrs.get(VenturiaContent.wind)` (через `Attribute.env()`).
- [ ] Минимальный визуал ветра (по желанию — частицы/полосы по направлению ветра).

### Файлы

```
src/venturia/content/VenturiaWeathers.java       # погоды
src/venturia/content/VenturiaBlocks.java         # ветрогенератор
src/venturia/world/block/power/WindTurbine.java  # кастомный блок
assets/sprites/blocks/power/venturia-wind-turbine/venturia-wind-turbine.png
```

### Код

Погоды:

```java
package venturia.content;

import arc.util.*;
import mindustry.audio.*;
import mindustry.type.*;
import mindustry.content.*;

public class VenturiaWeathers{
    public static Weather gale, calm;

    public static void load(){
        gale = new Weather("gale"){{
            attrs.set(VenturiaContent.wind, 0.7f);   // дельта: база 0.3 + 0.7 = 1.0
            duration = 3f * Time.toMinutes;
            sound = Sounds.wind;                    // mindustry.gen.Sounds
        }};
        calm = new Weather("calm"){{
            attrs.set(VenturiaContent.wind, -0.2f);  // дельта: база 0.3 - 0.2 = 0.1
            duration = 2f * Time.toMinutes;
        }};
    }
}
```

Блок-ветрогенератор (идея):

```java
public class WindTurbine extends PowerGenerator{
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
            // выработка = powerProduction * productionEfficiency
            productionEfficiency = enabled ? Mathf.clamp(Math.abs(VenturiaContent.wind.env()), 0f, 1f) : 0f;
        }
    }
}
```

Погода по секторам — переопределение хука генератора:

```java
@Override
public void addWeather(Sector sector, Rules rules){
    super.addWeather(sector, rules); // ванильная погода по типу рельефа

    // каждый второй сектор ветренее
    boolean windy = sector.id % 2 == 0;

    rules.weather.add(new WeatherEntry(VenturiaWeathers.gale,
        windy ? 45f : 90f, windy ? 90f : 180f, 150f, 300f));
    rules.weather.add(new WeatherEntry(VenturiaWeathers.calm,
        windy ? 150f : 60f, windy ? 240f : 120f, 120f, 240f));
}
```

> ⚠ `state.envAttrs` обновляется только в `mindustry.core.Logic.update()` — вне игрового
> цикла значение ветра будет нулевым (AGENTS.md §4.2).
> ⚠ Спрайт блока обязан называться **с префиксом мода**: `venturia-wind-turbine.png`,
> иначе — «белый квадрат» (AGENTS.md §5.4).

### Проверка

- [ ] В `CustomRulesDialog` видны погоды Venturia и их можно включать/выключать.
- [ ] При шторме выработка ветрогенератора заметно выше, чем в штиль; полоска отражает состояние.
- [ ] В разных секторах набор погод различается (в логе/в UI погоды сектора).
- [ ] Спрайты блока видны (не белые квадраты), у блока есть иконка в панели.
- [ ] Блок корректно ведёт себя при нулевом ветре (мощность 0, без ошибок).

### Риски

| Риск | Что делать |
|---|---|
| Мощность всегда 0 | читать `state.envAttrs`, а не `rules.attributes`; проверять, что игра в состоянии `playing` |
| Погода не появляется | `WeatherEntry` требует ненулевых частот/длительностей; проверить, что `rules.weather` не перетирается `Planet.applyRules` после наших правок |
| Белый квадрат | имя файла спрайта = полное имя контента (`venturia-wind-turbine.png`) |
| Android | не использовать `java.util.stream`, `List.of`, `String.strip` и прочие API Java 9+ |

---

## 5. Definition of Done (объём A)

- [x] `./gradlew jar --offline` и `./gradlew compileJava --offline` — без ошибок.
- [x] Мод грузится, в логе видны все этапы `[Venturia] ...` (проверено headless-проверкой).
- [ ] Планета Venturia видна в интерфейсе, сектор генерируется, приземление работает.
- [ ] Ветер меняется по погоде, ветрогенератор вырабатывает мощность ∝ силе ветра.
- [x] Бандлы EN и RU существуют, ключи читаются (хотя бы для планеты и одного блока).
- [ ] Никаких «белых квадратов» в игре (нужна проверка в игре).

---

## 6. Отложено (объёмы B и C)

Следующим шагом, по объёму B: своя фракция (`CobaltTeam extends Team`, id 128) и ядро
(`CobaltCore extends CoreBlock`) вместо `core-shard`; тех-дерево (`TechTree.nodeRoot` +
`root.planet = planet`); юниты; затем, по объёму C: пресеты секторов, волны врагов,
полировка спрайтов и бандлов, README и публикация 0.1.0 в Mod Browser.

---

## 7. Журнал

| Дата | Этап | Что сделано | Заметки |
|---|---|---|---|
| 2026-10-22 | M0–M3 (A) | Переименование мода, каркас контента, планета Venturia + генератор, ветер (gale/calm), ветрогенератор, бандлы EN/RU, спрайт | Скелет реализован. Пакет `venturia`. Контент регистрируется в `loadContent()`. Сборка проходит. |
