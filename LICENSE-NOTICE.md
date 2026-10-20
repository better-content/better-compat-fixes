# License notice

Copyright (C) 2026 Gerald Summers.

Unless otherwise noted, the original code in this repository is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See [LICENSE](LICENSE) for the full license text.

Third-party dependencies, assets, generated files, and vendored material remain under their respective licenses and notices.

## Cosmetic bee reference

The original colony particle implementation and 8×8 bee texture are authored in this repository;
no Forestry code or textures are incorporated. Behavioral reference inspected: ForestryMC/ForestryMC
commit `72e15b0f08daa25d8293cf28b5d5e1da200f9161` (Minecraft 1.12 branch),
`src/main/java/forestry/apiculture/entities/ParticleBeeExplore.java` and
`ParticleBeeRoundTrip.java`: small finite-lived, non-colliding bee particles departing and returning
to a colony. That reference is copyright 2011–2014 SirSengir and contributors, licensed LGPL v3
(`LICENSE.txt` at the same commit). Its Java 1.12 renderer/motion code and sprite were not adapted
or redistributed. Complicated Bees and EMI remain external MIT-licensed dependencies, not bundled
or vendored source.
