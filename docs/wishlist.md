# Wishlist

## Overview

The Wishlist feature helps you plan and manage crossing goals by tracking planned crosses and their completion status.

<figure align="center" class="image">
<img src="_static/images/wishlist.png" width="350px">
<figcaption><i>Wishlist view</i></figcaption>
</figure>

## Using the Wishlist

The wishlist displays:
- Two parents to be crossed
- Current number of crosses completed
- Minimum number of crosses desired
- Maximum number of crosses desired
- Completion status (checkmark for completed combinations)

## Importing a Wishlist

Wishlists are typically imported from CSV files:

1. Navigate to the Import section
2. Select a wishlist file
3. Map the columns to required fields
4. Import the file

## Wishlist File Format

A wishlist file should include these columns:
- Female ID (`femaleObsUnitDbId`)
- Male ID (`maleObsUnitDbId`)
- Female Name (optional)
- Male Name (optional)
- Cross Type
- Minimum crosses required
- Maximum crosses desired

## Tracking Progress

As you make crosses, the wishlist automatically updates to show progress toward your goals. Completed combinations (where the current count meets or exceeds the minimum) are marked with a checkmark.