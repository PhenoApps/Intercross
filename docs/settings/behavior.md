<link rel="stylesheet" type="text/css" href="_styles/styles.css">

# Behavior Settings

## Overview

Behavior settings in Intercross allow you to customize how the app operates to match your breeding program's processes.

<figure align="center" class="image">
<img src="_static/images/settings/behavior_settings.png" width="350px">
<figcaption><i>Behavior settings screen</i></figcaption>
</figure>

## Naming

### Allow Blank Male ID

When enabled, this setting allows you to record crosses without specifying a male parent.

This is useful for:
- Open pollinations
- Unknown pollen sources
- Self-pollination (when not explicitly tracking as self)

### Scan Male First

By default, Intercross assumes you'll scan or enter the female parent ID first, followed by the male parent. This setting reverses that order.

When enabled:
- The male parent input field appears first
- Barcode scanning in sequence will populate the male field first, then female
- The tab order on the screen changes accordingly

### Create Cross ID Pattern

This setting allows you to define a custom pattern for generating unique cross IDs, using UUID format. This ensures each cross has a distinct identifier, though the IDs may be longer and less human-readable.

## Workflow

### Collect Additional Information

When enabled, this setting allows you to collect extra metadata about each cross. After creating a cross, you'll be prompted to enter additional information as configured.

### Create Metadata

This option allows you to define new metadata properties to associate with crosses. You can specify the name and type of data to collect for each cross you make.

### Manage Metadata

Use this setting to edit or organize existing metadata properties. You can modify field names, types, or delete properties that are no longer needed.

### Sound Notifications

Enables audio feedback for various actions within the app:

- Successful barcode scans
- Completed crosses
- Errors or warnings

Audio cues are particularly useful when:
- Working in bright outdoor conditions where the screen is hard to see
- Performing rapid barcode scanning where visual confirmation is inconvenient
- Training new users who benefit from additional feedback

### Open Cross After Creating

When enabled, this setting automatically opens the newly created cross details immediately after saving. This allows you to review the cross information or add additional data without having to manually select the cross from the list.