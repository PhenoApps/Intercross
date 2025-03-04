# Importing Files

## Overview

Intercross allows importing wishlist and parent files to enhance functionality.

## File Locations

Template files are available in:
```
/android/data/org.phenoapps.intercross/cache
```

## Parents Import

<figure align="center" class="image">
<img src="_static/images/import_parent.png" width="350px">
<figcaption><i>File import screen</i></figcaption>
</figure>

The Parents import file format is a CSV containing a list of parents, typically with:
- Unique ID
- Name
- Sex (coded as `0 = female`, `1 = male`)

Example:
```
id,name,sex
15RPN00001,Kharkof,0
15RPN00002,Blueboy,1
```

## Wishlist Import

<figure align="center" class="image">
<img src="_static/images/import_wishlist.png" width="350px">
<figcaption><i>File import screen</i></figcaption>
</figure>

Wishlist import files should contain:
- Female ID
- Male ID
- Female Name (optional)
- Male Name (optional)
- Cross Type
- Minimum crosses required
- Maximum crosses desired

Headers must match templates for successful import.