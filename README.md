# Tharidia: Simple Weight

⚠️ **BETA VERSION - UNDER ACTIVE DEVELOPMENT** ⚠️

This mod is currently in a beta state and is being actively developed. Features may change, and bugs may be present. Use at your own risk in production environments.

---

## About the Tharidia Project

**Tharidia** is an ambitious Minecraft roleplay server project set in a medieval fantasy world. The server features complex, custom mechanics that significantly diverge from vanilla Minecraft gameplay, creating a unique roleplay experience focused on medieval life with fantasy elements.

The Tharidia Project consists of multiple interconnected mods that work together to create this immersive experience. **Simple Weight** is one of these modular components, extracted from the main **Tharidia Things** mod to allow for independent deployment and maintenance alongside other Tharidia mods.

---

## What This Mod Does

**Tharidia: Simple Weight** implements a realistic weight system that affects player movement based on their inventory load. This mechanic adds a layer of realism and strategic inventory management to the Tharidia roleplay experience.

### Core Features

#### 🎒 **Dynamic Weight Calculation**
- Every item in your inventory has a weight value
- Total weight is calculated in real-time as you pick up or drop items
- Weight values are fully configurable via datapacks

#### 🏃 **Movement Speed Penalties**
- Carrying heavy loads slows down your movement speed
- The more weight you carry, the slower you move
- Speed reduction is gradual and proportional to your total weight
- Uses Minecraft's attribute system for smooth, compatible speed modifications

#### 🎒 **Container Contents**
- Items stored inside carried containers count too: backpacks exposing an item handler (Sophisticated Backpacks, L2 Backpack...), shulker boxes, bundles and Supplementaries quivers, also when worn in Curios/Accessories slots
- Packed Simply Tents tents count the blocks, chest contents and entities packed inside them
- Blocks (with their contents) and entities carried with Carry On count as well
- All these integrations are optional: none of these mods is required

#### 🏊 **Swimming Restrictions**
- When overencumbered, you cannot swim upward in water: you sink to the bottom and have to walk out (wading in shallow water and climbing out onto the bank still works)
- Simulates the difficulty of swimming with heavy equipment
- Adds strategic considerations for water crossings and exploration

#### ⚙️ **Datapack Configuration**
- Define custom weight values for any item via JSON datapacks
- Server administrators can fine-tune the weight system to their needs
- Default weight values provided for common items (ores, blocks, tools, etc.)

#### 🔍 **`/weight` Command (OP)**
- `/weight check [player]` shows a player's carried weight and status
- `/weight testmode` makes the weight system apply to yourself even as OP

#### 👑 **Master/OP Bypass**
- Players with operator permissions automatically bypass the weight system
- Useful for server administration and testing
- Can be easily modified in the code if different behavior is desired

#### 🚀 **Performance Optimized**
- Player batching system distributes processing load across ticks
- Designed to handle large servers with many players
- Minimal performance impact even on busy servers

---

## Installation

### Requirements
- **Minecraft**: 1.21.1
- **NeoForge**: 21.1.215 or higher
- **Java**: 21 (shipped with Minecraft 1.21.1)

### Steps

1. Install NeoForge 21.1.215+ for Minecraft 1.21.1
2. Download the latest release of Tharidia: Simple Weight
3. Place the JAR file in your `mods` folder
4. Launch the game
5. (Optional) Add custom weight datapacks to configure item weights

---

## Configuration

### Default Weight Values

The mod comes with default weight values for common items:

- **Heavy Items** (10.0 weight): Anvils, Netherite blocks
- **Medium-Heavy Items** (5.0 weight): Iron blocks, Gold blocks, Diamond blocks
- **Medium Items** (2.0 weight): Iron ingots, Gold ingots
- **Light Items** (0.5 weight): Diamonds, Emeralds
- **Very Light Items** (0.1 weight): Most other items

### Custom Datapack Configuration

You can create custom weight configurations using datapacks. Place JSON files in:

```
datapacks/your_datapack/data/tharidia_simpleweight/weight_config/example.json
```

Example configuration (see `example_datapack/` for a complete one):

```json
{
  "item_weights": {
    "minecraft:iron_ingot": 2.0,
    "minecraft:gold_ingot": 2.5,
    "minecraft:diamond": 0.5,
    "#minecraft:logs": 1.5,
    "#c:ingots": 2.0,
    "#minecraft:mineable/pickaxe": 1.0
  },
  "default_item_weight": 0.1,
  "entity_weights": {
    "minecraft:pig": 40.0
  },
  "default_entity_weight": 50.0,
  "display": {
    "unit": "kg",
    "decimals": 2
  },
  "thresholds": {
    "light": 100.0,
    "medium": 200.0,
    "heavy": 300.0,
    "overencumbered": 400.0
  },
  "debuffs": {
    "light_speed_multiplier": 0.95,
    "medium_speed_multiplier": 0.85,
    "heavy_speed_multiplier": 0.70,
    "overencumbered_speed_multiplier": 0.50,
    "heavy_disable_swim_up": true,
    "overencumbered_disable_swim_up": true,
    "heavy_disable_jump": false,
    "overencumbered_disable_jump": false
  },
  "weight_multiplier": 1.0,
  "smooth_transition": true,
  "hud": {
    "position": "bottom_left",
    "x_offset": 0,
    "y_offset": 0
  }
}
```

Notes:
- `item_weights` keys starting with `#` are item tags and are expanded to all items in the tag (paths with `/` such as `#c:ingots/iron` work). If no item tag with that id exists, a block tag is used instead (e.g. `#minecraft:mineable/pickaxe`), applied to the items of those blocks. Specific item entries always override tag entries. Unknown tags are logged as warnings in the server log.
- `default_item_weight` (optional, default `1.0`) is the weight of every item not listed in `item_weights`.
- `entity_weights` / `default_entity_weight` (optional, default `0`) set the weight of entities carried with Carry On or packed inside a Simply Tents tent. Keys can be entity ids or `#` entity type tags.
- `display.unit` (optional, default empty) is appended to weights in tooltips and `/weight check`, e.g. `kg`; `display.decimals` (optional, 0-4, default `1`) is the number of decimal places shown in tooltips, the HUD and `/weight check`.
- `hud.position` accepts `bottom_left`, `bottom_right`, `top_left`, `top_right`; `x_offset`/`y_offset` push the indicator inward from the chosen corner (useful to avoid overlapping JEI or other HUD mods).
- `heavy_disable_jump` / `overencumbered_disable_jump` (optional, default `false`) also block jumping on land, for packs with auto-jump/step-up mods.

---

## Building from Source

### Prerequisites
- Java Development Kit (JDK) 21
- Git

### Build Steps

```bash
# Clone the repository
git clone <repository-url>
cd Simple-Weight

# Build the mod
./gradlew build

# The compiled JAR will be in build/libs/
```

### Development Environment

```bash
# Run the client for testing
./gradlew runClient

# Run a dedicated server
./gradlew runServer

# Generate data files
./gradlew runData
```

---

## Technical Details

### Package Structure

```
com.THproject.tharidia_simpleweight/
├── TharidiaSimpleWeight.java       # Main mod class
├── TharidiaSimpleWeightClient.java # Client-side initialization
├── event/
│   └── WeightDebuffHandler.java    # Event handler for weight effects
├── weight/
│   ├── WeightData.java             # Weight data model
│   ├── WeightDataLoader.java       # Datapack loader
│   ├── WeightManager.java          # Weight calculation logic
│   ├── WeightRegistry.java         # Weight data registry
│   └── ContainerWeights.java       # Contents of containers, packed tents, Carry On
├── mixin/
│   └── LivingEntityMixin.java      # Blocks swimming up when overencumbered
├── command/
│   └── WeightCommand.java          # /weight check, /weight testmode
├── network/                        # Config and test mode sync to clients
└── client/
    ├── WeightHudOverlay.java       # HUD indicator
    └── WeightTooltipHandler.java   # Item tooltips
```

### How It Works

1. **Data Loading**: On world load, the mod reads weight configurations from datapacks
2. **Player Tick**: Every second (20 ticks), the mod calculates each player's total inventory weight
3. **Attribute Modification**: Based on weight, a speed modifier is applied to the player's movement speed attribute
4. **Swimming Check**: Before a player moves in water, any upward speed is removed when swimming up is disabled for their weight status (on both client and server)
5. **Batching**: Players are processed in staggered batches to distribute server load

### Performance Considerations

- Weight calculations occur only once per second per player
- Player batching ensures only 1/5th of players are processed each second
- Swimming checks reuse a weight status cached for one second
- The HUD recalculates the weight a few times per second, not every frame
- Uses efficient attribute modifiers instead of continuous velocity manipulation

---

## Compatibility

### Designed for Tharidia Ecosystem

This mod is part of the Tharidia Project and is designed to work alongside:
- **Tharidia Things** (main mod)
- **Tharidia Tweaks**
- **Tharidia Features**
- Other Tharidia modular components

### General Compatibility

While designed for Tharidia, this mod should be compatible with most other mods that don't heavily modify player movement or attributes.

**Known Compatible:**
- Most content mods
- Most quality-of-life mods
- Datapack-based mods

**Potential Conflicts:**
- Mods that heavily modify player movement speed
- Mods that override player swimming mechanics
- Other weight/encumbrance systems

---

## Roadmap

### Planned Features (Future Versions)

- [ ] Configurable weight thresholds for different penalty tiers
- [ ] Visual indicators for current weight status
- [ ] Sound effects when becoming overencumbered
- [ ] Integration with armor weight
- [ ] Configurable master bypass behavior
- [ ] More granular datapack configuration options

### Known Issues

- None currently reported (alpha testing in progress)

---

## Support & Contributing

### Reporting Issues

If you encounter bugs or have feature requests, please report them through the appropriate channels for the Tharidia Project.

### Contributing

This mod is part of the Tharidia Project. Contributions should align with the project's goals and medieval fantasy roleplay theme.

---

## Credits

**Development Team**: THproject Team

**Original Source**: Extracted from Tharidia Things mod

**Special Thanks**: The Tharidia community for testing and feedback

---

## License

All Rights Reserved

This mod is part of the Tharidia Project. Please respect the project's licensing terms.

---

## Version History

### 0.5.0-beta (Current)
- Overencumbered players can no longer swim up (they used to keep rising slowly and could stay afloat at the surface)
- Configurable display unit and decimal places (`display`)
- Configurable weight for unlisted items (`default_item_weight`)
- Block tags (e.g. `#minecraft:mineable/pickaxe`) can be used in `item_weights`
- Contents of shulker boxes, bundles and Supplementaries quivers are counted, also in Curios slots
- Simply Tents packed tents and Carry On loads are counted (`entity_weights` for entities)
- `/weight check [player]`; tooltips show stack totals and contents weight
- Removed unused bundled libraries (MySQL, HikariCP, JLayer, VLCJ) from the jar

### 0.1.0-alpha
- Initial alpha release
- Core weight system functionality
- Datapack configuration support
- Performance optimizations for large servers
- Extracted from Tharidia Things for modular deployment
