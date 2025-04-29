<link rel="stylesheet" type="text/css" href="_styles/styles.css">

# Behavior Settings

<figure class="image">
    <img class="screenshot" src="_static/images/settings/behavior_settings.png" width="350px">
    <figcaption class="screenshot-caption"><i>Behavior settings screen</i></figcaption>
</figure>

## Naming

#### <img class="icon" src="_static/icons/human-male.png"> Allow Blank Male ID

When enabled, this setting allows you to record crosses without specifying a male parent.
 
This is useful for:
- Open pollinations
- Unknown pollen sources
- Self-pollination (when not explicitly tracking as self)

#### <img class="icon" src="_static/icons/repeat.png"> Scan Male First

By default, Intercross assumes you'll scan or enter the female parent ID first, followed by the male parent. This setting reverses that order.

When enabled:
- The male parent input field appears first
- Barcode scanning in sequence will populate the male field first, then female
- The tab order on the screen changes accordingly

#### <img class="icon" src="_static/icons/lock-pattern.png"> Create Cross ID Pattern

This setting allows you to switch from the default of randomly generated UUIDs to define a custom pattern for generating unique cross IDs, or to no pattern. Each cross must have a distinct identifier, custom patterns can be used to make them shorter and/or more human-readable.

<figure class="image">
    <img class="screenshot" src="_static/images/settings/behavior_create_uuid_pattern.png" width="350px">
    <figcaption class="screenshot-caption"><i>Cross ID Pattern options</i></figcaption>
</figure>

## Workflow

#### <img class="icon" src="_static/icons/plus-box-multiple.png"> Collect Additional Information

When enabled, this setting toggles on metadata options that allow you to collect extra information about each cross. Default metadata properties are `fruits`, `flowers`, and `seeds`; use the create and manage metadata settings that appear when this setting is enabled to extend or modify these properties.  

#### <img class="icon" src="_static/icons/note-add.png"> Create Metadata

This option allows you to define new metadata properties to associate with crosses. You can specify the name and type of data to collect for each cross you make.

<figure class="image">
    <img class="screenshot" src="_static/images/settings/behavior_new_metadata_property.png" width="350px">
    <figcaption class="screenshot-caption"><i>Create Metadata dialog</i></figcaption>
</figure>

#### <img class="icon" src="_static/icons/update.png"> Manage Metadata

Use this setting to edit or delete existing metadata properties. Press a property to edit its name or default. Long press a property to delete it. Press the <img class="icon" src="_static/icons/plus.png"> icon in the bottom right to add a new property.

<figure class="image">
    <img class="screenshot" src="_static/images/settings/behavior_manage_metadata_joined.png" width="1100px">
    <figcaption class="screenshot-caption"><i>Manage metadata interface</i></figcaption>
</figure>

#### <img class="icon" src="_static/icons/music-note.png"> Sound Notifications

Enables audio feedback for various actions within the app:

- Successful barcode scans
- Completed crosses
- Errors or warnings

Audio cues are particularly useful when:
- Working in bright outdoor conditions where the screen is hard to see
- Performing rapid barcode scanning where visual confirmation is inconvenient
- Training new users who benefit from additional feedback

#### <img class="icon" src="_static/icons/book-open.png"> Open Cross After Creating

When enabled, this setting automatically opens the newly created cross details immediately after saving. This allows you to review the cross information or add additional data without having to manually select the cross from the list.

#### <img class="icon" src="_static/icons/receipt-long.png"> Commutative Crossing

When enabled this treats female A x male B the same as female B x male A for the purposes of tracking progress towards crossing targets.