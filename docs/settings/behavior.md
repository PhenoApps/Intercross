# Behavior Settings

## Overview

Behavior settings in Intercross allow you to customize how the app operates to match your breeding program's processes.

<figure align="center" class="image">
<img src="_static/images/settings/behavior_settings.png" width="350px">
<figcaption><i>Behavior settings screen</i></figcaption>
</figure>

## Collect Additional Information

When enabled, this setting allows you to collect extra metadata about each cross.

## Cross ID Generation Methods

There are three options for generating cross IDs:

### None
Manual entry of cross IDs is required for each cross.

### Pattern
Configure a pattern with:
- Prefix (e.g., `20RPN`)
- Suffix (optional)
- Starting value (e.g., `1`)
- Padding (number of digits, e.g., `5` for `00001`)

Example: Using prefix "20RPN", starting value "1", and padding "5" would generate: `20RPN00001`, `20RPN00002`, etc.

### UUID
Generate a Universal Unique ID for each cross. This ensures completely unique identifiers but results in longer, less human-readable IDs.

## Allow Blank Male ID

When enabled, this setting allows you to record crosses without specifying a male parent.

This is useful for:
- Open pollinations
- Unknown pollen sources
- Self-pollination (when not explicitly tracking as self)

To enable:
1. Navigate to Settings > Naming
2. Toggle "Allow Blank Male ID" to On

## Scan Male First

By default, Intercross assumes you'll scan or enter the female parent ID first, followed by the male parent. This setting reverses that order.

When enabled:
- The male parent input field appears first
- Barcode scanning in sequence will populate the male field first, then female
- The tab order on the screen changes accordingly

To enable:
1. Navigate to Settings > Naming
2. Toggle "Scan Male First" to On

## Create Cross ID Pattern

This setting works in conjunction with [Auto Cross](auto-cross.md) settings to determine how cross IDs are generated.

Here you can define a pattern template that includes:
- Static prefix text
- Static suffix text
- Auto-incrementing number with padding

For example, a pattern of "20RPN{0000}" would generate IDs like "20RPN0001", "20RPN0002", etc.

To configure:
1. Navigate to Settings > Naming
2. Tap "Create Cross ID Pattern"
3. Enter your desired pattern
4. Press "OK"

### Available Metadata Fields

After creating a cross, you'll be prompted to enter:
- Success rating
- Notes
- Custom metadata fields (if configured)

### Enabling Extra Data Collection

1. Navigate to Settings > Behavior
2. Toggle "Collect Additional Information" to On

Additional metadata fields can be configured in the Metadata settings section.

## Audio Notifications

Intercross can provide audio feedback for various actions:

- Successful barcode scans
- Completed crosses
- Errors or warnings

Audio cues are particularly useful when:
- Working in bright outdoor conditions where the screen is hard to see
- Performing rapid barcode scanning where visual confirmation is inconvenient
- Training new users who benefit from additional feedback

### Enabling Audio Notifications

1. Navigate to Settings > Behavior
2. Toggle "Audio Notifications" to On

### Sound Types

- **Successful scan**: A brief chime indicates a barcode was successfully read
- **Successful cross**: A different tone indicates a cross was successfully saved
- **Error**: A distinct sound indicates an error condition

## Custom Metadata Fields

You can create custom fields to collect additional information for each cross:

1. Navigate to Settings > Behavior > Manage Custom Fields
2. Tap "Add New Field"
3. Configure the field:
   - Field name
   - Field type (text, number, date, etc.)
   - Required/optional status
4. Press "Save"

These custom fields will appear in the metadata collection form after creating a cross.