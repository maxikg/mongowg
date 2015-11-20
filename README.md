[![MongoWG](https://raw.githubusercontent.com/maxikg/mongowg/master/docs/logo.png "MongoWG")](https://github.com/maxikg/mongowg)

[![Build Status](https://travis-ci.org/maxikg/mongowg.svg)](https://travis-ci.org/maxikg/mongowg)

This Bukkit/Spigot plugin allows to use MongoDB as a storage backend for WorldGuard regions.

## Features

 * Storing regions in MongoDB database
   * Supported region types: Cuboid, Polygonal and Global
   * Supported features: region inheritances, priorities, owners, members and flags
 * Optionally updating regions on database changes (since 0.3 with oplog enabled)

## Compile

Binaries are possibly released on GitHub: https://github.com/maxikg/mongowg/releases

If not or you just want to build it by yourself there are just a few steps. You only need Java Development
Kit 1.7+, Apache Maven 3 and Git.

 1. Clone this repository: `git clone https://github.com/maxikg/mongowg.git`
 2. Change into cloned repository: `cd mongowg`
 3. Invoke maven build: `mvn clean package`

You can find your binary in the `target/` directory.

## Installation

The server needs WorldGuard 6.1 or a compatible version.

The installation is easy:

 1. Drop the plugin jar into your plugins folder (normally `plugins/`)
 2. Restart server
 3. (Edit created configuration, if necessary)

### Using oplog for live updates

In order to use the oplog feature you must start your MongoDB server with the `--master` option. After you have done
this you need to enable the oplog support by changing `mongodb.use_oplog` to true in MongoWG's configuration file.
