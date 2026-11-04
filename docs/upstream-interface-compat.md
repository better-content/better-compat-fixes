# Original upstream interface-mod compatibility (source candidate, unqualified)

The pack resolves the pinned **unmodified** Epic ParCool 20.12.0.1 and Valkyrien Skies 2.4.11 JARs on both physical sides. `UpstreamMixinCanceller` is a Better Compat Fixes-owned MixinSquared `MixinCanceller` service, registered in `META-INF/services`; Forge's unmodified MixinSquared 0.3.7 library is included through JarJar. `stageRuntimeJar` must ship the reobfuscated JarJar output rather than the plain mod JAR. Both the foreign resource digest and Forge mod version are checked before cancellation. Unknown registered inputs throw rather than silently applying an unreviewed workaround.

| Upstream registration | Server | Client |
| --- | --- | --- |
| Epic ParCool `ParCoolMixinAnimation` (client-only `Animator` return type) | Cancel | Keep upstream |
| VS `MixinChainConveyorInteractionHandler` (render references) | Cancel | Keep upstream |
| VS `MixinVibrationSystemTicker` (unsupported interface injector) | Cancel | Cancel |

The VS vibration conversion is instead performed by the existing source-owned `VibrationTickerMixin` plugin/transform; its resource binding is now the **original** VS Mixin config and original pinned raw/named class, not the historic derived config. The old source derivative builder/qualifier is retained for historical evidence but no longer runs in ordinary verification; the bytecode parity oracle reads the checksum-verified original upstream JAR from `BC_PACKAGE_ARTIFACT_CACHE` (default `$HOME/.cache/bc/packwiz-downloads`).

**Not a runtime claim.** MixinSquared cancels at target pre-apply, not at config parsing; an earlier upstream Mixin preparation failure cannot be handled by this service. Epic ParCool may have additional server-only startup hazards beyond the one accessor. Focused source checks and an archive-stage check do not establish startup, game-session parity, or authorization to distribute any binary. Before binary promotion, verify actual dedicated-server and client startup with the original JARs, assert the three exact cancellation decisions occurred, then run the required new-artifact tiers. Do not treat any old derivative build receipt as acceptance of this source candidate.
