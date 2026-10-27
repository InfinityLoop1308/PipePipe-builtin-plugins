# PipePipe Built-in Plugins

Plugins that ship with [PipePipe](https://github.com/InfinityLoop1308/PipePipe) 5.6.0 and later. They are the legacy NewPipe services, moved out of the app:

| Plugin | Directory |
| ------ | --------- |
| SoundCloud | `soundcloud` |
| PeerTube | `peertube` |
| media.ccc.de | `mediaccc` |
| Bandcamp | `bandcamp` |

## Maintainers wanted

These services need people who use them and can keep them working when the site changes. Fixes and improvements are welcome as pull requests. If you want to take over a plugin, say so in PipePipe#3080.

## Writing a new plugin

This repository is only for the built-in services. A new service should be its own plugin:

- [PipePipePluginTemplate](https://github.com/InfinityLoop1308/PipePipePluginTemplate): starting point and `plugin.json` reference
- [awesome-pipepipe-plugins](https://github.com/InfinityLoop1308/awesome-pipepipe-plugins): list users install plugins from

Questions and discussion: PipePipe#3080

## Contributing

Each plugin is a Gradle module (`src/main/java`, `src/main/resources/plugin.json`) that implements a `StreamingService` from [PipePipeExtractor](https://github.com/InfinityLoop1308/PipePipeExtractor).

Needs JDK 25, the Android SDK (`build-tools;37.0.0`, `platforms;android-36`, set `sdk.dir` in `local.properties` or `ANDROID_HOME`) and a PipePipeExtractor checkout at `../PipePipeExtractor`.

```
./gradlew assembleRepo     # build/repo/<plugin>-<versionCode>.ppp
```

- Never change `pluginId` or `legacyServiceId` in `plugin.json`. Existing user data depends on them.
- CI builds every pull request. Packages are signed and published only from `main` with the maintainers' key, so you do not need a signing key.

## License

GPL-3.0, same as PipePipe.
