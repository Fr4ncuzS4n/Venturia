# AGENTS.md — Improved Erekir Content (IEC)

Рабочий документ для ИИ-агентов и разработчиков, работающих в этом репозитории.
Здесь описано: что это за мод, как он устроен, как собирается, какие соглашения действуют
и какие подводные камни есть в API Mindustry v146.

---

## 1. Что это за проект

**Improved Erekir Content (IEC)** — Java-мод для Mindustry, добавляющий собственный контент:
блоки, юниты и (в плане) целую **третью планету** с собственной фракцией.

Проект в двух словах:

| | |
|---|---|
| Игра | Mindustry |
| Версия игры | **v146** (`minGameVersion: 146`, `mindustryVersion = 'v146'` в `build.gradle`) |
| Язык | Java 8 target (`--release 8`), без jabel — синтаксис Java 17 использовать нельзя |
| Идентификатор мода | `iec` (из `mod.hjson`) |
| Точка входа | `iec.IECMod` |
| Текущее состояние | ~390 строк Java, 4 файла, работающая сборка |

### 1.1. Текущий (первый) этап — «Improved Erekir Content»

Мод расширяет контент Эрекира: производственные блоки, конденсаторы газов, тепловой генератор.
Реализовано 9 блоков, 4 из них подключены к тех-дереву (см. §8 — там список найденных дефектов,
которые ещё не исправлены).

### 1.2. Следующий этап — третья планета

Целевая дизайн-документация нового контента (подробно — §3):

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
./gradlew jar          # -> build/libs/IEC.jar         (десктоп)
./gradlew jarAndroid   # -> build/libs/IECAndroid.jar  (нужны ANDROID_HOME и d8 в PATH)
./gradlew deploy       # -> объединённый jar для дистрибуции
```

Проверка компиляции без запуска игры:

```bash
./gradlew compileJava --offline
```

Артефакт мода (`IEC.jar`) раскладывается в папку `mods/` Mindustry.

### 2.2. Структура репозитория

```
src/iec/
├── IECMod.java                     точка входа (Mod)
├── content/
│   ├── IECBlocks.java              блоки
│   └── IECTechTree.java            ветка тех-дерева
└── world/block/power/
    └── IECHeaterGenerator.java     кастомный блок-генератор

assets/
├── sprites/blocks/<category>/<block-name>/*.png   спрайты блоков (32×32 на тайл)
├── bundles/bundle.properties       EN
└── bundles/bundle_ru.properties    RU

mod.hjson           манифест мода (имя, автор, main-класс, minGameVersion)
build.gradle        сборка (Mindustry/Arc из JitPack + Zelaux-репозитория)
```

### 2.3. Порядок загрузки контента (критично для понимания кода)

`mindustry.core.ContentLoader`:

1. `createBaseContent()` → `Items`, `Blocks`, `Planets`, `SectorPresets`, **`SerpuloTechTree.load()`**, **`ErekirTechTree.load()`**
2. `createModContent()` → `mods.loadContent()` → **наш `IECMod.loadContent()`**
3. `init()` → `Content#init()` у всего контента (здесь вызывается `Block.setBars()`, `Planet.init()`)
4. `load()` → `Content#load()` (загрузка спрайтов/иконок)

Следствия:

* В `loadContent()` **уже существуют** все ванильные блоки и узлы `TechTree.all`.
* Новый контент, созданный в `loadContent()`, автоматически попадает в `content.blocks()`/`content.planets()`
  (регистрация — в конструкторе `Content` → `Vars.content.handleContent(this)`).
* `Attribute.add(...)` можно вызывать только **до** `ContentInitEvent` — то есть внутри `loadContent()`.
* `Planet.init()` (шаг 3) ищет корень тех-дерева планеты: `TechTree.roots.find(n -> n.planet == this)`.
  В Java-модах поле `TechNode.planet` **не заполняется автоматически** — его надо выставить самому
  (в hjson-модах это делает `mindustry.mod.ContentParser`).

---

## 3. Дизайн новой планеты

### 3.1. Планета

Рабочее имя — **Nimbus** (работа с облаками и ветром). Запасные варианты: *Zephyra*, *Cirrus*,
*Ventara*, *Aetheria*. **Имя — временное**, в коде оно должно быть одной константой,
чтобы переименование было дешёвым (см. §6.1).

Реализация — свой класс `Planet` (наследник `mindustry.type.Planet`):

```java
public class IECPlanet extends Planet{
    public IECPlanet(){
        super("nimbus", Planets.sun, 1f, 3);   // radius, sectorSize(3 = hex-сетка)
        generator  = new IECPlanetGenerator(); // свой PlanetGenerator
        meshLoader = () -> new HexMesh(this, 5);
        defaultEnv = Env.terrestrial | Env.oxygen;
        defaultAttributes.set(IECContent.wind, 0.5f);   // базовая сила ветра
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
| `hiddenItems` / `itemWhitelist` | какие предметы доступны на планете |
| `unlockedOnLand` | контент, открываемый при высадке |
| `defaultCore` | блок ядра для запуска с этой планеты |

Секторы: `SectorPreset` (`mindustry.type.SectorPreset`) — по образцу `mindustry.content.SectorPresets`.
У пресета задаются `difficulty`, `captureWave`, `rules` (cons-правила), `overrideLaunchDefaults`,
`allowLaunchSchematics` и т.п.

### 3.2. Ветер — основная энергия (ключевая механика)

**Модель:** ветер — это *атрибут окружения* (`Attribute`), значение которого плавает.
Сила ветра — число (0…1 и выше), знак — направление (отрицательный ветер дует в другую сторону).

**Создание атрибута** — один раз, при загрузке контента:

```java
// mindustry/world/meta/Attribute.java
public static Attribute add(String name)   // регистрирует глобальный атрибут
public static Attribute get(String name)   // БРОСАЕТ исключение, если атрибута нет
public static boolean exists(String name)
```

> ⚠ `Attribute.get("hydrogen")` кидает `IllegalArgumentException`, если атрибут не создан.
> `Attribute.add` при повторной загрузке контента (дев-режим, hot-reload) задвоит атрибут в
> внутреннем массиве `Attribute.all`. Поэтому используем безопасный хелпер (§6.1).

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

**«Переменный» ветер = погода.** Готовый в движке механизм: `Weather` (`mindustry.type.Weather`)
имеет поле `Attributes attrs` — при появлении погоды её атрибуты подмешиваются в окружение
с весом `opacity`. Значит «шторм» и «штиль» — это два объекта `Weather`:

```java
gale = new Weather("gale"){{
    attrs.set(IECContent.wind, 1.0f);   // сильный ветер
    duration = 3f * Time.toMinutes;
    sound = Sounds.wind;
}};
calm = new Weather("calm"){{
    attrs.set(IECContent.wind, 0.1f);   // почти штиль
    duration = 2f * Time.toMinutes;
}};
```

А расписание — `WeatherEntry` в `Rules.weather`:

```java
rules.weather.add(new WeatherEntry(gale, 60f, 120f, 180f, 300f));
//                    (weather, minFrequency, maxFrequency, minDuration, maxDuration)
```

**«Задать перепадки в настройках карты»** — это ровно то, что уже умеет движок:
диалог `mindustry.ui.dialogs.CustomRulesDialog` («Настройки игры/карты») позволяет
добавлять/удалять `WeatherEntry` и менять их длительность, частоту и флаг `always`.
То есть игрок сам настраивает, как часто и насколько сильно дует.

**Как блоки потребляют ветер:**

```java
// внутри Building.update() или в кастомном Block
float wind = state.envAttrs.get(IECContent.wind);   // 0..1+ , знак = направление
```

* ветрогенераторы: мощность ∝ |wind| (при нулевом ветре — 0);
* юниты/планеры: ветер смещает/ускоряет (`rules.dragMultiplier`, кастомная физика);
* визуал: `Weather.drawOver/drawUnder` + `Weather.drawParticles(...)` — рисуем пыль/облака
  по направлению ветра (в движке есть готовые хелперы `drawRain`, `drawNoiseLayers`, `drawParticles`).

### 3.3. Настройки карты и правила сектора

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

### 3.4. Фракция синих

Рабочее название — **Cobalt Compact** (синие). Запасные: *Azure Compact*, *Zephyr Pact*,
*The Azure Gale*. Как и с планетой — временное, держим в одной константе.

Реализация:

```java
// mindustry/game/Team.java
protected Team(int id, String name, Color color)     // конструктор protected!
public static final Team[] all = new Team[256];      // 0..5 — базовые, 7..255 — плейсхолдеры
public static final Team[] baseTeams = new Team[6];
```

> ⚠ Конструктор `Team` — `protected`, поэтому фракция оформляется **наследником**:
> `public class IECBlueTeam extends Team { public IECBlueTeam(){ super(128, "cobalt", Pal.blue); } }`.
> ID 128+ безопасен: слоты 7..255 — генерируемые заглушки `"team#N"`, их можно переиспользовать.

Что нужно от фракции:

* **Команда** — `Team` (цвет, палитра, `items()`, `core()`, `data()`).
* **Данные команды** — `mindustry.game.Teams.TeamData` (`units`, `buildings`, `plans`, `unitCap`).
* **Ядро** — свой `CoreBlock` (наследник `mindustry.world.blocks.storage.CoreBlock.CoreBuild`),
  задаёт стартовые предметы фракции (`Team.items()` → `core().items`).
* **Юниты** — свои `UnitType` (см. §3.6).
* **Блоки** — свои блоки (см. §3.5), уникальные для планеты (обычно `hiddenItems` планеты
  прячет ванильные блоки, а мод добавляет свои).
* **Враги/волны** — `rules.waveTeam = <фракция>`, спавны в `rules.spawns`,
  либо отдельный ИИ (`Teams.TeamData.buildAi`, `rtsAi`).

### 3.5. Свои блоки

Блоки создаются в `iec.content.IECBlocks` (или новом классе по домену) и регистрируются
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

* спрайты: `assets/sprites/blocks/<category>/<block-name>/<block-name>.png` (+ суффиксы `-bottom`,
  `-top`, `-rotator`, `-glow`, … — в точности как в ванилле, имена читаются из `@Load`);
* размер — 32×32 px на тайл, для `size = 2` → 64×64;
* бандл: `block.iec-<name>.name` и `block.iec-<name>.description` (EN + RU).

Подключение к технике: `iec.content.IECTechTree` (см. §3.7).

### 3.6. Свои юниты

```java
public class IECUnits{
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
  бандл: `unit.iec-<name>.name` / `.description`;
* «Порталы»/конструкции (`Payload`) — если нужна доставка юнитов;
* ИИ: `mindustry.ai.types.*` (наследники `UnitAI`), регистрация через `aiType`;
* реакция на ветер — в `updater`/`updateTile` сущности, читать `state.envAttrs.get(wind)`.

### 3.7. Тех-дерево планеты

У новой планеты должно быть **своё** тех-дерево с корнем (root-узлом):

```java
// mindustry/content/TechTree.java
public static TechNode nodeRoot(String name, UnlockableContent content, Runnable children);
```

Корень обязан быть привязан к планете, иначе `Planet.init()` его не найдёт:

```java
TechNode root = TechTree.nodeRoot("nimbus", coreBlock, () -> {
    TechTree.node(windTurbine, () -> { ... });
});
root.planet = nimbusPlanet;   // ← обязательно для Java-модов
```

`TechNode.planet` для остальных узлов наследуется от родителя в UI автоматически
(поиск идёт от корня).

---

## 4. Соглашения

### 4.1. Имена и пакеты

* Пакет модов — `iec.*`; домены: `iec.content` (блоки/юниты/жидкости/планеты),
  `iec.world.block.<domain>` (кастомные блоки), `iec.content.tech` (тех-деревья).
* Имена контента — kebab-case латиницей: `"wind-turbine"`, `"nimbus"`, `"cobalt-core"`.
  Имя мода (`iec`) добавляется к имени контента **автоматически** (`ContentLoader.transformName`),
  поэтому в бандле ключи всегда с префиксом: `block.iec-wind-turbine.name`.
* Java-классы — `IEC`-префиксом (`IECBlocks`, `IECTechTree`, `IECHeaterGenerator`),
  чтобы не конфликтовать с классами других модов.

### 4.2. Стиль кода

* 4 пробела, без табов; `new X(){{ ... }}` (анонимные подклассы) — основной стиль Mindustry.
* Порядок полей в блоке: `requirements` → `consume*` → размер/механика → `drawer` → звуки/эффекты.
* Комментарии — на английском в коде, этот документ — на русском.
* Никаких «магических» литералов без константы: цвета через `Color.valueOf("rrggbb")`,
  числа — именованные константы рядом с классом.

### 4.3. Бандлы

`assets/bundles/bundle.properties` (EN, UTF-8) и `assets/bundles/bundle_ru.properties` (RU):

```properties
block.iec-wind-turbine.name = Wind turbine
block.iec-wind-turbine.description = Converts wind into power. Output scales with wind strength.
planet.iec-nimbus.name = Nimbus
unit.iec-wind-scout.name = Wind scout
```

Обязательно переводить оба файла синхронно; RU-файл читается при `locale=ru`.

### 4.4. Ассеты

* Спрайты блоков — PNG 32×32 на тайл, с прозрачностью, без метаданных.
* Путь: `assets/sprites/blocks/<category>/<name>/<name>[-suffix].png`,
  где `<category>` — `production`, `power`, `crafting`, `units`, `environment`, `effect`.
* Звуки/эффекты — `assets/sounds/`, `assets/sprites/effects/`.
* Новый спрайт → обязательно проверить отсутствие «белого квадрата» в игре (см. §5, п. 9).

---

## 5. Подводные камни Mindustry v146 (проверено по исходникам)

1. **`Block.initBuilding()` рефлексией выбирает buildType.** Из класса блока берётся
   **первый** объявленный inner-класс-наследник `Building`; он становится сущностью блока.
   Добавление второго inner-класса молча меняет тип сущности. Кастомный `Build`-класс
   обязан быть первым и наследовать правильный базовый класс.
2. **`Attribute.get(name)` бросает исключение**, `Attribute.add` дублирует атрибут при повторной
   загрузке → использовать безопасный хелпер (§6.1).
3. **`state.envAttrs` обновляется только в `mindustry.core.Logic.update()`** — это единственное
   место, где атрибуты среды пересчитываются из `rules.attributes` + погоды.
4. **`Planet.applyRules` очищает `rules.attributes` и перезаписывает `rules.env`** —
   индивидуальные значения сектора задавать через `SectorPreset.rules`.
5. **`TechNode.planet` в Java-модах не проставляется автоматически** — делаем руками для корня.
6. **`VariableReactor` (и любой `HeatConsumer`-реактор) рассчитан на расход охлаждающей жидкости.**
   Без `consumeLiquid(...)` механика охлаждения/нестабильности теряет смысл.
7. **`removeBar("...")` убирает только полоску UI**, но не саму механику (нестабильность,
   взрыв и т.п.). Скрывать механику игрока — плохо.
8. **`AttributeCrafter.attribute` должен совпадать** с атрибутом, реально выставленным на тайлах.
   Иначе блок нельзя поставить (`canPlaceOn` требует `minEfficiency` по своему атрибуту).
9. **Отсутствующий спрайт не крашит игру**, но рисуется «белым квадратом» — такие блоки
   нужно проверять визуально. Особенно актуально для блоков с `DrawBlurSpin("-rotator")`,
   `DrawRegion("-bottom")` и `@Load("@-lights")`.
10. **`TechTree.all.find(t -> t.content == parent)`** возвращает первое совпадение;
    если блок есть и в Serpulo-, и в Erekir-дереве — попадёт не тот узел. Всегда проверяй null.
11. **`--release 8` в `build.gradle`** запрещает синтаксис Java 17 (`var`, `switch`-expressions,
    records) без jabel. `build.gradle` также содержит **бессмысленный kotlin-блок** —
    см. §8.
12. **`jarAndroid` использует `configurations.runtimeClasspath`**, а зависимости объявлены
    `compileOnly` → для d8 нужен `compileClasspath`, иначе dexing может падать.

---

## 6. Правила работы для ИИ-агентов

### 6.1. Обязательные константы

Все «специальные» имена и атрибуты — в одном месте, чтобы их можно было переименовать:

```java
public class IECContent{
    /** Имя планеты. Временное — см. §3.1. */
    public static final String planetName = "nimbus";
    /** Название фракции. Временное — см. §3.4. */
    public static final String factionName = "cobalt";

    /** Атрибут окружения «ветер». */
    public static final Attribute wind = attribute("wind");

    /** Безопасное создание атрибута (не дублирует при повторной загрузке). */
    public static Attribute attribute(String name){
        return Attribute.exists(name) ? Attribute.get(name) : Attribute.add(name);
    }
}
```

### 6.2. Правила правок

* Не менять публичные имена блоков/юнитов без миграции: они попадают в сейвы игроков.
* Любой новый блок/юнит → сразу: спрайт, бандл (EN+RU), запись в тех-дереве, `requirements`.
* Перед сдачей: `./gradlew compileJava --offline` (обязательно) + проверка в игре
  (иначе нельзя поймать ошибки рендера и «белых квадратов»).
* Коммит — атомарно, сообщение в стиле репозитория (`Update 0.1V`, `Rebuild ...`, `Change ...`).
* Не оставлять закомментированный код надолго: убрать или оформить TODO с issue-ссылкой.

### 6.3. Порядок регистрации контента

`IECMod.loadContent()` — единственная точка входа:

```java
@Override
public void loadContent(){
    IECContent.load();      // атрибуты, жидкости, предметы
    IECBlocks.load();       // блоки
    IECUnits.load();        // юниты
    IECPlanets.load();      // планета + секторы
    IECTechTree.load();     // тех-деревья (после создания блоков!)
    Log.info("Loading IEC content.");
}
```

Порядок важен: тех-дерево строим **после** блоков и **после** планеты (нужны корни и `planet`).

---

## 7. Чеклист добавления нового контента

- [ ] Блок/юнит создан в своём классе (`iec.content.*`), зарегистрирован в `load()`.
- [ ] `requirements(...)` заданы; `researchCostMultiplier` осмысленный.
- [ ] Потребление/выработка ресурсов сбалансированы (сравнить с ванилльными аналогами).
- [ ] Спрайты на месте (`assets/sprites/blocks/...`), размеры 32×32 на тайл.
- [ ] Бандл: EN **и** RU, ключ `block.iec-<name>.name` + `.description`.
- [ ] Блок подключён к тех-дереву; `BuildVisibility` соответствует задумке
      (в `sandboxOnly`-блок **нельзя** вешать узел тех-дерева).
- [ ] Атрибуты окружения (ветер) читаются корректно; учтён `state.envAttrs` (обновляется в `Logic`).
- [ ] Проверено в игре: рендер, полоски (bars), звук, эффекты, отсутствие белых квадратов.
- [ ] `./gradlew compileJava --offline` — без ошибок.

---

## 8. Текущее состояние и TODO

### 8.1. Найденные дефекты существующего кода (не исправлены)

| # | Файл:строка | Проблема |
|---|---|---|
| 1 | `IECBlocks.java:177, 204` | Конденсаторы используют `attribute = Attribute.steam`, а венты — кастомные атрибуты `hydrogen`/`ozone` → блок нельзя поставить рядом с вентом |
| 2 | `IECBlocks.java:228-233` | `heaterGenerator` без `consumeLiquid` — механика охлаждения/нестабильности не работает |
| 3 | `IECHeaterGenerator.java:20` | `removeBar("instability")` прячет шкалу, но механизм взрыва остаётся |
| 4 | `IECHeaterGenerator.java:31` | `IECHeaterGeneratorBuild` дублирует `HeatConsumer` (уже есть в `VariableReactorBuild`) |
| 5 | `IECBlocks.java:125-173` | `ozonelyzer` помечен `BuildVisibility.sandboxOnly`, но навешан на узел тех-дерева (`IECTechTree.java:37`) |
| 6 | `IECTechTree.java:42` | `TechTree.all.find(...)` без проверки на null → при переименовании ванильного блока узел станет корневым |
| 7 | `IECBlocks.java:4, 14` | Неиспользуемые импорты `arc.struct.Seq`, `Reconstructor` |
| 8 | `IECMod.java:12-18` | Мёртвый код: `BaseDialog` создаётся и не показывается |
| 9 | `IECBlocks.java:235-254` | Закомментированный WIP-рефактор `shipRefabricator` |
| 10 | `build.gradle:100-108` | Бессмысленный `buildscript` с kotlin-плагином (плагин не применён, тестов нет) |
| 11 | `build.gradle:59` | `jarAndroid` берёт `runtimeClasspath` вместо `compileClasspath` (d8) |
| 12 | assets | Нет спрайтов для `hydro-vent`, `ozone-vent`, `hydrogen-condenser`, `ozone-condenser`, `heater-generator` |
| 13 | bundle | Нет записей для `iec-heater-generator`, `iec-hydro-vent`, `iec-ozone-vent` |

### 8.2. Планируемые работы

- [ ] Починить дефекты 1–8 (не блокируют начало работы над планетой).
- [ ] Рефакторинг `IECTechTree` — убрать копипаст API `TechTree.node`, добавить null-проверки.
- [ ] `build.gradle` — привести в порядок (kotlin-блок, d8 classpath, версии).
- [ ] **Новая планета**: `IECPlanets`, генератор, сетка секторов, пресеты, `defaultEnv/defaultAttributes`.
- [ ] **Ветер**: атрибут `wind`, погоды (`gale`/`calm`/...), `Rules.weather` для секторов,
      чтение ветра блоками и юнитами, визуал.
- [ ] **Фракция**: `Team`-наследник, ядро, юниты, блоки, волны/ИИ.
- [ ] Тех-дерево планеты с корнем `root.planet = planet`.

---

## 9. Полезные ссылки на API (v146)

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
| Тех-дерево | `mindustry.content.TechTree`, `mindustry.content.TechTree.TechNode` |
| Загрузка контента | `mindustry.core.ContentLoader`, `mindustry.mod.Mod` |

Исходники вендора для справки: `~/.gradle/caches/modules-2/files-2.1/com.github.Anuken.Mindustry/core/v146/*/core-v146-sources.jar`
(распаковать: `unzip -q core-v146-sources.jar -d /tmp/mdsrc`).
