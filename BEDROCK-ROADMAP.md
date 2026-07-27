



Conversion:

- [X] Sound conversion
- [X] Language conversion

Java resource pack structure:

**name** or **name.zip**:
|- pack.mcmeta
|- pack.png
|- <overlay_directory>/assets
|- assets
    |- <namespace>
    |   |- atlases [Folder]: `.json`
    |   |- blockstates [Folder]: `.json`
    |   |- equipment [Folder]: `.json`
    |   |- font [Folder]: `.json`
    |   |- items [Folder]: `.json`
    |   |- lang [Folder]: `<language code>.json` [X]
    |   |- models [Folder]: `.json`
    |   |- particles [Folder]: `.json`
    |   |- post_effect [Folder]: `.json`
    |   |- sounds [Folder]: `.ogg`
    |   |- texts [Folder]: `.json` [X]
    |   |- textures [Folder]: `.png`
    |   |- waypoint_style [Folder]: `.json` [X]
    |   |- gpu_warnlist.json
    |   |- regional_compliancies.json
    |   |- sounds.json [X] Finish
    |- .mcassetsroot