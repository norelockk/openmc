# OpenMC

A modular Minecraft server plugin system.

## Project Structure

This project consists of two main components:

1. **OpenMCCore** - The core plugin that provides player data management, module system, and API
2. **OpenMCAuth** - An authentication plugin that depends on the core plugin

## Building

To build both plugins, run:

```bash
./gradlew coreJar authJar
```

Or use the IntelliJ IDEA run configuration "Build Core and Auth".

The built JARs will be located in:
- `build/libs/OpenMCCore-<version>.jar`
- `build/libs/OpenMCAuth-<version>.jar`

## Installation

1. Place `OpenMCCore.jar` in your server's `plugins` folder
2. Place `OpenMCAuth.jar` in your server's `plugins` folder
3. Start your server

## Features

### Core Plugin

- Player data management (points, ranks, etc.)
- Module system for extending functionality
- API for other plugins to interact with

### Auth Plugin

- Registration and login system
- Password hashing with salt
- Protection against unauthorized actions

## Development

### Core API

The Core API is available to other plugins through the Bukkit Services API:

```java
RegisteredServiceProvider<CoreAPI> provider = 
    Bukkit.getServicesManager().getRegistration(CoreAPI.class);
    
if (provider != null) {
    CoreAPI coreAPI = provider.getProvider();
    // Use the API
}
```

### Player Data

Player data is stored in properties files in the `plugins/OpenMCCore/playerdata/` directory.

### Authentication Data

Authentication data is stored in properties files in the `plugins/OpenMCAuth/authdata/` directory.

## Commands

### Auth Plugin

- `/register <password> <password>` - Register a new account
- `/login <password>` - Log in to your account