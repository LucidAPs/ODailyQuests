# ODailyQuests

ODailyQuests is a daily quests plugin for Minecraft servers. Players receive configurable random quests and rewards designed to encourage regular participation.

> [!IMPORTANT]
> This repository is a maintained fork of [Ordwen/ODailyQuests](https://github.com/Ordwen/ODailyQuests), which was archived on April 30, 2026. The fork is based on upstream commit [`19dba325`](https://github.com/Ordwen/ODailyQuests/commit/19dba325e9bd1fbfe50f4f9737cdff0d8f52dce8). The original project was created by Ordwen; this continuation is maintained by LucidAPs.

## Requirements

- Paper 26.2 or a compatible server implementation
- Java 25
- Maven 3.9 or newer when building from source

## Documentation and support

The [upstream documentation](https://ordwenplugins.gitbook.io/odailyquests/) remains a useful reference for compatible configuration and quest types. Some behavior in this fork may differ from the archived version.

Use [GitHub Issues](https://github.com/LucidAPs/ODailyQuests/issues) to report a problem or propose an improvement to this fork.

## Building

```bash
mvn clean package
```

The compiled plugin JAR is generated in `target/`. To override the project version, pass the `revision` property:

```bash
mvn -Drevision=4.1-SNAPSHOT clean package
```

This fork does not currently publish official binary releases. Build from source until a server-tested release is announced.

## License and attribution

ODailyQuests is distributed under the [GNU General Public License v3.0](LICENSE). See [NOTICE.md](NOTICE.md) for the upstream attribution and modification notice.
