This Bukkit/Spigot plugin allows to use MongoDB as an storage backend for WorldGuard regions.

## Features

 * Storing regions in MongoDB database
   * Supported region types: Cuboid, Polygonal and Global
   * Supported features: region inheritances, priorities, owners, members and flags)

## Compilation

There are currently no binaries for this plugin so you need to build it by yourself. You only need Java Development
Kit 1.7+, Apache Maven 3 and Git.

 1. Clone this repository: `git clone https://github.com/maxikg/mongowg.git`
 2. Change into cloned repository: `cd mongowg`
 3. Invoke maven build: `mvn clean package`

You can find your binary in the `target/` directory.

## Installation

The server needs WorldGuard 6.1 or a compatible version.

The installation is easy:

 1. Drop the plugin jar into your plugins folder (normally `plugins/`)
 2. Restarts server
 3. (Edit created configuration, if necessary)

## ToDo

 * Automatic update of ingame regions on database changes (using MongoDB's oplog)