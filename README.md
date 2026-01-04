# Tharidia: Simple Weight

⚠️ **ALPHA VERSION - UNDER ACTIVE DEVELOPMENT** ⚠️

This mod is currently in an alpha state and is being actively developed. Features may change, and bugs may be present. Use at your own risk in production environments.

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

#### 🏊 **Swimming Restrictions**
- When overencumbered, you cannot swim upward in water
- Simulates the difficulty of swimming with heavy equipment
- Adds strategic considerations for water crossings and exploration

#### ⚙️ **Datapack Configuration**
- Define custom weight values for any item via JSON datapacks
- Server administrators can fine-tune the weight system to their needs
- Default weight values provided for common items (ores, blocks, tools, etc.)

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
datapacks/your_datapack/data/tharidia_simpleweight/weight/items.json
```

Example configuration:

```json
{
  "weights": {
    "minecraft:iron_ingot": 2.0,
    "minecraft:gold_ingot": 2.5,
    "minecraft:diamond": 0.5,
    "minecraft:netherite_ingot": 3.0
  }
}
```

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
│   └── WeightRegistry.java         # Weight data registry
└── client/
    └── ClientModEvents.java        # Client event handlers
```

### How It Works

1. **Data Loading**: On world load, the mod reads weight configurations from datapacks
2. **Player Tick**: Every second (20 ticks), the mod calculates each player's total inventory weight
3. **Attribute Modification**: Based on weight, a speed modifier is applied to the player's movement speed attribute
4. **Swimming Check**: Every 5 ticks, the mod checks if overencumbered players are trying to swim upward and prevents it
5. **Batching**: Players are processed in staggered batches to distribute server load

### Performance Considerations

- Weight calculations occur only once per second per player
- Player batching ensures only 1/5th of players are processed each second
- Swimming checks are performed every 5 ticks instead of every tick
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

### 0.1.0-alpha (Current)
- Initial alpha release
- Core weight system functionality
- Datapack configuration support
- Performance optimizations for large servers
- Extracted from Tharidia Things for modular deployment
