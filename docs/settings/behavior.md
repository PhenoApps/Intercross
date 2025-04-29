<link rel="stylesheet" type="text/css" href="_styles/styles.css">

# Behavior Settings

<figure class="image">
    <img class="screenshot" src="_static/images/settings/behavior_settings.png" width="350px">
    <figcaption class="screenshot-caption"><i>Behavior settings screen</i></figcaption>
</figure>

## Naming

#### <img class="icon" src="_static/icons/human-male.png"> Allow Blank Male ID

When enabled, this setting allows you to record crosses without specifying a male parent.
This is useful for open pollinations and bulk pollen sources.

#### <img class="icon" src="_static/icons/repeat.png"> Scan Male First

By default, Intercross assumes you'll scan or enter the female parent ID first, followed by the male parent.
This setting reverses that order and displays the male parent input field first.

#### <img class="icon" src="_static/icons/lock-pattern.png"> Create Cross ID Pattern

This feature allows you to switch from the default UUIDs to either a custom pattern for generating unique cross IDs or to no pattern.
Custom patterns can be used to make cross IDs shorter and more human-readable.
Selecting `No Pattern` allows users to input their own cross ID, often from a list of pre-printed labels.

<figure class="image">
    <img class="screenshot" src="_static/images/settings/behavior_create_uuid_pattern.png" width="350px">
    <figcaption class="screenshot-caption"><i>Cross ID Pattern options</i></figcaption>
</figure>

## Workflow

#### <img class="icon" src="_static/icons/plus-box-multiple.png"> Collect Additional Information

When enabled, this setting toggles metadata options that allow you to collect extra information about each cross.
Default metadata properties are `fruits`, `flowers`, and `seeds`.
Additional metadata properties can be created and managed using the below settings.

#### <img class="icon" src="_static/icons/note-add.png"> Create Metadata

This option allows you to define new metadata properties to collect for crosses.
Both a name and default value can be defined for metadata properties.

<figure class="image">
    <img class="screenshot" src="_static/images/settings/behavior_new_metadata_property.png" width="350px">
    <figcaption class="screenshot-caption"><i>Create Metadata dialog</i></figcaption>
</figure>

#### <img class="icon" src="_static/icons/update.png"> Manage Metadata

This setting allows editing or deletion of existing metadata properties.
Selecting a property will open the edit screen.
Long pressing a property opens a dialog that allows you to delete it.
Press the <img class="icon" src="_static/icons/plus.png"> icon in the bottom right to add a new property.

<figure class="image">
    <img class="screenshot" src="_static/images/settings/behavior_manage_metadata_joined.png" width="1100px">
    <figcaption class="screenshot-caption"><i>Manage metadata interface</i></figcaption>
</figure>

#### <img class="icon" src="_static/icons/music-note.png"> Sound Notifications

Enables audio feedback for various actions within the app:
- Successful barcode scans
- Completed crosses
- Errors or warnings

Audio cues are particularly useful when working in bright outdoor conditions where the screen is hard to see, performing rapid barcode scanning, or training new users who benefit from additional feedback.

#### <img class="icon" src="_static/icons/book-open.png"> Open Cross After Creating

When enabled, this will automatically open the details page for a newly created cross immediately after it has been created.
This simplifies the process for entering metadata and allows you to review the cross information.

#### <img class="icon" src="_static/icons/receipt-long.png"> Commutative Crossing

When enabled this treats crosses consisting of `female A` x `male B` the same as `female B` x `male A` for the purposes of tracking progress towards crossing targets.