# AGENTS.md — Venturia (мод для Mindustry)

Рабочий документ для ИИ-агентов и разработчиков, работающих в этом репозитории.
Здесь описано: что это за проект, как он устроен, как собирается, какие соглашения действуют
и какие подводные камни есть в API Mindustry **v160**.

> Все утверждения об API ниже помечены как **проверено** — они сверены с распакованным
> `Mindustry-v160.jar` (см. §10). Остальное — проектные решения и планы.

---

## 1. Что это за проект

**Venturia** — Java-мод для Mindustry, добавляющий **третью планету** с собственной фракцией.
Главная идея: на новой планете **основной источник энергии — ветер**, и он *переменный*
(циклически меняет силу и направление), а величину перепадов игрок задаёт в настройках карты.

Проект в двух словах:

| | |
|---|---|
| Игра | Mindustry |
| Версия игры | **v160** (`mindustryVersion = "v160"` в `build.gradle.kts`, `minGameVersion: 160.3` в `mod.hjson`) |
| Сборка | Gradle 9.8, Kotlin DSL (`build.gradle.kts`), JDK 17 |
| Язык | Java 17 (`sourceCompatibility`/`targetCompatibility` = 17), но см. §6, п. 11 про Android |
| Идентификатор мода | `venturia` |
| Точка входа | `venturia.VenturiaMod` |
| Текущее состояние | объём **A. Skeleton** (M0→M3) реализован: планета-спайк + ветер + ветрогенератор |

### 1.1. Текущий этап — скелет (объём A)

Репозиторий создан из официального шаблона (Anuken/MindustryJavaModTemplate); шаблонные
остатки убраны, инфраструктура сохранена:

* сборка: `jar` (десктоп), `jarAndroid` (dex через d8), `deploy` (универсальный jar);
* CI: `.github/workflows/commitTest.yml` (сборка на каждом push/PR) и `release.yml`
  (артефакт публикуется в GitHub Release);
* точка входа `src/venturia/VenturiaMod.java`, весь контент — в пакете `venturia`.

Уже есть (M0→M3): планета-спайк с генератором, атрибут ветра, погоды `gale`/`calm`,
ветрогенератор, бандлы EN/RU, спрайт ветрогенератора.

Нет: своей фракции и ядра, тех-дерева, юнитов, пресетов секторов, волн врагов.

### 1.2. Целевой этап — планета Venturia

Целевая дизайн-документация нового контента (подробно — §4):

* **Новая планета** с собственным генератором, сеткой секторов, окружением и тех-деревом.
* **Основной источник энергии — воздух (ветер).** Ветер *переменный*: он циклически
  меняет силу и направление. Величину перепадов игрок задаёт **в настройках карты**
  (через погоду — `Weather`/`WeatherEntry`).
* **Собственная компания/фракция синего цвета** со своими юнитами и блоками.
* Своя **жидкость/ресурсы**, свой тех-дерево, свой набор врагов (волны).

---

## 2. Быстрый старт

### 2.1. Сборка

```bash
./gradlew jar          # -> build/libs/VenturiaDesktop.jar   (десктоп)
./gradlew jarAndroid   # -> build/libs/VenturiaAndroid.jar   (нужны ANDROID_HOME и d8 в PATH)
./gradlew deploy       # -> build/libs/Venturia.jar          (desktop+android в одном)
```

Проверка компиляции без запуска игры:

```bash
./gradlew compileJava --offline
```

`--offline` работает, пока зависимость `Anuken:Mindustry:v160` лежит в кэше Gradle
(`~/.gradle/caches/modules-2/files-2.1/Anuken/Mindustry/v160/...`); иначе нужен интернет.

Особенности шаблона (`build.gradle.kts`):

* имя артефактов = `project.name` = имя каталога репозитория → `Venturia*.jar`;
  при клонировании в другой каталог имена поменяются;
* `jar` кладёт в себя `mod.hjson`, `icon.png` (если появится) и **всё** из `assets/`;
* зависимость Mindustry объявлена как `compileOnly` — так и надо (см. README шаблона);
* `jarAndroid` требует `ANDROID_HOME` (или `ANDROID_SDK_ROOT`) и `d8` в `PATH`,
  деселит с `--min-api 21`.

Артефакт мода (`Venturia.jar` / `VenturiaDesktop.jar`) кладётся в папку `mods/` Mindustry.

### 2.2. Структура репозитория (как сейчас)

```
src/venturia/
├── VenturiaMod.java                       точка входа (Mod)
├── content/
│   ├── VenturiaContent.java                константы, атрибут ветра
│   ├── VenturiaWeathers.java               погоды (gale / calm)
│   ├── VenturiaBlocks.java                 блоки
│   ├── VenturiaUnits.java                  юниты
│   ├── VenturiaPlanets.java                планета + секторы
│   └── VenturiaTechTree.java               ветки тех-дерева
└── world/
    ├── planet/
    │   ├── VenturiaPlanet.java             планета Venturia
    │   └── VenturiaPlanetGenerator.java    рельеф + погоды по секторам
    └── block/power/
        └── WindTurbine.java                ветрогенератор

assets/
├── bundles/bundle.properties               EN
├── bundles/bundle_ru.properties            RU
└── sprites/blocks/<category>/<name>/*.png  спрайты блоков (32×32 на тайл)

mod.hjson                         # манифест мода
build.gradle.kts                  # сборка
.github/workflows/commitTest.yml  # CI: сборка на push/PR
.github/workflows/release.yml      # CI: публикация артефакта в Release
gradle/wrapper/                   # Gradle 9.8.0
ROADMAP.md                        # план работ по объёмам
```

### 2.3. Переименование мода — сделано (M0)

Мод переименован: `mod.hjson` (`name: venturia`, `main: venturia.VenturiaMod`), класс
перенесён в `src/venturia/`, «лягушка» и `frog.png` удалены.

```hjson
displayName: "Venturia"
name: "venturia"           # внутреннее имя мода; из него же берётся префикс контента
author: "Fr4ncuz"
main: "venturia.VenturiaMod"
description: "Adds a third planet to Mindustry, where the wind is the main source of power."
version: 0.1.0
minGameVersion: 160.3
java: true
```

> ⚠ Внутреннее имя мода (`name` в `mod.hjson`) — это префикс **всего** контента (§5.3).
> Менять его после выхода первого релиза нельзя: оно попадает в сейвы игроков.

---

## 3. Порядок загрузки контента (критично для понимания кода)

`mindustry.core.ContentLoader` (v160):

1. `createBaseContent()` → `UnitCommand.loadAll` → `TeamEntries.load` → `Items.load` →
   `UnitStance.loadAll` → `StatusEffects.load` → `Liquids.load` → `Bullets.load` →
   `UnitTypes.load` → `Blocks.load` → `Loadouts.load` → `Weathers.load` → **`Planets.load`** →
   **`SectorPresets.load`** → **`SerpuloTechTree.load`** → **`ErekirTechTree.load`**
2. `createModContent()` → `mods.loadContent()` → для каждого мода:
   `Vars.content.setCurrentMod(mod)` → `mod.loadContent()` → `setCurrentMod(null)`
3. `init()` → для всего контента `Content#init()` (здесь `Block.setBars()`, `Planet.init()`)
   → затем событие `ContentInitEvent`
4. `load()` → `Content#load()` (загрузка спрайтов/иконок)

Следствия:

* В `loadContent()` **уже существуют** все ванильные блоки, планеты и узлы `TechTree.all`.
* Новый контент, созданный в `loadContent()`, автоматически попадает в `content.blocks()`/
  `content.planets()` (регистрация — в конструкторе `Content` → `Vars.content.handleContent(this)`).
* `Attribute.add(...)` можно вызывать только **до** `ContentInitEvent` — то есть внутри `loadContent()`.
* **Префикс мода ставится только пока `currentMod` установлен**, т.е. только внутри `loadContent()`.
  Контент, созданный позже (в обработчике события, лениво), останется без префикса →
  сломанные бандлы и спрайты. **Весь контент создаём строго в `loadContent()`.**
* `Planet.init()` (шаг 3) ищет корень тех-дерева планеты:
  `TechTree.roots.find(n -> n.planet == this)` — в Java-модах поле `TechNode.planet`
  **не заполняется автоматически**, его надо выставить самому (в hjson-модах это делает
  `mindustry.mod.ContentParser`).
* Конструктор `Planet(name, parent, radius, sectorSize)` сам добавляет планету в
  `parent.children` и вызывает `parent.updateTotalRadius()` → планета появится
  в интерфейсе выбора планет, если её создать с `parent = Planets.sun` в `loadContent()`.

---

## 4. Дизайн новой планеты

### 4.1. Планета

Рабочее имя — **Venturia** (совпадает с именем репозитория; связано с ветром и облаками).
Запасные варианты: *Nimbus*, *Zephyra*, *Cirrus*, *Aetheria*. **Имя — временное**, в коде оно
должно быть одной константой, чтобы переименование было дешёвым (см. §7.1).

Реализация — свой класс `Planet` (наследник `mindustry.type.Planet`):

```java
public class VenturiaPlanet extends Planet{
    public VenturiaPlanet(){
        super("venturia", Planets.sun, 1f, 3);   // radius, sectorSize(3 = hex-сетка)
        generator  = new VenturiaPlanetGenerator(); // свой PlanetGenerator
        meshLoader = () -> new HexMesh(this, 5);
        defaultEnv = Env.terrestrial | Env.oxygen;
        defaultAttributes.set(VenturiaContent.wind, 0.5f);   // базовая сила ветра
        alwaysUnlocked = true;
        accessible = true;
        ruleSetter = rules -> { ... };
    }
}
```

Что обязательно задать:

| Поле | Зачем |
|---|---|
| `generator` | без него планета не «приземляется», нет секторов |
| `meshLoader` / `cloudMeshLoader` | визуал планеты в интерфейсе (иначе дефолтная сфера) |
| `defaultEnv` | флаги окружения (`Env.terrestrial`, `Env.oxygen`, `Env.spores`, …) |
| `defaultAttributes` | базовые значения атрибутов среды, **в т.ч. ветер** |
| `ruleSetter` | правила сектора: волны, туман, `waveTeam` и т.п. |
| `unlockedOnLand` | контент, открываемый при высадке |
| `defaultCore` | блок ядра для запуска с этой планеты |

> ⚠ **Порядок в `Planet.applyRules(rules)` (проверено по байт-коду v160):**
> 1) `ruleSetter.accept(rules)` → 2) `rules.attributes.clear(); rules.attributes.add(defaultAttributes)`
> → 3) `rules.env = defaultEnv` → 4) `rules.planet = this`.
> Значит `ruleSetter` **не может** менять `rules.env` и `rules.attributes` — их перезапишут сразу
> после. Через `ruleSetter` задают только то, что не трогается дальше: `waveTeam`, `spawns`,
> `dragMultiplier`, `rules.weather` и т.п. Окружение и атрибуты — через `defaultEnv`
> и `defaultAttributes`.

> ⚠ **Пакеты v160 (проверено):** `Env` → `mindustry.world.meta.Env`,
> `Category` → `mindustry.type.Category`, `Sounds` → `mindustry.gen.Sounds`
> (в v146 звуки лежали в `mindustry.content.Sounds`), `Log` → `arc.util.Log`.

Секторы: `SectorPreset` (`mindustry.type.SectorPreset`) — по образцу `mindustry.content.SectorPresets`.
У пресета задаются `difficulty`, `captureWave`, `rules` (cons-правила), `overrideLaunchDefaults`,
`allowLaunchSchematics` и т.п. Привязка пресета к сектору: `planet.preset(index, preset)`
(пресет сам вызывает `initialize(planet, index)`).

> ⚠ **v160 vs v146:** полей `Planet.hiddenItems` и `Planet.itemWhitelist` в v160 **больше нет**
> (в v146 они были). Ограничить предметы планетой теперь можно через
> `UnlockableContent.shownPlanets` (`ObjectSet<Planet>`) и `Item.hidden`
> (см. `Item.isOnPlanet(planet)` — это `shownPlanets.contains(planet) && !hidden`).

### 4.2. Ветер — основная энергия (ключевая механика)

**Модель:** ветер — это *атрибут окружения* (`Attribute`), значение которого плавает.
Сила ветра — число (0…1 и выше), знак — направление (отрицательный ветер дует в другую сторону).

**Создание атрибута** — один раз, при загрузке контента:

```java
// mindustry/world/meta/Attribute.java (v160)
public static Attribute add(String name)      // создаёт НОВЫЙ атрибут, без проверки дубликатов
public static Attribute get(String name)      // БРОСАЕТ IllegalArgumentException, если атрибута нет
public static Attribute getOrNull(String name)// null вместо исключения
public static boolean exists(String name)
```

> ⚠ `Attribute.get("wind")` кидает `IllegalArgumentException`, если атрибут не создан.
> `Attribute.add` при повторной загрузке контента (дев-режим, hot-reload) задвоит атрибут в
> внутреннем массиве `Attribute.all` (проверено по байт-коду: `add` всегда создаёт новый объект
> и перезаписывает `map[name]`). Поэтому используем безопасный хелпер (§7.1).

**Как атрибут попадает в игру:**

```
Rules.attributes            — базовое значение (задаётся Planet.defaultAttributes / applyRules)
        +
Weather.attrs              — модификаторы от погоды, подмешиваются с весом = opacity погоды
        ↓
state.envAttrs              — пересчитывается каждый кадр в mindustry.core.Logic.update()
        ↓
Attribute.env() / state.envAttrs.get(wind)
```

> ⚠ `state.envAttrs` обновляется **только** в `mindustry.core.Logic.update()`
> (это `ApplicationListener`, вызываемый каждый кадр). Вне игрового цикла (тесты, headless без
> `Logic`) значения будут нулевыми/устаревшими.

> ⚠ **Атрибуты погоды СУММИРУЮТСЯ с атрибутами правил, а не заменяют их**
> (проверено по байт-коду `Logic.update()`: `envAttrs.clear(); envAttrs.add(rules.attributes);
> Groups.weather.each(ws -> envAttrs.add(ws.weather.attrs, ws.opacity))`).
> Значит погода — это **дельта** от базового ветра:
> база `0.3` + шторм `+0.7` = `1.0`; база `0.3` + штиль `-0.2` = `0.1`.
> Отрицательная дельта = ветер дует в другую сторону (знак = направление).

**«Переменный» ветер = погода.** Готовый в движке механизм: `Weather` (`mindustry.type.Weather`)
имеет поле `Attributes attrs` — при появлении погоды её атрибуты подмешиваются в окружение
с весом `opacity`. Значит «шторм» и «штиль» — это два объекта `Weather`:

```java
gale = new Weather("gale"){{
    attrs.set(VenturiaContent.wind, 0.7f);   // дельта шторма: база 0.3 + 0.7 = 1.0
    duration = 3f * Time.toMinutes;
    sound = Sounds.wind;                    // mindustry.gen.Sounds (v160)
}};
calm = new Weather("calm"){{
    attrs.set(VenturiaContent.wind, -0.2f);  // дельта штиля: база 0.3 - 0.2 = 0.1
    duration = 2f * Time.toMinutes;
}};
```

А расписание — `WeatherEntry` в `Rules.weather`:

```java
rules.weather.add(new WeatherEntry(gale, 60f, 120f, 180f, 300f));
//                    (weather, minFrequency, maxFrequency, minDuration, maxDuration)
```

У `WeatherEntry` есть ещё поля `cooldown`, `intensity`, `always`.

**«Задать перепадки в настройках карты»** — это ровно то, что уже умеет движок:
диалог `mindustry.ui.dialogs.CustomRulesDialog` («Настройки игры/карты») позволяет
добавлять/удалять `WeatherEntry` и менять их длительность, частоту и флаг `always`.
То есть игрок сам настраивает, как часто и насколько сильно дует.

**Свой генератор — лучшее место для погоды по секторам.** В `PlanetGenerator` (v160) есть хук
`addWeather(Sector, Rules)`: переопределив его, можно задавать свой набор погод
(и, следовательно, свой диапазон ветра) для каждого сектора Venturia.

**Как блоки потребляют ветер:**

```java
// внутри Building.update() или в кастомном Block
float wind = state.envAttrs.get(VenturiaContent.wind);   // 0..1+ , знак = направление
```

* ветрогенераторы: мощность ∝ |wind| (при нулевом ветре — 0);
* юниты/планеры: ветер смещает/ускоряет (`rules.dragMultiplier`, кастомная физика);
* визуал: `Weather.drawOver/drawUnder` + `Weather.drawParticles(...)` — рисуем пыль/облака
  по направлению ветра (в движке есть готовые хелперы `drawRain`, `drawNoiseLayers`, `drawParticles`).

### 4.3. Настройки карты и правила сектора

`mindustry.game.Rules` — это и есть «настройки карты»:

| Поле | Назначение |
|---|---|
| `rules.env` | битовые флаги окружения (`Env.*`) |
| `rules.attributes` | значения атрибутов среды (в т.ч. базовый ветер) |
| `rules.weather` | `Seq<WeatherEntry>` — расписание погоды (ветер) |
| `rules.spawns` | волны врагов |
| `rules.waveTeam` | команда волн (наша фракция-противник) |
| `rules.dragMultiplier` | множитель сопротивления среды (ветер для юнитов) |
| `rules.planet` | текущая планета |

`Planet.applyRules(Rules)` **очищает** `rules.attributes` и перезаписывает `rules.env`
значениями планеты — поэтому индивидуальные значения сектора задаются через
`SectorPreset.rules` (вызывается после `applyRules`) либо через `Planet.ruleSetter`.

### 4.4. Фракция синих

Рабочее название — **Cobalt Compact** (синие). Запасные: *Azure Compact*, *Zephyr Pact*,
*The Azure Gale*. Как и с планетой — временное, держим в одной константе.

Реализация:

```java
// mindustry/game/Team.java (v160)
protected Team(int id, String name, Color color)     // конструктор protected!
public static final Team[] all = new Team[256];       // 0..6 — ванильные, 7..255 — «заглушки»
public static final Team[] baseTeams = new Team[6];   // только 6 базовых команд
```

> ⚠ Конструктор `Team` — `protected`, поэтому фракция оформляется **наследником**:
> `public class CobaltTeam extends Team { public CobaltTeam(){ super(128, "cobalt", Pal.blue); } }`.
> Проверено по байт-коду: конструктор сам делает `all[id] = this`, а при `id < 6` ещё и
> `baseTeams[id] = this`. Ванильные команды занимают id 0..6 (`derelict`, `sharded`, `crux`,
> `malis`, `green`, `blue`, `neoplastic`), id 7..255 заняты сгенерированными «цветными»
> заглушками — их можно переиспользовать, создав свою команду с тем же id.
> ⚠ `baseTeams` — всего 6 ячеек: **свою фракцию нельзя сделать базовой командой**,
> только обычной (`rules.waveTeam`, `rules.team` из списка и т.п.).

Что нужно от фракции:

* **Команда** — `Team` (цвет, палитра, `items()`, `core()`, `data()`).
* **Данные команды** — `mindustry.game.Teams.TeamData` (`units`, `buildings`, `plans`, `unitCap`).
* **Ядро** — свой `CoreBlock` (наследник `mindustry.world.blocks.storage.CoreBlock.CoreBuild`),
  задаёт стартовые предметы фракции (`Team.items()` → `core().items`).
* **Юниты** — свои `UnitType` (см. §4.6).
* **Блоки** — свои блоки (см. §4.5), уникальные для планеты.
* **Враги/волны** — `rules.waveTeam = <фракция>`, спавны в `rules.spawns`,
  либо отдельный ИИ (`Teams.TeamData.buildAi`, `rtsAi`).

### 4.5. Свои блоки

Блоки создаются в `venturia.content.VenturiaBlocks` (или новом классе по домену) и регистрируются
в `content.blocks()` автоматически. Обязательные поля для «нормального» блока:

```java
block = new MyBlock("my-block"){{
    requirements(Category.production, with(Items.copper, 50));
    size = 2;
    consumePower(0.5f);
    // рисуем сами: drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawDefault());
}};
```

Требования к ассетам блока:

* спрайты: `assets/sprites/blocks/<category>/<mod>-<block-name>/<mod>-<block-name>.png`
  (+ суффиксы `-bottom`, `-top`, `-rotator`, `-glow`, … — имена читаются из `@Load`);
  имя региона в атласе = **полное имя контента** (с префиксом мода!), см. §5.4;
* размер — 32×32 px на тайл, для `size = 2` → 64×64;
* бандл: `block.<mod>-<name>.name` и `block.<mod>-<name>.description` (EN + RU).

Подключение к технике: `venturia.content.VenturiaTechTree` (см. §4.7).

**Генератор-электростанция (проверено по байт-коду v160).** Наследуем
`mindustry.world.blocks.power.PowerGenerator`; «Build»-класс — **первый** внутренний класс
наследника `Building` (правило §6.8):

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
            productionEfficiency = enabled ? Mathf.clamp(Math.abs(VenturiaContent.wind.env()), 0f, 1f) : 0f;
        }
    }
}
```

* `GeneratorBuild.getPowerProduction()` = `enabled ? powerProduction * productionEfficiency : 0` —
  поэтому «эффективность» считаем сами в `updateTile()` (как делает ванильный
  `SolarGeneratorBuild` для света);
* `PowerGenerator.setBars()` сам добавляет полоску «power» по `productionEfficiency`
  → полоска ветрогенератора показывает выработку по ветру;
* в v160 у `Block` **нет** поля `hasBars` — не пытаться его выставлять;
* `PowerGenerator` уже ставит `outputsPower = true`, `consumesPower = false`,
  `conductivePower = true` (через `PowerDistributor`) — дублировать не надо.

**Генератор рельефа планеты (проверено по байт-коду v160).** У `TileGen` три поля:
`floor` (пол), `block` (стена), `overlay` (накладка/руда). Полей `wall`/`ore` нет!
`TileGen.reset()` ставит `floor = stone`, `block = air`, `overlay = air` — все три поля
надо задавать в каждом тайле. `Tile.floor` и `Tile.overlay` — типа `Floor`, поэтому
в `overlay` кладут только `Floor`-подклассы (`OreBlock extends OverlayFloor extends Floor`,
`AirBlock extends Floor`), а `Blocks.air` вместо `null` — безопасно.

```java
@Override
public void genTile(Vec3 point, TileGen gen){
    gen.floor = Blocks.sand;
    gen.block = Blocks.sandWall;
    gen.overlay = ore(point) ? Blocks.oreCopper : Blocks.air;
}
```

### 4.6. Свои юниты

```java
public class VenturiaUnits{
    public static UnitType
        windScout, cobaltMech;

    public static void load(){
        windScout = new UnitType("wind-scout"){{
            constructor = entity -> new UnitEntity(entity);
            aiType = FlyingAI.class;            // или GroundAI / etc.
            speed = 3f; health = 120f; size = 10f;
            engine = new FlyingMineEngines();
            weapons.add(new Weapon("wind-cannon"){{ ... }});
            updater = e -> { ... };             // реакция на ветер
        }};
    }
}
```

* Юниты — `mindustry.entities.units.UnitType`, список `content.units()`;
  бандл: `unit.<mod>-<name>.name` / `.description`;
* «Порталы»/конструкции (`Payload`) — если нужна доставка юнитов;
* ИИ: `mindustry.ai.types.*` (наследники `UnitAI`), регистрация через `aiType`;
* реакция на ветер — в `updater`/`updateTile` сущности, читать `state.envAttrs.get(wind)`.

### 4.7. Тех-дерево планеты

У новой планеты должно быть **своё** тех-дерево с корнем (root-узлом):

```java
// mindustry/content/TechTree.java (v160)
public static TechNode nodeRoot(String name, UnlockableContent content, Runnable children);
```

Корень обязан быть привязан к планете, иначе `Planet.init()` его не найдёт:

```java
TechNode root = TechTree.nodeRoot("venturia", coreBlock, () -> {
    TechTree.node(windTurbine, () -> { ... });
});
root.planet = venturiaPlanet;   // ← обязательно для Java-модов
```

`TechNode.planet` для остальных узлов наследуется от родителя в UI автоматически
(поиск идёт от корня).

---

## 5. Соглашения

### 5.1. Имена и пакеты

* Пакет модов — `venturia.*`; домены: `venturia.content` (блоки/юниты/жидкости/планеты),
  `venturia.world.block.<domain>` (кастомные блоки).
* Имена контента — kebab-case латиницей: `"wind-turbine"`, `"venturia"`, `"cobalt-core"`.
  Имя мода добавляется к имени контента **автоматически**
  (`ContentLoader.transformName` → `"<mod>-<name>"`), поэтому в бандле ключи всегда
  с префиксом: `block.venturia-wind-turbine.name`.
* Java-классы — с префиксом проекта (`VenturiaBlocks`, `VenturiaTechTree`, …),
  чтобы не конфликтовать с классами других модов.
* Внутреннее имя мода (`name` в `mod.hjson`) — kebab-case латиницей; оно же префикс контента.

### 5.2. Стиль кода

* 4 пробела, без табов; `new X(){{ ... }}` (анонимные подклассы) — основной стиль Mindustry.
* Порядок полей в блоке: `requirements` → `consume*` → размер/механика → `drawer` → звуки/эффекты.
* Комментарии — на английском в коде, этот документ — на русском.
* Никаких «магических» литералов без константы: цвета через `Color.valueOf("rrggbb")`,
  числа — именованные константы рядом с классом.
* Публичные имена блоков/юнитов не менять без миграции: они попадают в сейвы игроков.

### 5.3. Бандлы

`assets/bundles/bundle.properties` (EN, UTF-8) и `assets/bundles/bundle_ru.properties` (RU):

```properties
block.venturia-wind-turbine.name = Wind turbine
block.venturia-wind-turbine.description = Converts wind into power. Output scales with wind strength.
planet.venturia.name = Venturia
unit.venturia-wind-scout.name = Wind scout
```

Ключ = `<contentType>.<content.name>.<name|description>`, где `content.name` уже содержит
префикс мода. Обязательно переводить оба файла синхронно; RU-файл читается при `locale=ru`.

### 5.4. Ассеты

* Спрайты блоков — PNG 32×32 на тайл, с прозрачностью, без метаданных.
* Путь: `assets/sprites/blocks/<category>/<content-name>/<content-name>[-suffix].png`,
  где `<category>` — `production`, `power`, `crafting`, `units`, `environment`, `effect`,
  а `<content-name>` — **полное имя с префиксом мода** (`venturia-wind-turbine`).
  Имя региона в атласе = имя контента, поэтому `Block.load()` (`Core.atlas.find(name)`)
  ищет именно `venturia-wind-turbine`.
* Звуки/эффекты — `assets/sounds/`, `assets/sprites/effects/`.
* Новый спрайт → обязательно проверить отсутствие «белого квадрата» в игре (см. §6.9).

---

## 6. Подводные камни Mindustry v160

1. **Имя контента автоматически получает префикс мода.** `ContentLoader.transformName(name)`
   возвращает `modName + "-" + name`, а `MappableContent` вызывает его в конструкторе.
   Отсюда — префикс в ключах бандлов и в именах файлов спрайтов. Контент, созданный вне
   `loadContent()`, останется без префикса (§3).
2. **`Attribute.get(name)` бросает исключение**, `Attribute.add` **не проверяет дубликаты**
   (всегда создаёт новый атрибут) → использовать безопасный хелпер (§7.1).
3. **`state.envAttrs` обновляется только в `mindustry.core.Logic.update()`** — это единственное
   место, где атрибуты среды пересчитываются из `rules.attributes` + погоды.
4. **`Planet.applyRules` очищает `rules.attributes` и перезаписывает `rules.env`** —
   индивидуальные значения сектора задавать через `SectorPreset.rules`.
5. **`TechNode.planet` в Java-модах не проставляется автоматически** — делаем руками для корня,
   иначе `Planet.init()` не найдёт тех-дерево планеты.
6. **`Planet.hiddenItems` / `Planet.itemWhitelist` удалены в v160** (в v146 были) →
   используем `UnlockableContent.shownPlanets` и `Item.hidden`.
7. **Команда не может быть «базовой»**: `Team.baseTeams` — массив из 6 элементов,
   ванильные команды занимают id 0..6, id 7..255 — генерируемые заглушки (их можно переиспользовать).
   Конструктор `Team` — `protected`, нужен наследник.
8. **`Block.initBuilding()` рефлексией выбирает buildType.** Из класса блока берётся
   **первый** объявленный inner-класс-наследник `Building`; он становится сущностью блока.
   Добавление второго inner-класса молча меняет тип сущности. Кастомный `Build`-класс
   обязан быть первым и наследовать правильный базовый класс.
9. **Отсутствующий спрайт не крашит игру**, но рисуется «белым квадратом» — такие блоки
   нужно проверять визуально. Особенно актуально для блоков с `DrawBlurSpin("-rotator")`,
   `DrawRegion("-bottom")` и `@Load("@-lights")`.
10. **`TechTree.all.find(t -> t.content == parent)`** возвращает первое совпадение;
     если блок есть и в Serpulo-, и в Erekir-дереве — попадёт не тот узел. Всегда проверяй null.
11. **Android-совместимость.** `jarAndroid` деселит d8 с `--min-api 21` и **без**
    `coreLibraryDesugaring` → API из Java 9+ (`List.of`, `Map.of`, `String.strip`,
    `java.util.stream`, `java.time`) на Android не заработает. Держись API уровня Java 8,
    даже если локально `sourceCompatibility = 17`.
12. **Зависимости Mindustry/Arc — только `compileOnly`** (`implementation` утащит весь движок
    в jar). В шаблоне так и сделано — не «улучшай».
13. **Артефакты называются по имени каталога** (`project.name`): клонировали репозиторий в
    другую папку — получите другие имена jar и другой путь в CI-артефакте.
14. **Пакеты классов в v160 отличаются от привычных** (проверено): `Env` →
    `mindustry.world.meta.Env`, `Category` → `mindustry.type.Category`,
    `Sounds` → `mindustry.gen.Sounds`, `Log` → `arc.util.Log`, `DrawDefault` →
    `mindustry.world.draw.DrawDefault`. Проверять по `javap` перед использованием.
15. **Атрибуты погоды суммируются с атрибутами правил** (§4.2) — погода задаёт *дельту*,
    а не абсолютное значение.
16. **`ruleSetter` не может менять `rules.env`/`rules.attributes`** — `Planet.applyRules`
    перезаписывает их сразу после вызова `ruleSetter` (§4.1).
17. **У `Block` в v160 нет поля `hasBars`**, а `TileGen` не имеет полей `wall`/`ore`
    (есть `floor`, `block`, `overlay`) — см. §4.5.
18. **`Planet.init()` переживает отсутствие тех-дерева**: если корень не найден,
    `techTree` остаётся `null` и блок добавления в базу знаний просто пропускается —
    краша нет, но планета не появится в базе знаний игры.

---

## 7. Правила работы для ИИ-агентов

### 7.1. Обязательные константы

Все «специальные» имена и атрибуты — в одном месте, чтобы их можно было переименовать:

```java
public class VenturiaContent{
    /** Имя планеты. Временное — см. §4.1. */
    public static final String planetName = "venturia";
    /** Название фракции. Временное — см. §4.4. */
    public static final String factionName = "cobalt";

    /** Атрибут окружения «ветер». */
    public static final Attribute wind = attribute("wind");

    /** Безопасное создание атрибута (не дублирует при повторной загрузке). */
    public static Attribute attribute(String name){
        return Attribute.exists(name) ? Attribute.get(name) : Attribute.add(name);
    }
}
```

### 7.2. Порядок регистрации контента

`VenturiaMod.loadContent()` — единственная точка входа:

```java
@Override
public void loadContent(){
    VenturiaContent.load();      // атрибуты, жидкости, предметы
    VenturiaBlocks.load();       // блоки
    VenturiaUnits.load();        // юниты
    VenturiaPlanets.load();      // планета + секторы
    VenturiaTechTree.load();     // тех-деревья (после создания блоков!)
    Log.info("Loading Venturia content.");
}
```

Порядок важен: тех-дерево строим **после** блоков и **после** планеты (нужны корни и `planet`),
и всё это — внутри `loadContent()` (иначе потеряем префикс мода, §3).

### 7.3. Правила правок

* Любой новый блок/юнит → сразу: спрайт, бандл (EN+RU), запись в тех-дереве, `requirements`.
* Перед сдачей: `./gradlew compileJava --offline` (обязательно) + проверка в игре
  (иначе нельзя поймать ошибки рендера и «белых квадратов»).
* Не оставлять закомментированный код надолго: убрать или оформить TODO.
* Не менять `mod.hjson` (`name`, `main`) после первого публичного релиза.

### 7.4. Правила коммитов (для ИИ-агентов)

* ИИ-агент **имеет право** делать коммиты самостоятельно — но **только от имени владельца
  репозитория**: использовать identity из локального `git config`
  (`git config user.name` / `git config user.email`), не выдумывать её и не подменять.
* Запрещено:
  * коммитить от имени бота/агента («OpenCode», «assistant», «AI», «copilot» и т.п.);
  * указывать `--author` с чужим именем или добавлять трейлеры вида
    `Co-authored-by: <бот>` / `Signed-off-by: <бот>`;
  * менять глобальный или локальный `git config` (`user.name`, `user.email`) без просьбы;
  * запускать `push`, `force push`, `reset --hard`, `rebase`, `amend` чужих коммитов и
    переписывать историю без явного указания пользователя.
* Перед коммитом — `git status` и `git diff`: в коммит попадают только осознанные правки.
  Незакоммиченные изменения пользователя не затрагивать, не откатывать и не «поглощать»
  в свой коммит без явного согласия.
* Коммит — атомарно (один смысловой набор изменений), короткое сообщение в стиле репозитория
  (`Add AGENTS.md`, `Initial commit`).
* Публикация релиза (GitHub Release, Mod Browser) — только по явной просьбе пользователя.

---

## 8. Чеклист добавления нового контента

- [ ] Блок/юнит создан в своём классе (`venturia.content.*`), зарегистрирован в `load()`.
- [ ] Создаётся строго внутри `loadContent()` (иначе потеряется префикс мода).
- [ ] `requirements(...)` заданы; `researchCostMultiplier` осмысленный.
- [ ] Потребление/выработка ресурсов сбалансированы (сравнить с ванильными аналогами).
- [ ] Спрайты на месте (`assets/sprites/blocks/...`), имена — **с префиксом мода**, 32×32 на тайл.
- [ ] Бандл: EN **и** RU, ключ `block.venturia-<name>.name` + `.description`.
- [ ] Блок подключён к тех-дереву; `BuildVisibility` соответствует задумке
      (в `sandboxOnly`-блок **нельзя** вешать узел тех-дерева).
- [ ] Атрибуты окружения (ветер) читаются корректно; учтён `state.envAttrs` (обновляется в `Logic`).
- [ ] Проверено в игре: рендер, полоски (bars), звук, эффекты, отсутствие белых квадратов.
- [ ] `./gradlew compileJava --offline` — без ошибок.

---

## 9. Текущее состояние и TODO

### 9.1. Что уже есть

| Файл | Состояние |
|---|---|
| `mod.hjson` | переименован: `name: venturia`, `main: venturia.VenturiaMod`, версия 0.1.0 |
| `src/venturia/VenturiaMod.java` | точка входа; `loadContent()` вызывает все `load()` в правильном порядке |
| `src/venturia/content/*` | `VenturiaContent` (атрибут ветра), `VenturiaWeathers` (gale/calm), `VenturiaBlocks`, `VenturiaUnits`, `VenturiaPlanets`, `VenturiaTechTree` |
| `src/venturia/world/planet/*` | `VenturiaPlanet` + `VenturiaPlanetGenerator` (песок/стены/медь, погоды по секторам) |
| `src/venturia/world/block/power/WindTurbine.java` | ветрогенератор: выработка ∝ силе ветра |
| `assets/bundles/bundle.properties`, `bundle_ru.properties` | бандлы EN/RU (планета, погоды, ветрогенератор) |
| `assets/sprites/blocks/power/venturia-wind-turbine/` | спрайт ветрогенератора (64×64, `size = 2`) |
| `build.gradle.kts` | рабочая сборка (jar / jarAndroid / deploy), Mindustry v160, JDK 17 |
| `.github/workflows/*` | CI: сборка на push/PR + публикация в Release |
| `README.md` | README шаблона (описание сборки и публикации) — обновить под проект |

Своего контента по объёму B (фракция, ядро, юниты, тех-дерево, пресеты секторов, волны) — **нет**.

### 9.2. Планируемые работы

Выбранный объём — **A. Skeleton** (M0→M3) — **реализован** (код есть, проверка в игре — за
владельцем). Детализация и чеклисты — в [ROADMAP.md](ROADMAP.md).

- [x] **Переименовать мод** (§2.3): `mod.hjson`, пакет `venturia`, класс `VenturiaMod`,
      удалены «лягушка» и `frog.png`.
- [x] **Каркас контента**: `VenturiaContent` (атрибут ветра), `VenturiaBlocks`, `VenturiaUnits`,
      `VenturiaPlanets`, `VenturiaTechTree` + порядок вызовов в `loadContent()` (§7.2).
- [x] **Планета-спайк**: генератор, `defaultEnv/defaultAttributes`, `meshLoader`,
      `defaultCore = Blocks.coreShard` (временно).
- [x] **Ветер**: атрибут `wind`, погоды (`gale`/`calm`), `PlanetGenerator.addWeather` для
      секторов, ветрогенератор, читающий `state.envAttrs`.
- [x] **Бандлы** `assets/bundles/bundle.properties` + `bundle_ru.properties`.
- [ ] **Проверка в игре**: планета видна, приземление работает, ветер меняется по погоде,
      спрайты не «белые квадраты».
- [ ] **Фракция**: `Team`-наследник, ядро, юниты, блоки, волны/ИИ (объём B).
- [ ] **Тех-дерево планеты** с корнем `root.planet = planet` (объём B).
- [ ] **Пресеты секторов и волны** (объём C).
- [ ] Обновить `README.md` под проект (сейчас — текст шаблона).

---

## 10. Полезные ссылки на API (v160)

| Что | Класс |
|---|---|
| Планета | `mindustry.type.Planet`, `mindustry.content.Planets` |
| Сектор / пресет | `mindustry.type.Sector`, `mindustry.type.SectorPreset` |
| Генератор планеты | `mindustry.maps.generators.PlanetGenerator` |
| Атрибуты среды | `mindustry.world.meta.Attribute`, `mindustry.world.blocks.Attributes` |
| Погода | `mindustry.type.Weather`, `mindustry.type.Weather.WeatherEntry`, `mindustry.content.Weathers` |
| Правила карты | `mindustry.game.Rules`, `mindustry.ui.dialogs.CustomRulesDialog` |
| Команды/фракции | `mindustry.game.Team`, `mindustry.game.Teams`, `mindustry.game.Teams.TeamData` |
| Юниты | `mindustry.entities.units.UnitType`, `mindustry.ai.types.*` |
| Блоки | `mindustry.world.Block`, `mindustry.world.blocks.*` |
| Ядро | `mindustry.world.blocks.storage.CoreBlock`, `...CoreBlock.CoreBuild` |
| Тех-дерево | `mindustry.content.TechTree`, `mindustry.content.TechTree.TechNode` |
| Загрузка контента | `mindustry.core.ContentLoader`, `mindustry.mod.Mod`, `mindustry.mod.Mods` |
| Состояние игры | `mindustry.core.GameState` (`state.envAttrs`) |
| Электростанция | `mindustry.world.blocks.power.PowerGenerator`, `...PowerGenerator.GeneratorBuild` |
| Рельеф | `mindustry.world.TileGen` (`floor`/`block`/`overlay`), `mindustry.world.Tile` |
| Окружение (флаги) | `mindustry.world.meta.Env` |
| Категории блоков | `mindustry.type.Category` |
| Звуки | `mindustry.gen.Sounds` |
| Лог | `arc.util.Log` |

Как проверить API локально (jar зависимости уже лежит в кэше Gradle):

```bash
mkdir -p /tmp/opencode/md160 && cd /tmp/opencode/md160
unzip -q -o ~/.gradle/caches/modules-2/files-2.1/Anuken/Mindustry/v160/*/Mindustry-v160.jar
javap -classpath . mindustry.type.Planet      # сигнатуры и поля
javap -p -c -classpath . mindustry.game.Team  # байткод конструктора
```

Исходники вендора для справки (для другой версии):
`~/.gradle/caches/modules-2/files-2.1/com.github.Anuken.Mindustry/core/v146/*/core-v146-sources.jar`
(распаковать: `unzip -q core-v146-sources.jar -d /tmp/mdsrc`).
