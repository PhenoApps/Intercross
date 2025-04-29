<link rel="stylesheet" type="text/css" href="_styles/styles.css">

# Storage

## Overview

Intercross requires storage permissions to save and manage your crossing data, import files, and export results.

<figure class="image">
    <img class="screenshot" src="_static/images/storage_structure.png" width="350px">
    <figcaption class="screenshot-caption"><i>Storage folder structure</i></figcaption>
</figure>

## Initial Setup

When you first launch Intercross, you'll be guided through a setup process that includes requesting necessary permissions and selecting or creating a default storage location.

## Directory Structure

Intercross creates several folders in your selected storage location:

```
/Intercross/
  ├── parents_import        # Parent import files
  ├── crosses_export        # Cross data exports
  ├── wishlist_import       # Wishlist import files
```

## Template Files

Upon installation, Intercross creates template files in:

```
/android/data/org.phenoapps.intercross/cache
```

These templates provide the correct format for parent and wishlist imports.

## Changing Storage Location

To change your storage location after initial setup:

1. Go to Settings
2. Select Storage Settings
3. Choose "Change Storage Location"
4. Select or create a new storage location on your device
